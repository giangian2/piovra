# 11 — Roadmap

Every phase has a **verifiable exit criterion**. We do not move on without it.

## Phase 0 — Foundations (2–3 weeks)
- Maven multi-module scaffolding, parent POM, wrapper, Spotless/ArchUnit.
- Complete local `docker-compose`; CI pipeline (build, test, images).
- `piovra-model`, `piovra-events` (first Avro schemas), `piovra-kafka-support`, `piovra-outbox`.
- `piovra-driver-spi` plus the TCK skeleton.

**Exit**: a "hello" service publishes and consumes an event with schema registry, outbox and tracing
working end to end locally.

## Phase 1 — Catalog end-to-end on a single channel (4–6 weeks)
- `feed-ingestion` (HTTP only), `feed-processor` (CSV only), `catalog-service`, `publication-service`.
- `piovra-driver-woocommerce` plus `connector-woocommerce` (Woo before eBay: a simpler API, and its
  sandbox is a local WordPress container).
- Diff, upsert, `channel_listing`, outcomes, retry topics, DLQ.

**Exit**: a 10,000-row CSV uploaded over HTTP creates and updates the products on WooCommerce; a
second, identical upload produces **zero** API calls (noop ratio = 1).

## Phase 2 — Orders and inventory (3–4 weeks)
- `order-service`, `inventory-service` with the ledger and idempotency.
- Woo order polling, webhooks with signature verification.
- Closed stock loop on a single channel, plus reconciliation.

**Exit**: an order on Woo decrements canonical stock in under 15 s; reprocessing the order does not
produce a second decrement; a cancellation restores the stock.

## Phase 3 — eBay, that is, real multi-channel (5–7 weeks)
- `piovra-driver-ebay` (clients generated from OpenAPI, `EbayTokenManager`, aspect/category mapping).
- `connector-ebay` with a distributed rate limiter and batching over `bulkUpdatePriceQuantity`.
- Cross-channel stock fan-out, stock buffers, priority for inventory commands.

**Exit**: an order on eBay decrements stock on WooCommerce (and vice versa) in under 15 s; a load
test with 50,000 SKUs across two channels with no overselling.

## Phase 4 — Complete ingestion and operability (3–4 weeks)
- Embedded SFTP, XML/JSON/XLSX, declarative mapping profiles, `FULL` feeds with the sanity threshold.
- `ops-console`: feed status, errors with `suggestedAction`, DLQ plus replay, visual diff, `UNMAPPED`
  orders.
- `channel-config` with listing rules and policies.

**Exit**: a new supplier is onboarded **without a deployment**, using only a mapping profile; a
non-technical operator resolves a sync error starting from the console.

## Phase 5 — Production (3–4 weeks)
- Helm charts, KEDA, Grafana dashboards, alerts, runbooks.
- Chaos testing, a disaster-recovery drill, backup verification.
- Vault, token rotation, hardening, audit.

**Exit**: SLOs met for two weeks in staging under realistic traffic; the runbook proven in a real
drill.

## Beyond

| Theme | Notes |
|---|---|
| New channels | Amazon SP-API, Shopify, PrestaShop, ManoMano — the SPI holds; the cost is taxonomy mapping |
| Multi-tenancy | `tenantId` is already everywhere; resource isolation and per-tenant quotas are missing |
| Fulfilment | tracking pushed back to the marketplaces, WMS/carrier integration |
| Repricing | per-channel price rules, competitor monitoring |
| Lightweight PIM | content enrichment, machine translation, media management |
| Analytics | sales per channel/product, margins, sell-through on ClickHouse |

## Main risks

| Risk | Impact | Mitigation |
|---|---|---|
| Marketplace rate limits tighter than expected | real throughput far below target | aggressive diffing, batching, early measurement against real sandboxes |
| eBay taxonomy mapping more costly than expected | phase 3 slips | prototype the category/aspect mapping **during phase 1**, on paper |
| Overselling on very fast channels | commercial and reputational damage | stock buffers, stock priority, critical threshold, alerts |
| Poor-quality supplier feeds | noise and false errors | layered validation, sanity thresholds, rejection reports from phase 1 |
| Microservice complexity out of proportion to the team | general slowdown | monorepo, a service template, shared libraries; merge `feed-ingestion` and `feed-processor` if the team is small |
