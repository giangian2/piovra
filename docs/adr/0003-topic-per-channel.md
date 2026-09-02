# ADR-0003 — Separate command topics per channel type

**Status**: Proposed — 2026-09-02

## Context
`publication-service` emits commands destined for different drivers. Two options: a single
`piovra.channel.command.v1` topic with a `channel-id` header and consumer-side filtering, or one
topic per channel type (`…command.ebay.v1`, `…command.woocommerce.v1`).

## Decision
**One topic per channel type**, each with its own partitions, retention and priority. On top of that,
per channel, separate topics per priority: `command.<channel>.high.v1` (stock), `.normal.v1`
(content), `.low.v1` (bulk resyncs).

## Consequences
**Positive**
- Each driver reads only what concerns it: no bandwidth wasted deserializing and discarding.
- Real isolation: an eBay backlog does not lengthen the path of WooCommerce messages.
- Partitions tuned to the parallelism each marketplace allows.
- Priority becomes modellable: Kafka has no priority within a partition, and separate topics are the
  only practical mechanism.

**Negative**
- More topics to create and monitor; adding a channel requires provisioning (automated via
  Terraform/IaC).
- The publication service has to know the `channelType → topic` map (configuration data, not logic).

## Alternatives rejected
- **A single topic with consumer-side filtering**: every driver would read 100 % of the traffic to use
  a fraction of it; the coupled retention and the lack of isolation surface at the first incident.
