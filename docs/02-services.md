# 02 — Services

Conventions: every service is a self-contained Spring Boot application with its own Postgres schema,
its own Kafka consumer group, `/actuator/health|prometheus` endpoints, and a Docker artifact.

---

## `piovra-gateway`
**Role.** The single public HTTP entry point: authentication, per-client rate limiting, routing to
internal services, feed upload.

**Exposes**
| Method | Path | Notes |
|---|---|---|
| `POST` | `/v1/feeds` | multipart or `application/octet-stream`; returns a `feedId` (202) |
| `GET` | `/v1/feeds/{id}` | processing status |
| `GET` | `/v1/products/{sku}` | canonical view plus per-channel status |
| `GET` | `/v1/products/{sku}/channels/{channel}` | listing detail, last error, snapshot |
| `POST` | `/v1/products/{sku}/resync` | forces republication (bypasses the diff) |
| `GET` | `/v1/orders` | canonical order search |
| `GET` | `/v1/sync-errors` | open errors, filterable |

**Does not.** Business logic. It is a proxy with authentication.

---

## `feed-ingestion`
**Role.** Acquire files and make them immutable and traceable.

**Inputs.**
- **SFTP/FTP**: an embedded SFTP server (Apache MINA SSHD) or a watcher on a mounted directory. A
  file counts as complete only after a *quiet period* or via a `.done` sentinel file (this prevents
  reading half-finished uploads).
- **HTTP**: from the gateway.
- **Pull**: a scheduled job that fetches the feed from a supplier's URL or remote FTP.

**Behaviour.**
1. Computes the file's `sha256`. If identical to the previous feed from the same source, it is
   marked `SKIPPED_DUPLICATE` (optional, configurable).
2. Archives the raw file in S3/MinIO: `s3://piovra-feeds/{tenant}/{source}/{yyyy}/{MM}/{dd}/{feedId}.{ext}`.
3. Writes the `feed` row (state `RECEIVED`) plus the outbox entry, in one transaction.
4. Emits `feed.received`.

**Data.** `feed(id, tenant_id, source_id, filename, sha256, size, storage_uri, format, status, received_at, ...)`

---

## `feed-processor`
**Role.** Turn a raw file into validated canonical records.

**Behaviour.**
1. Streams the file from object storage (CSV via univocity/opencsv, XML via StAX, JSON via Jackson
   streaming, XLSX via a streaming reader).
2. Applies the source's **mapping profile** (see [05](05-feed-flow.md)): column → canonical field,
   transformations, defaults, units of measure.
3. Validates (required fields, types, ranges, taxonomy). An invalid record produces
   `feed.record.rejected` plus a row in the error report, **without blocking the rest**.
4. Emits `catalog.product.upsert` in batches, keyed by SKU.
5. On completion emits `feed.completed` with counters (`ok`, `rejected`, `skipped`).

**Feed modes.** `DELTA` (default, only the records present) or `FULL` (products absent from the feed
are marked `discontinued` → delisted from the channels). The mode is per source and per feed.

---

## `catalog-service`
**Role.** Source of truth for the canonical product.

**Behaviour.**
- Applies the upsert: compares the incoming record with the current state; if **nothing changed**, it
  does not bump the revision and emits nothing (the first noise filter).
- If something changed: bumps `revision`, persists, and writes an outbox entry carrying
  `catalog.product.changed` with **the modified fields** (`changedFields`), not just the new state.
- Handles products with variants (parent plus children) as one aggregate: the revision lives on the
  parent.

**Exposes.** Read APIs (for the gateway and console) and `POST /internal/products/{sku}/touch` to
force a recomputation.

---

## `inventory-service`
**Role.** Source of truth for stock and its history.

**Model.** An append-only movement ledger plus a materialized `stock_level` projection.

```
stock_level(sku, on_hand, reserved, available, buffer, version)
stock_movement(id, sku, delta, reason, source_channel, idempotency_key UNIQUE, created_at)
```
`available = max(0, on_hand - reserved - buffer)` — `available` is what gets published to channels.

**Behaviour.**
- Applies idempotent movements (`idempotency_key`, e.g. `ebay:ORDER-123:LINE-1`).
- Recomputes `available`; when it differs from the last published value, emits `inventory.changed`.
- Movement sources: feeds (absolute set), orders (decrement), returns and cancellations (increment),
  manual adjustment, reconciliation.
- Absolute `SET` and relative `DELTA` are distinct operations: feeds do `SET`, orders do `DELTA`. A
  `SET` arriving while orders are in flight is applied with a timestamp/sequence check so it cannot
  "resurrect" quantities already sold.

---

## `order-service`
**Role.** Canonical orders, normalized and deduplicated.

**Behaviour.**
1. Consumes `channel.order.received` from the drivers.
2. Deduplicates on `UNIQUE(channel, channel_order_id)`; a re-emission of the same order only updates
   its state.
3. Resolves `channelSku → canonicalSku` against the mapping (locally cached). An unresolvable SKU
   leaves the order stored with the line in `UNMAPPED` state plus an alert: **an order is never
   lost**.
4. Emits `order.accepted` with the resolved lines, consumed by `inventory-service`.
5. Manages transitions: `NEW → PAID → SHIPPED → COMPLETED`, `CANCELLED`, `REFUNDED`. Cancellations
   and returns generate positive stock movements.

---

## `publication-service`
**Role.** The orchestration brain: it decides **what** to publish, **where** and **when**.

**Behaviour.**
- Maintains the `channel_listing` table (mapping, snapshot and state, see [03](03-data-model.md)).
- Consumes `catalog.product.changed` and `inventory.changed`.
- For every channel active on the product:
  - checks the **listing rules** (is the product eligible for that channel? category mapped? minimum
    price? enough images?);
  - computes the **diff** between the desired payload and `published_snapshot`;
  - if the diff is empty, **emits nothing**;
  - otherwise emits `channel.command.upsert` (or `.inventory`, `.price`, `.end`) with a `commandId`,
    a `revision` and only the changed fields.
- Consumes `channel.result` and updates state, `external_id`, snapshot and last error.
- Handles bulk **resync** and periodic **reconciliation** (comparing against the marketplace's real
  state).

**Why it is separate from the drivers.** The "when and what" logic stays in one testable place, while
drivers remain dumb, replaceable translators.

---

## `connector-ebay` / `connector-woocommerce`
**Role.** Translate canonical commands into marketplace API calls, and marketplace orders into
canonical events. See [08](08-marketplace-drivers.md).

Each connector holds two sub-components:
- **outbound**: consumes `channel.command.*` → API calls → `channel.result`;
- **inbound**: a scheduled poller and/or webhook endpoint → `channel.order.received` (plus
  notifications of listing/price changes made on the marketplace side).

---

## `channel-config`
**Role.** Channel and account registry: credentials (as Vault references), publication policies,
category mappings, listing rules, polling schedules, per-channel stock buffers.

It also publishes the configuration on the compacted `channel.config.v1` topic so drivers keep it in
a local cache without synchronous calls.

---

## `ops-console`
**Role.** Operational UI and API: feed status, out-of-sync products, per-channel errors, DLQ contents
with **replay** or **discard** actions, a visual diff between desired and published, `UNMAPPED`
orders.

---

## Service ↔ topic matrix

| Service | Consumes | Produces |
|---|---|---|
| `feed-ingestion` | — | `feed.received` |
| `feed-processor` | `feed.received` | `catalog.product.upsert`, `feed.record.rejected`, `feed.completed` |
| `catalog-service` | `catalog.product.upsert` | `catalog.product.changed` |
| `inventory-service` | `catalog.product.changed`, `order.accepted` | `inventory.changed` |
| `publication-service` | `catalog.product.changed`, `inventory.changed`, `channel.result` | `channel.command.*` |
| `connector-*` | `channel.command.*`, `channel.config` | `channel.result`, `channel.order.received` |
| `order-service` | `channel.order.received` | `order.accepted`, `order.status.changed` |
