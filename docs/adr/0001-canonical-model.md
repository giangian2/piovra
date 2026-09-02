# ADR-0001 — Canonical model with per-channel projections

**Status**: Accepted — 2026-09-02

## Context
Every marketplace has its own product model (eBay: InventoryItem plus Offer with per-category
aspects; WooCommerce: a WordPress post with attributes and variations). Two options: (a) translate
directly from feed to channel, (b) introduce an intermediate canonical model.

## Decision
A **canonical model** is the source of truth. For every `(product, channel)` pair there is a
`ChannelListing` holding the external id, the sync state and the **snapshot** of the last published
payload.

## Consequences
**Positive**
- The cost of adding a channel is `O(1)` rather than `O(#sources)`: N sources × M channels becomes
  N + M mappings.
- The per-channel snapshot enables diffing, and therefore incremental upserts and no-op suppression.
- The core is testable without real marketplaces.
- You can answer "what exactly did I publish to eBay for this SKU?".

**Negative**
- One more translation layer and one more store to maintain.
- The canonical model has to be designed as the expressive lowest common multiple: fields that are
  too poor force everything into `attributes`, fields that are too rich become dead weight.

## Alternatives rejected
- **Direct feed→channel translation**: faster for the first channel, unmanageable from the third
  onwards, and there is no single place that answers "what is the truth?".
- **Adopting eBay's model as canonical**: ties the core to the most complex channel and makes every
  addition painful.
