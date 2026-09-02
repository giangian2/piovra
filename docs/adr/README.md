# Architecture Decision Records

One decision per file, numbered and immutable: a decision that is later overturned is not rewritten,
it is **superseded** by a new ADR that references it.

Format: context → decision → consequences → alternatives rejected.

| # | Title | Status |
|---|---|---|
| [0001](0001-canonical-model.md) | Canonical model with per-channel projections | Accepted |
| [0002](0002-kafka-sku-key.md) | Partition key = canonical SKU | Accepted |
| [0003](0003-topic-per-channel.md) | Command topics per channel type | Proposed |
| 0004 | Java plus virtual threads instead of reactive | Accepted |
| 0005 | Monorepo in phase 1 | Accepted |
