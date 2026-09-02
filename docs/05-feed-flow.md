# 05 — Feed flow (catalog upload)

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant S as Source (FTP/HTTP)
    participant I as feed-ingestion
    participant O as Object storage
    participant K as Kafka
    participant P as feed-processor
    participant C as catalog-service

    S->>I: file (CSV/XML/JSON/XLSX)
    I->>I: wait for completion + sha256
    I->>O: PUT immutable raw file
    I->>I: INSERT feed(RECEIVED) + outbox  [1 tx]
    I->>K: feed.received
    K->>P: consume
    P->>O: GET stream
    loop chunks of N records
        P->>P: parse -> map -> validate
        P->>K: catalog.product.upsert (batched, key=sku)
        P->>K: feed.record.rejected (for rejects)
    end
    P->>K: feed.completed{ok, rejected, skipped}
    K->>C: consume upsert
    C->>C: diff against current state
    alt changed
        C->>C: revision++ ; persist + outbox [1 tx]
        C->>K: catalog.product.changed
    else identical
        C->>C: no-op (no event)
    end
```

## 2. Ingestion

### 2.1 FTP/SFTP
Prefer **SFTP**. The classic problem is reading a file while it is still uploading; three strategies,
configurable per source:

| Strategy | How | When |
|---|---|---|
| **Sentinel file** | the supplier uploads `catalog.csv` then `catalog.csv.done` | ideal, if the supplier cooperates |
| **Quiet period** | the file is "ready" when size and mtime are stable for N seconds | default |
| **Atomic rename** | upload to `.tmp` then `mv` | excellent, where supported |

After ingestion the file is **moved** to `processed/` or `failed/` on the FTP server, never left in
the inbox.

### 2.2 HTTP
`POST /v1/feeds` with `sourceId`, `mode` (`DELTA`|`FULL`), `format` and the file body. It answers
`202 Accepted` plus a `feedId` and a status-polling URL. For large files: direct upload to S3 with a
**presigned URL**, then `POST /v1/feeds/register` with the key — the gateway is not a tunnel for a
2 GB CSV.

### 2.3 Pull
A scheduled job (`channel-config` → `sources`) downloads from a URL or remote FTP on a cron. The
downstream pipeline is identical.

## 3. Mapping profile

Every source has a versioned declarative profile (YAML in the database, editable from the console):

```yaml
sourceId: supplier-x
format: CSV
csv: { delimiter: ";", quote: '"', charset: "ISO-8859-1", header: true }
mode: DELTA
idempotency: { skipIfSameHash: true }
mapping:
  sku:            { column: "Code", required: true, transform: [trim, upper] }
  parentSku:      { column: "ParentCode" }
  title.it:       { column: "Description", required: true, transform: [trim, "truncate:80"] }
  description.it: { column: "LongDescription", transform: [stripHtmlUnsafeTags] }
  brand:          { column: "Brand", default: "Acme" }
  price.amount:   { column: "SellPrice", transform: ["decimal:2", "comma2dot"], required: true }
  price.currency: { const: "EUR" }
  stock:          { column: "OnHand", transform: ["int", "clampMin:0"] }
  identifiers.ean:{ column: "EAN13", validate: "ean13" }
  categoryPath:   { column: "Category", transform: ["split:/"] }
  media[].url:    { columns: ["Photo1","Photo2","Photo3"], transform: [nonEmpty] }
  attributes.*:   { columnsPrefixed: "ATTR_" }
validation:
  - rule: "price.amount > 0"        onFail: REJECT
  - rule: "media.length >= 1"       onFail: WARN
  - rule: "identifiers.ean != null" onFail: WARN
```

**Why declarative**: adding a supplier must not require a deployment. The transformations are a
closed, tested set, not arbitrary code.

## 4. High-performance parsing

- **Always streaming**: `univocity-parsers` (CSV, the fastest on the JVM), StAX (XML), Jackson
  `JsonParser` (JSON Lines or streamed arrays), `monitorjbl/excel-streaming-reader` (XLSX).
- Processing in **chunks** (1,000 records by default) with asynchronous batched Kafka production; a
  single `flush` per chunk.
- **No** database query per record: the processor does not know the current state — the
  `catalog-service` performs the diff. This keeps the processor stateless and linearly scalable.
- Parallelism: different files in parallel (one per consumer); inside a file record order is
  irrelevant as long as the Kafka key is the SKU.
- Very large feeds: split into logical shards and publish several `feed.chunk.ready` events to
  parallelize a single file too.

## 5. Validation and rejects

Three levels:
1. **Structural** — the file is readable and the expected columns exist. On failure the whole feed is
   `FAILED` and no record is published.
2. **Per record** — types, required fields, rules. An invalid record is rejected and the others carry
   on.
3. **Sanity threshold** — if rejects exceed *X %* (default 20 %) or if a `FULL` feed contains fewer
   than *Y %* (default 50 %) of the expected products, the feed is **suspended** pending manual
   approval. This prevents the classic disaster: a truncated feed delisting the entire catalog.

The reject report is downloadable as CSV from the console and keeps the line number and the original
value.

## 6. FULL versus DELTA feeds

| | DELTA | FULL |
|---|---|---|
| Missing records | ignored | marked `DISCONTINUED` → `channel.command.END` |
| Typical use | frequent updates (prices, stock) | nightly realignment |
| Risk | drift on deletions | mass delisting from a corrupt feed |
| Protection | periodic reconciliation | sanity threshold plus dry run |

`FULL` is implemented by comparing the set of SKUs in the feed with those active for that source (a
query against `catalog-service` at the end of the feed, not per record).

## 7. Feed states

```
RECEIVED → PARSING → PROCESSED
                  ↘ PARTIALLY_PROCESSED (with rejects)
                  ↘ SUSPENDED (sanity threshold exceeded) → [approve] → PARSING
                  ↘ FAILED
         ↘ SKIPPED_DUPLICATE (same sha256 as the previous feed)
```

## 8. Idempotency

- The same file sent twice has the same `sha256` and becomes `SKIPPED_DUPLICATE` (when enabled).
- Reprocessing a `feed.received` event (a Kafka retry) is harmless: the re-emitted upserts find no
  difference in `catalog-service`, which emits nothing.
