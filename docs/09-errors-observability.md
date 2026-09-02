# 09 — Errors and observability

> A synchronization system is judged not by how fast it is when everything works, but by how clear it
> is when something breaks. Every error must carry: **a code**, **the product or order it refers
> to**, **a cause a non-technical human can read**, and **an action they can take**.

## 1. Error taxonomy

| Class | Examples | Retry | Destination |
|---|---|---|---|
| `VALIDATION` | missing required field, invalid EAN, title over 80 chars | ❌ | record rejected / listing `BLOCKED` |
| `MAPPING` | unmapped category, missing required aspect, unknown order SKU | ❌ | console, requires a configuration change |
| `AUTH` | token expired and not refreshable, credentials revoked | ❌ (after one refresh attempt) | immediate alert, channel `SUSPENDED` |
| `RATE_LIMIT` | 429, daily quota exhausted | ✅ with long backoff | retry topic, consumer pause |
| `TRANSIENT` | timeout, 502/503/504, connection reset | ✅ exponential backoff | retry topic |
| `MARKETPLACE_REJECT` | listing refused, policy violation, duplicate | ❌ | listing `ERROR`, console |
| `CONFLICT` | resource modified elsewhere, stale version | ✅ once after re-reading | immediate retry |
| `INTERNAL` | bug, NPE, undeserializable schema | ❌ | DLQ plus an on-call alert |

Every driver implements `translate(Exception)` to bring native errors into this taxonomy. The
canonical code has the shape `<CHANNEL>_<CLASS>_<NATIVE_CODE>` — e.g.
`EBAY_MARKETPLACE_REJECT_25002`, `WOO_TRANSIENT_HTTP_504`.

## 2. Retry strategy

```
attempt 1  → immediate
attempt 2  → 5 s   ± jitter
attempt 3  → 30 s  ± jitter      (up to here: in process)
attempt 4  → retry topic, 5 min
attempt 5  → retry topic, 30 min
attempt 6  → retry topic, 6 h
beyond     → DLQ
```
- Jitter is mandatory (`±20 %`): without it, after a marketplace outage 50,000 commands restart
  together and take it down again.
- For `RATE_LIMIT` the backoff starts from the `Retry-After` header when present.
- The `attempt` counter travels in the message, not in memory: it survives restarts.

## 3. Circuit breakers and bulkheads

Per `channelId` (Resilience4j):
- **Circuit breaker**: opens at 50 % failures over a 20-call window; half-open after 60 s with 3 trial
  calls. While open the consumer pauses instead of burning retries.
- **Bulkhead**: separate connection and thread pools per channel.
- **Time limiter**: an explicit timeout on every external call (typically 3 s connect, 20 s read;
  higher for Woo batches).

The breaker state is exposed as a metric and as a console indicator: "eBay: circuit open for 4
minutes, 320 commands queued".

## 4. Dead letter queues

Every consumer group has its own DLQ, `piovra.<topic>.dlq.v1`. Added headers:
`x-piovra-error-class`, `x-piovra-error-code`, `x-piovra-error-message`,
`x-piovra-original-topic|partition|offset`, `x-piovra-failed-at`, `x-piovra-attempts`, `traceparent`.

The console offers:
- an aggregate view **by error code** (three causes usually explain 90 % of the messages);
- **selective replay** (by channel, code, time range or single message) back to the original topic,
  with a fresh `commandId` and `attempt=0`;
- **discard** with a reason, recorded in the audit log.

## 5. Errors visible to the merchant

A `sync_error` table fed by permanent outcomes, designed for an operator rather than a developer:

| Field | Example |
|---|---|
| `sku` | `TSHIRT-BASE-M-RED` |
| `channel` | eBay IT |
| `severity` | `BLOCKING` / `WARNING` |
| `code` | `MISSING_REQUIRED_ASPECT` |
| `message` | "eBay requires the 'Material' attribute for the T-shirts category" |
| `suggestedAction` | "Add an ATTR_Material column to the feed, or set a default in the mapping profile" |
| `occurredAt` / `resolvedAt` | |

Rule: if an error has no `suggestedAction`, either it is our bug or a translation is missing. That is
a code review criterion.

## 6. Observability

### 6.1 Tracing
OpenTelemetry end to end. `traceparent` travels in the Kafka headers, so a single trace spans
`feed upload → parse → upsert → publish → eBay call`. `correlation-id = feedId | orderId` answers
"what happened to this morning's feed?" in one click.

### 6.2 Metrics (Micrometer → Prometheus)

**Business**
- `piovra_products_total{state}` — products by sync state, per channel
- `piovra_publish_noop_ratio{channel}` — how well the diff is working
- `piovra_sync_lag_seconds{channel}` — age of the oldest canonical change not yet published
  (**the system's single most important metric**)
- `piovra_orders_ingested_total{channel}` / `piovra_orders_unmapped`
- `piovra_inventory_propagation_seconds` — histogram of the closed loop
- `piovra_oversold_total{channel}`

**Technical**
- consumer lag per topic/group, `piovra_dlq_messages_total{topic,code}`
- `piovra_marketplace_call_duration_seconds{channel,operation,outcome}`
- `piovra_ratelimit_tokens_available{channel}`, `piovra_circuit_state{channel}`
- error rate per class, `feed_processing_duration_seconds`, `feed_rejected_records_total`

### 6.3 Logs
Structured JSON, with `traceId`, `tenantId`, `sku`, `channelId`, `commandId` in the MDC. **Never** log
credentials, tokens or buyer personal data: addresses are masked in logs (they stay in the database
with controlled access).

### 6.4 Alerts (example thresholds)
| Condition | Severity |
|---|---|
| `sync_lag_seconds > 900` on a channel | P2 |
| no orders ingested from a channel for > 3× the polling interval | P1 |
| circuit open for more than 10 minutes | P2 |
| `oversold_total` increments | P1 |
| DLQ above 100 messages/hour | P2 |
| a feed `SUSPENDED` by the sanity threshold | P2 |
| a refresh token expiring in under 14 days | P3 |

## 7. Audit

An append-only log (a partitioned table or a compacted topic) of: channel configuration changes,
forced resyncs, DLQ replays and discards, manual stock adjustments, mapping changes. Each entry
records the actor, the timestamp and the before/after values.

## 8. Testing and quality

| Level | Tools |
|---|---|
| Unit | JUnit 5, AssertJ; payload and diff computation are pure, so **golden-file** tests apply |
| Integration | **Testcontainers** (Kafka, Postgres, Redis, MinIO) |
| External APIs | **WireMock**, with scenarios recorded from the real sandboxes |
| Driver contract | the shared TCK ([08](08-marketplace-drivers.md)) plus a nightly run against sandboxes |
| Load | k6/Gatling on ingestion; a generator for 500k-row feeds |
| Chaos | kill the broker, a marketplace returning 500 for 10 minutes, latency at 5 s: the system must recover **without loss and without duplicated effects** |
