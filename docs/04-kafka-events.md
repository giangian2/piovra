# 04 — Kafka & events

## 1. Why Kafka

- **An immutable, re-readable log**: channel state can be rebuilt by replaying it, which is the
  property that saves you when a driver has published wrong data for three hours.
- **Temporal decoupling**: if eBay is down, the backlog stays in the log and nothing upstream blocks.
- **Ordering by key**: per-product consistency without distributed locks.
- **Natural backpressure**: lag becomes the system's health metric.

## 2. Conventions

- Topic name: `piovra.<domain>.<entity>.<verb>.v<version>` — e.g. `piovra.catalog.product.changed.v1`.
- **Key**: `tenantId|sku` for everything about a product; `tenantId|channelId|channelOrderId` for
  orders.
- **Format**: Avro or Protobuf with a **Schema Registry**, `BACKWARD` compatibility. JSON only for
  development topics.
- **Standard headers** on every message:
  | Header | Meaning |
  |---|---|
  | `x-piovra-event-id` | unique ULID of the event |
  | `x-piovra-correlation-id` | correlates the whole chain (starts at the `feedId` or `orderId`) |
  | `x-piovra-causation-id` | id of the event that caused this one |
  | `x-piovra-tenant` | tenant |
  | `x-piovra-schema-version` | logical payload version |
  | `traceparent` | W3C trace context (OpenTelemetry) |

## 3. Topic catalog

| Topic | Key | Part. | Retention | Cleanup | Producer → Consumer |
|---|---|---|---|---|---|
| `piovra.feed.received.v1` | feedId | 6 | 7 d | delete | ingestion → processor |
| `piovra.feed.record.rejected.v1` | feedId | 6 | 30 d | delete | processor → ops |
| `piovra.feed.completed.v1` | feedId | 3 | 30 d | delete | processor → ops, gateway |
| `piovra.catalog.product.upsert.v1` | sku | 24 | 7 d | delete | processor → catalog |
| `piovra.catalog.product.changed.v1` | sku | 24 | 30 d | delete | catalog → publication, inventory |
| `piovra.catalog.product.snapshot.v1` | sku | 24 | ∞ | **compact** | catalog → bootstrap of new services |
| `piovra.inventory.changed.v1` | sku | 24 | 7 d | delete | inventory → publication |
| `piovra.channel.command.v1` | sku | 24 | 3 d | delete | publication → connector-* |
| `piovra.channel.result.v1` | sku | 24 | 30 d | delete | connector-* → publication |
| `piovra.channel.order.received.v1` | channelOrderId | 12 | 30 d | delete | connector-* → order |
| `piovra.order.accepted.v1` | orderId | 12 | 30 d | delete | order → inventory |
| `piovra.order.status.changed.v1` | orderId | 12 | 30 d | delete | order → ops, connector-* |
| `piovra.channel.config.v1` | channelId | 3 | ∞ | **compact** | channel-config → everyone |
| `piovra.<topic>.dlq.v1` | original | = origin | 30 d | delete | consumer → ops-console |

> `piovra.channel.command.v1` can be a single topic with a `channel-id` header or — the preferred
> production choice — one topic per channel type (`…command.ebay.v1`, `…command.woocommerce.v1`), so
> each driver consumes only what concerns it and retention/partitions are tuned per channel. See
> [ADR-0003](adr/0003-topic-per-channel.md).

## 4. Schemas of the main events

### `catalog.product.changed.v1`
```jsonc
{
  "eventId": "01J…",
  "tenantId": "acme",
  "sku": "TSHIRT-BASE",
  "revision": 42,
  "changeType": "UPDATED",            // CREATED | UPDATED | DISCONTINUED
  "changedFields": ["price", "title.it", "media"],
  "product": { /* full canonical state, see doc 03 */ },
  "occurredAt": "2026-09-02T09:15:00Z"
}
```
It carries **the full state** *and* the list of changed fields: consumers never need a callback to
the catalog, and the publication service can optimize the command type.

### `inventory.changed.v1`
```jsonc
{
  "eventId": "01J…", "tenantId": "acme", "sku": "TSHIRT-BASE-M-RED",
  "available": 7, "previousAvailable": 9,
  "reason": "ORDER", "sourceChannelId": "ebay-it-main",
  "movementId": 998211, "occurredAt": "…"
}
```

### `channel.command.v1`
```jsonc
{
  "commandId": "01J…",                 // driver-side idempotency key
  "tenantId": "acme",
  "channelId": "ebay-it-main",
  "sku": "TSHIRT-BASE",
  "operation": "UPSERT",               // UPSERT | INVENTORY | PRICE | END | RELIST
  "revision": 42,                       // used to discard stale commands
  "priority": "NORMAL",                 // HIGH (stock) | NORMAL (content) | LOW (bulk)
  "payload": { /* desired canonical projection for that channel */ },
  "changedGroups": ["price", "stock"],  // guides the driver to the lightest API call
  "attempt": 0,
  "issuedAt": "…"
}
```

### `channel.result.v1`
```jsonc
{
  "commandId": "01J…", "channelId": "ebay-it-main", "sku": "TSHIRT-BASE",
  "outcome": "SUCCESS",                 // SUCCESS|RETRYABLE_ERROR|PERMANENT_ERROR|STALE|NOOP
  "externalId": "v1|1234567890|0",
  "externalVariantIds": { "TSHIRT-BASE-M-RED": "9876" },
  "publishedSnapshotHash": "sha256:…",
  "error": { "code": "EBAY_25002", "message": "Duplicate listing", "retryable": false },
  "latencyMs": 412, "attempt": 1, "completedAt": "…"
}
```

## 5. Guarantees and configuration

**Producer**
```properties
acks=all
enable.idempotence=true
max.in.flight.requests.per.connection=5
compression.type=zstd
linger.ms=20
batch.size=131072
```

**Consumer**
```properties
enable.auto.commit=false          # manual commit after processing
isolation.level=read_committed
max.poll.records=500
max.poll.interval.ms=300000
```

**Broker/topic**: `min.insync.replicas=2`, `replication.factor=3`.

## 6. Sizing the partitions

Rule of thumb: `#partitions ≥ (target throughput) / (throughput per consumer)`, and never below the
maximum parallelism expected for the slowest consumer. Since drivers are bound by **marketplace rate
limits** rather than CPU, the real constraint is *how many parallel requests the API tolerates*. 24
partitions on the product topics is a reasonable starting point, deliberately oversized (increasing
partitions later changes the key→partition mapping and breaks historical ordering).

## 7. Retries and DLQ

A three-level strategy (see also [09](09-errors-observability.md)):

1. **In process**: immediate retries with exponential backoff plus jitter, for obviously transient
   errors (timeouts, 5xx), up to 3 attempts.
2. **Delay topics**: `piovra.channel.command.retry.5m.v1`, `…retry.30m.v1`, `…retry.6h.v1`. The
   consumer of a delay topic waits until `issuedAt + delay` (or uses a consumer that pauses), then
   republishes to the main topic. This avoids blocking the partition.
3. **DLQ**: once exhausted, the message goes to `…dlq.v1` with headers `x-piovra-error-code`,
   `x-piovra-error-stack`, `x-piovra-original-topic/partition/offset`. The console allows **selective
   replay** (by channel, by error code, by time range).

> **Golden rule**: never block a partition on a single poison message. One broken product must not
> stop the other 50,000.

## 8. Schema evolution

- Adding optional fields: free (BACKWARD compatible).
- Removing or renaming: a new `.v2` topic, dual writes for a window, consumer migration, then
  decommissioning `v1`.
- The registry check runs in CI: a PR that breaks compatibility fails the build.
