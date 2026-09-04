# 🐙 Piovra

**Piovra** (Italian for *octopus*) is a distributed, high-performance system for synchronizing
product catalogs, stock levels and orders across multiple marketplaces and e-commerce platforms.

The name mirrors the architecture:

- **the head** — the canonical core (catalog, inventory, orders): the single source of truth,
  agnostic of any marketplace;
- **the tentacles** — the marketplace *drivers* (eBay, WooCommerce, …): independent, individually
  deployable modules that speak each channel's dialect;
- **the nervous system** — Kafka: a distributed, immutable, per-product-ordered log that decouples
  head from tentacles.

## What it does

1. **Feed ingestion** — receives product feeds over **FTP/SFTP** or **HTTP** (push or pull), stores
   them immutably and normalizes them into the canonical model.
2. **Publish / upsert** — propagates the catalog to every marketplace with *upsert* semantics:
   creates the listing when it does not exist, otherwise updates **only the fields that actually
   changed** (snapshot diff).
3. **Order intake** — pulls orders from every marketplace (polling and/or webhooks), normalizes and
   deduplicates them.
4. **Inventory sync** — every order decrements canonical stock, and the change is propagated back
   **to all other channels** to prevent overselling.
5. **Error tracking** — every synchronization attempt is recorded, classified, retried per policy
   and, when unrecoverable, sent to a DLQ with replay tooling.

## Status

> ⚠️ Phase: **early development.** The core (catalog + channel-config + publication) runs end to end
> against Kafka and Postgres, with automated test coverage. Inventory, order, feed parsing and driver
> bodies are still to come. See [Status of the skeleton](#status-of-the-skeleton).

## Documentation index

| Doc | Content |
|---|---|
| [01 — Architecture](docs/01-architecture.md) | Overview, principles, diagrams, key decisions |
| [02 — Services](docs/02-services.md) | Responsibilities, APIs and boundaries of each service |
| [03 — Data model](docs/03-data-model.md) | Canonical model: Product, Variant, Offer, Listing, Order, Stock |
| [04 — Kafka & events](docs/04-kafka-events.md) | Topics, keys, partitioning, schemas, retention, DLQ |
| [05 — Feed flow](docs/05-feed-flow.md) | FTP/HTTP ingestion, parsing, validation, staging |
| [06 — Publish flow](docs/06-publish-flow.md) | Diffing, idempotency, ordering, batching, rate limits |
| [07 — Order flow](docs/07-order-flow.md) | Order intake, cross-channel decrement, anti-oversell |
| [08 — Marketplace drivers](docs/08-marketplace-drivers.md) | Driver SPI, eBay driver, WooCommerce driver |
| [09 — Errors & observability](docs/09-errors-observability.md) | Error taxonomy, retries, circuit breakers, metrics, tracing |
| [10 — Stack & repository](docs/10-stack-and-repo.md) | Technologies, Maven layout, build, local environment |
| [11 — Roadmap](docs/11-roadmap.md) | Development phases and exit criteria |
| [12 — Development guidelines](docs/12-development-guidelines.md) | Single point, encapsulation, cross-cutting concerns, AOP, conventions |
| [ADRs](docs/adr/) | Architecture Decision Records |

## Non-functional targets

| Requirement | Target |
|---|---|
| Ingestion throughput | ≥ 5,000 products/s per logical staging partition |
| p95 latency catalog → marketplace | < 60 s (excluding channel rate limits) |
| p95 latency order → decrement on other channels | < 15 s |
| Overselling | zero under nominal conditions; mitigated by stock buffers |
| Core availability | 99.9 % |
| Event delivery | at-least-once plus downstream idempotency |
| Recovery from a marketplace outage | resume without loss, with a compacted backlog |

## Principles

- **Single source of truth**: the canonical catalog and stock live in Piovra, not in the marketplaces.
- **Event-driven, not chained RPC**: services talk over Kafka; synchronous calls only go outwards,
  to external APIs.
- **Idempotency everywhere**: every command carries an idempotency key; reprocessing an event
  produces no different effect.
- **Ordering per product, not globally**: the Kafka key is the canonical SKU, which guarantees
  ordering where it matters and maximum parallelism everywhere else.
- **Isolated drivers**: a problem with the eBay API must not degrade WooCommerce (bulkheads and
  separate deployments).
- **No business logic in the drivers**: drivers translate, they do not decide.

## Repository structure

A Maven monorepo, **22 modules** across four levels. The distinction that matters is between
modules (code boundaries) and deployables (release units): 18 code modules plus 4 deployables.

```
piovra/
├── libs/       contracts and shared infrastructure (JARs, never deployed)
│   ├── piovra-common          Sku, Money, ChannelId, error taxonomy
│   ├── piovra-model           canonical model: only what appears in events or the SPI
│   ├── piovra-events          Kafka contract: payloads, headers, topic names
│   ├── piovra-driver-spi      driver contract. Zero Spring, zero Kafka
│   ├── piovra-crosscutting    annotations and aspects: idempotency, metrics, audit
│   ├── piovra-kafka-support   producers/consumers, retry topics, DLQ, tracing
│   ├── piovra-outbox          reusable transactional outbox
│   ├── piovra-driver-tck      contract tests every driver must pass
│   └── piovra-test-support    fixtures and test containers
├── drivers/    one tentacle per marketplace (JARs, unaware of Kafka)
│   ├── piovra-driver-woocommerce
│   └── piovra-driver-ebay
├── services/   business modules (JARs, one DB schema each)
│   ├── piovra-catalog · piovra-inventory · piovra-order
│   ├── piovra-publication · piovra-channel-config
│   └── piovra-feed-ingestion · piovra-feed-processor
├── apps/       the 4 deployables
│   ├── piovra-core                    catalog+inventory+order+publication+channel-config
│   ├── piovra-feed                    ingestion+processor
│   ├── piovra-connector-woocommerce   WooCommerce driver host
│   └── piovra-connector-ebay          eBay driver host
└── deploy/local/  kafka, postgres, redis, minio, wiremock
```

The connectors are separate from day one because isolation between channels is the whole reason
they exist. The other services start packaged together and split apart when there is a measured
reason to: they already communicate only through Kafka and already own distinct DB schemas, so
extracting one is a packaging change, not a refactor.

### Dependency rules (verified by ArchUnit, not merely documented)

1. No service depends on another service.
2. Drivers know nothing about Kafka, Spring, or any service.
3. `..domain..` packages do not import Spring, JPA or Kafka.
4. The canonical model is not a persistence model.

## Status of the skeleton

| Area | Status |
|---|---|
| Maven structure, 22 modules, green build on JDK 25 | ✅ |
| Devcontainer, CI, runtime images (same Java major everywhere) | ✅ |
| Contracts: canonical model, events, driver SPI | ✅ |
| `piovra-kafka-support`: JSON (de)serialization, MDC propagation, DLQ error handler | ✅ unit + Testcontainers tests |
| `piovra-outbox`: transactional outbox (poll-and-publish relay, one table per schema) | ✅ Testcontainers tests |
| `piovra-crosscutting`: `@Idempotent`, `@ChannelCall`, `@Audited` plus aspects; `web` package (correlation filter, global exception handler) | ✅ unit tests |
| **`channel-config`**: registry, REST API, outbox → `channel.config.v1` | ✅ persistence + outbox + HTTP tests |
| **`catalog`**: upsert/diff domain logic, REST API, outbox → `catalog.product.changed.v1`, first-noise-filter no-op | ✅ domain + persistence + outbox + HTTP tests |
| **`publication`**: existing diff engine (`ChannelProjector`/`DiffCalculator`, untouched) wired to real consumers, `channel_listing` persistence, idempotent command emission | ✅ domain + Testcontainers consumer tests, including the "same event twice" case |
| Diff engine and per-channel projection (`publication` domain) | ✅ 9 tests |
| Engine bootstrap (all 4 apps): Kafka (de)serializers, structured JSON logging, health probes | ✅ |
| S3-compatible object storage wiring (`feed-ingestion`, `S3Client` + health check) | ✅ |
| Driver TCK | ✅ complete skeleton |
| WooCommerce / eBay drivers | 🚧 structure, capabilities and error translation; bodies pending |
| `inventory` / `order` services | 🚧 empty modules (deferred to a follow-up step) |
| `feed-ingestion` / `feed-processor` business logic (parsing, mapping, S3 upload) | 🚧 not started; only the app skeleton and S3 client are wired |
| Local Docker environment | ✅ |
| Connector command/result consumers, order polling | ⬜ to do |

## Build

**The project requires JDK 25.** Not a preference: since [JEP 491](https://openjdk.org/jeps/491) a
`synchronized` block no longer pins a virtual thread to its carrier thread. On 21, any library
holding a `synchronized` monitor across a blocking call cancels out the benefit of virtual threads,
which is exactly what a connector does. `maven-enforcer-plugin` fails the build on older JVMs, with
a message that says how to get the right one.

The same Java major applies everywhere: devcontainer, CI, runtime images.

### A shell in the development container (simplest)

No IDE integration required. `scripts/devshell` builds the image from `.devcontainer/Dockerfile`,
mounts the workspace, and reads the host's Docker group id at run time so Testcontainers can reach
the daemon.

```bash
docker compose -f deploy/local/docker-compose.yml up -d   # kafka, postgres, redis, minio, wiremock

./scripts/devshell                                  # interactive shell, then use ./mvnw normally
./scripts/mvnd clean install                        # or run one Maven command and exit
./scripts/mvnd -pl services/piovra-publication test # just the diff engine
./scripts/mvnd spotless:apply                       # format before committing
```

### With the devcontainer

Open the folder in VS Code and accept "Reopen in Container". You get JDK 25, the workspace at the
same `/workspaces/piovra` path `devshell` uses, a Maven cache on a volume, and the environment already
pointed at the local infrastructure. Testcontainers works: a `postStartCommand` moves the
container's docker group onto the host socket's gid, which the terminals pick up because
devcontainer sessions are spawned with `docker exec` and it re-resolves group membership.

### With a local JDK 25

If the host already has one, `./mvnw` works directly — no container involved.

Kafka UI on :8090, MinIO on :9001, WireMock on :8089.

### Runtime images

```bash
docker build -f deploy/Dockerfile --build-arg APP=piovra-core -t piovra/core .
```

## Try the core end to end

With the local infrastructure up (`docker compose -f deploy/local/docker-compose.yml up -d`) and
`./scripts/mvnd -pl apps/piovra-core spring-boot:run`:

```bash
# 1. Register a channel - watch it land on the channel.config.v1 topic (Kafka UI: localhost:8090)
curl -X PUT localhost:8080/v1/channels/woo-main \
  -H 'Content-Type: application/json' \
  -d '{"type":"WOOCOMMERCE","marketplaceCode":"https://shop.test","enabled":true,
       "credentialsRef":"vault://x","policy":{"stockBuffer":0,"maxPublishableQty":99,
       "priceAdjustmentPercent":0,"endOnZero":false,"criticalStockThreshold":3,
       "requiresManualApproval":false},"categoryMapping":{},"settings":{}}'

# 2. Upsert a product - watch catalog.product.changed.v1, then channel.command.woocommerce.normal.v1
curl -X PUT localhost:8080/v1/products/TSHIRT-BASE \
  -H 'Content-Type: application/json' \
  -d '{"status":"ACTIVE","type":"SIMPLE","title":{"values":{"it":"T-shirt"}},
       "description":{"values":{"it":"desc"}},"brand":"Acme","categoryPath":["Test"],
       "identifiers":{},"media":[],"attributes":{},"variantAxes":[],
       "variants":[{"sku":"TSHIRT-BASE","identifiers":{},"axisValues":{},
         "price":{"amount":19.90,"currency":"EUR"},"weightGrams":null,"dimensions":null,
         "media":[],"attributes":{}}],"channelOverrides":{}}'

# 3. Resubmit the identical payload: 204, no new event (the "first noise filter")
```

This is exactly what `ProductChangedConsumerIT` and `ChannelConfigOutboxRelayIT` verify with
Testcontainers, minus the manual curl — see those tests for the same flow driven end to end
automatically.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the
[development guidelines](docs/12-development-guidelines.md). Security reports:
[SECURITY.md](SECURITY.md).

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE).
