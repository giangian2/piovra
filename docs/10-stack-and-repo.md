# 10 — Technology stack and repository structure

## 1. Stack

| Area | Choice | Why |
|---|---|---|
| Language | **Java 25 LTS** | virtual threads: the drivers are I/O-bound, so reactive is not needed to scale. 25 is a requirement, not a preference — see below |
| Framework | **Spring Boot 4.x** | Spring Kafka, Spring Data, Actuator, Micrometer already integrated |
| Build | **Maven** multi-module (wrapper included) | reproducible builds, centralized `dependencyManagement` |
| Messaging | **Apache Kafka** + Schema Registry | immutable log, ordering by key, replay |
| Serialization | **Avro** (or Protobuf) | schema evolution verified in CI |
| Database | **PostgreSQL 16**, one schema per service | JSONB for snapshots and attributes, native partitioning |
| Migrations | **Flyway** | versioned inside each service module |
| Cache / rate limit | **Redis** | distributed token bucket, short-term dedup, token-refresh locks |
| Object storage | **S3 / MinIO** | immutable raw feeds |
| Secrets | **HashiCorp Vault** (or AWS SM) | marketplace credentials, rotation |
| HTTP client | JDK `HttpClient` in the drivers; generated clients for eBay | keeps driver modules framework-free |
| Resilience | **Resilience4j** | circuit breaker, bulkhead, retry, time limiter |
| Observability | **OpenTelemetry**, Prometheus, Grafana, Loki, Tempo | end-to-end tracing across Kafka |
| Testing | JUnit 5, AssertJ, **Testcontainers**, WireMock, Awaitility | |
| Containers | Docker (distroless/jlink images) | |
| Orchestration | Kubernetes + Helm; **KEDA** autoscaling on consumer lag | lag is the right metric to scale on |
| CI/CD | GitHub Actions | build, test, schema compatibility, images, deploy |
| Quality | Spotless, ArchUnit, OWASP dependency-check | ArchUnit forbids `core` from depending on the drivers |

**Why Java 25 and not 21.** With virtual threads, a `synchronized` block held across a blocking call
pins the virtual thread to its carrier and cancels out the benefit. On Java 21 the countermeasure is
replacing every `synchronized` with `ReentrantLock` and watching for pinning with
`-Djdk.tracePinnedThreads=full` — including inside third-party libraries, where you cannot
intervene. [JEP 491](https://openjdk.org/jeps/491), shipped in Java 24 and therefore in the 25 LTS,
removes the problem at the root. Since connectors are exactly the code that holds locks across
network calls, 25 is the project's floor: `maven-enforcer-plugin` fails the build on older JVMs, and
the development environment brings it along (`.devcontainer/`).

**Why not reactive.** With virtual threads, blocking and readable code sustains tens of thousands of
concurrent calls. WebFlux's complexity is not justified by this workload. The only blocking points
left are the ones that need to be (JDBC), which is precisely what virtual threads handle well.

## 2. Repository structure (monorepo)

```
piovra/
├── pom.xml                              # parent: dependencyManagement, plugins
├── mvnw / mvnw.cmd
├── scripts/mvnd                         # Maven inside a JDK 25 container
├── docs/                                # this documentation
├── libs/
│   ├── piovra-common/                   # ULID, Money, errors, utilities
│   ├── piovra-model/                    # canonical model (immutable records)
│   ├── piovra-events/                   # event contract: payloads, headers, topics
│   ├── piovra-kafka-support/            # producer/consumer factories, DLQ, retry topics, tracing
│   ├── piovra-outbox/                   # reusable transactional outbox
│   ├── piovra-crosscutting/             # annotations and aspects: idempotency, metrics, audit
│   ├── piovra-driver-spi/               # driver interfaces (no framework dependency)
│   ├── piovra-driver-tck/               # contract tests for the drivers
│   └── piovra-test-support/             # fixtures, Testcontainers, feed generators
├── drivers/
│   ├── piovra-driver-ebay/              # plus OpenAPI client generation
│   └── piovra-driver-woocommerce/
├── services/
│   ├── piovra-catalog/ · piovra-inventory/ · piovra-order/
│   ├── piovra-publication/ · piovra-channel-config/
│   └── piovra-feed-ingestion/ · piovra-feed-processor/
├── apps/                                # the deployables
│   ├── piovra-core/ · piovra-feed/
│   └── piovra-connector-woocommerce/ · piovra-connector-ebay/
├── deploy/
│   ├── local/docker-compose.yml         # kafka, postgres, redis, minio, wiremock
│   └── Dockerfile                       # runtime image, parameterized by APP
└── .devcontainer/                       # JDK 25 development environment
```

**Modules versus deployables.** 18 code modules produce 4 processes. Boundaries are finer than
release units, so moving a module between applications — or giving it its own — is packaging, not a
refactor. The connectors are separate from day one because isolation between channels is why they
exist; the other services start packaged together and split when there is a measured reason:
different scaling profile, need for failure isolation, or a different release cadence.

**Monorepo** in phase 1: refactoring the SPI and the events in a single PR, one version, simple CI.
Moving to multiple repositories is worth reconsidering when there is more than one team — not before.

## 3. Internal structure of a service (hexagonal)

```
piovra-catalog/
└── src/main/java/dev/piovra/catalog/
    ├── domain/            # entities, value objects, rules. ZERO framework dependencies
    │   ├── model/
    │   └── service/       # ProductUpsertService, DiffCalculator…
    ├── application/       # use cases, orchestration, transactions
    │   ├── port/in/       # use-case interfaces
    │   └── port/out/      # ProductRepository, EventPublisher (interfaces)
    └── adapter/
        ├── in/kafka/      # consumers
        ├── in/web/        # REST controllers
        ├── out/persistence/  # JPA/JDBC, DB entities, mappers
        └── out/kafka/     # outbox relay
```
Enforced by **ArchUnit**: `domain` imports nothing from `adapter` and nothing from Spring. Diffing,
payload construction and stock rules live in the domain, so they are testable without a Spring
context, in milliseconds.

## 4. Conventions

- **Packages**: `dev.piovra.<service>.…`
- **Event naming**: noun plus past participle (`ProductChanged`); commands: imperative verb
  (`UpsertListing`).
- **Immutable DTOs**: `record` everywhere; no setters in the domain.
- **No `Object`/`Map` fields in the domain** except `attributes`, which is deliberately open.
- **No `@Transactional` wrapping an external HTTP call.**
- **Every Kafka consumer**: idempotent, manual commit, with a catch that classifies the error and
  decides retry versus DLQ.
- **Migrations**: never modify a released migration.
- **Feature flags** per channel (`channel.enabled`) and for risky operations (mass delisting).

See [12 — Development guidelines](12-development-guidelines.md) for the full set.

## 5. Local environment

The JDK is not installed by hand: the project brings its own environment, so the Java version is a
property of the repository rather than of whoever opens it.

| Where | What | File |
|---|---|---|
| Development shell | a shell in the JDK 25 image, workspace mounted, host Docker gid resolved at run time | `scripts/devshell` |
| One-off Maven command | a thin wrapper over `devshell`, so both share one image, user and cache volume | `scripts/mvnd` |
| IDE integration | JDK 25 devcontainer, Maven cache on a volume | `.devcontainer/` |
| CI | `actions/setup-java` with temurin 25 | `.github/workflows/ci.yml` |
| Runtime | multi-stage: build on `temurin:25-jdk`, run on `temurin:25-jre` | `deploy/Dockerfile` |
| Infrastructure | Kafka KRaft, Postgres, Redis, MinIO, WireMock, Kafka UI | `deploy/local/docker-compose.yml` |

```bash
docker compose -f deploy/local/docker-compose.yml up -d
./scripts/devshell              # shell in the container, then ./mvnw as usual
./scripts/mvnd clean install    # or a single Maven command
./mvnw clean install            # if the host already has a JDK 25
```

The base image is pinned to `eclipse-temurin:25-jdk-noble` (Ubuntu 24.04 LTS) rather than the
floating `:25-jdk` tag: the latter currently resolves to Ubuntu 26.04, which is newer than parts of
the tooling ecosystem, and a floating base makes the build a moving target.

Reaching the host's Docker daemon from inside a container is a group-id question, and that gid
differs on every machine, so it cannot be hardcoded anywhere. Each path resolves it at run time:
`scripts/devshell` reads it and passes `--group-add`; the devcontainer's `postStartCommand` moves the
container's docker group onto it, which its terminals pick up because devcontainer sessions are
spawned with `docker exec`, and that re-resolves group membership from `/etc/group` every time.

Both mount the workspace at `/workspaces/piovra`, the devcontainer tooling's own default. Absolute
paths leak into `target/` and into IDE configuration, so the two entry points must agree — and the
agreement follows the tool's convention rather than overriding it: on WSL the local path has to be
translated from a Windows UNC path, and that is not a translation worth taking over.

The compose file creates **one Postgres schema and user per service module**
(`postgres-init.sql`), each granted only on its own schema: the "no cross-service joins" rule fails
at runtime instead of relying on discipline. The `local` profile points the drivers at WireMock; the
`sandbox` profile points them at the marketplaces' real sandboxes.

## 6. Deployment

- One Deployment per service; connectors with `replicas ≤ #partitions` of their command topic.
- **KEDA** scales on consumer lag (`lagThreshold`), not CPU.
- `PodDisruptionBudget` and a `terminationGracePeriodSeconds` long enough to finish the current poll
  and commit.
- Rolling updates: in-flight events are already designed to be reprocessed, so no downtime is
  required.
- Database migrations run as a pre-deploy Flyway job and must be **backward compatible**
  (expand/contract): add the column, release the code, drop the old one in a later release.
