# ADR-0002 — Partition key = canonical SKU

**Status**: Accepted — 2026-09-02

## Context
Concurrent updates to the same product (a price change from a feed plus a stock decrement from an
order) can arrive out of order and overwrite each other, pushing wrong data to the marketplaces. The
options: a distributed lock, global ordering, or partitioning by entity.

## Decision
Every event about a product uses `tenantId|sku` as its **Kafka key**. Kafka guarantees ordering per
partition, therefore total ordering per product. In addition, every command carries the canonical
`revision`, and the driver discards commands whose revision is below the one already applied
(explicit last-write-wins).

## Consequences
**Positive**
- No distributed lock on the critical path.
- Parallelism equals the partition count, which is real horizontal scalability.
- Semantics that are simple to explain and to test.

**Negative**
- The partition count cannot be changed without breaking the key→partition mapping, so it must be
  oversized from the start (24 on the product topics).
- "Hot" SKUs can unbalance partitions; acceptable, because the load is dominated by channel rate
  limits rather than CPU.
- For products with variants the key is the **parent** SKU, so variants of the same product stay
  ordered relative to one another.

## Alternatives rejected
- **A distributed Redis lock per SKU**: adds a dependency on the critical path and a new way to fail.
- **A single partition for global ordering**: unacceptable throughput.
- **No ordering, only timestamps**: clocks are not reliable across services, and marketplace
  timestamps even less so.
