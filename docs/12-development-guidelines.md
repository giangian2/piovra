# 12 — Architecture and development guidelines

> These rules exist for one reason: a year from now the code has to be modifiable by someone who was
> not there when it was written. Every entry states **what to do**, **why**, and — where possible —
> **how it is verified**, because a rule nobody checks stops holding within three months.

---

## 1. Single point — one thing, one place

The project's guiding principle. Every concept has **exactly one place where it is defined**;
everything else references it.

| Concept | The one place | Why there |
|---|---|---|
| Topic names | `Topics` in `piovra-events` | a hand-typed string does not fail: it silently creates a new topic |
| Kafka headers | `EventHeaders` | same reason, with the added twist that logs stop being correlatable |
| Error taxonomy | `ErrorClass` in `piovra-common` | it is the class, not the native code, that decides whether we retry |
| MDC keys | `MdcKeys` | a wrong key breaks nothing, it merely makes the logs useless |
| Partition key | `Ids.partitionKey()` | it is the ordering guarantee: computing it two ways destroys it |
| Channel capabilities | `DriverCapabilities` | avoids `if (channelType == EBAY)` scattered through the core |
| Channel policy | `ChannelPolicy` | stock buffers and markups in one place, not in every mapper |
| Snapshot format | `FieldGroupHasher` | two different serializations mean the diff is always positive |

**The test**: if adding a marketplace requires changing more than one file outside its driver module,
"single point" has been violated somewhere. Go find it.

**The uncomfortable corollary**: a value duplicated "just for now" never comes back. If it is needed
in two places, extract it immediately, even when the extraction looks disproportionate.

---

## 2. Encapsulation

### 2.1 State does not leave home

- **Immutable `record`s everywhere** in the domain and in the contracts. No setters, no empty
  constructors, no Lombok `@Data`.
- Collections defend themselves in the **compact constructor**, always:
  ```java
  public CanonicalProduct {
      media = media == null ? List.of() : List.copyOf(media);
  }
  ```
  Without `copyOf`, whoever handed you the list can still mutate it afterwards. It is the quietest
  way to corrupt a snapshot.
- Changes produce **new instances**, with names that say what changes:
  `listing.markPublished(...)`, `variant.withPrice(...)`. Never a `setState()`.

### 2.2 Invariants belong in the constructor, not in the callers

`Sku` normalizes and validates; `Money` pins the currency scale; `OrderLine` rejects non-positive
quantities. An object **must not be able to exist in an invalid state**, so no downstream service has
to remember to check and the checks do not scatter.

On `Money`, the fixed scale is not fussiness: `19.90` and `19.9` are the same price, and if they
enter the diff as different strings you republish the whole catalog to every marketplace.

### 2.3 Persistence entities do not leave their module

`piovra-model` contains **only** what appears in events or in the SPI. JPA entities are
package-private where possible and never cross a module boundary: they are mapped to the canonical
model in the adapter. Verified by
`ArchitectureTest.the_canonical_model_is_not_a_jpa_model`.

The practical reason: if the JPA entity is also the API DTO, the first schema migration becomes a
breaking change to the public API.

### 2.4 Secrets have no `toString()`

`ChannelCredentials.toString()` returns `ChannelCredentials[***]`. A token in a log is a security
incident, and logs end up in places secrets must not reach. The same rule covers buyer addresses:
masked in logs, in cleartext only in the database with controlled access.

---

## 3. Cross-cutting concerns

A concern is cross-cutting when it **repeats identically in many places** and is **orthogonal to the
domain** of the code it runs through. Three real examples in Piovra:

| Concern | Where it appears | If hand-written |
|---|---|---|
| Consumer idempotency | every Kafka consumer in the system | `if (alreadySeen) return;` repeated ~15 times, with 15 ways of getting the release-on-failure wrong |
| Metrics for external calls | every connector method | adding one metric tag means remembering 20 call sites |
| Auditing human actions | resyncs, adjustments, DLQ replays | you forget to record exactly the action you later need during an incident |

They all live in **`libs/piovra-crosscutting`**, as an annotation plus an aspect.

### 3.1 The available annotations

```java
// Runs once per key, even though Kafka delivers the event twice (at-least-once, always).
@Idempotent(key = "'cmd:' + #command.commandId()", ttl = "P7D")
public void handle(ChannelCommand command) { ... }

// Produces piovra_marketplace_call_duration_seconds{channel,operation,outcome}.
@ChannelCall(operation = "INVENTORY")
public UpdateResult pushStock(List<InventoryUpdate> updates) { ... }

// Records the action in the append-only audit log, whether it succeeds or fails.
@Audited(action = "product-resync", target = "#sku.value()")
public void forceResync(Sku sku, ChannelId channel) { ... }
```

### 3.2 Rules for adding a new one

1. **The logic must be identical at every call site.** If it varies case by case, it is not
   cross-cutting: it is domain logic in disguise, and it should be written out explicitly.
2. **It must depend on a port, not an implementation.** `IdempotencyAspect` knows
   `IdempotencyStore`, not Postgres: a table in the connectors (it must survive restarts), a
   `HashSet` in tests.
3. **It must be testable without a Spring context**, using `AspectJProxyFactory`. If testing it
   requires booting the application, the aspect is doing too much.
4. **The order must be declared** with `@Order`. `IdempotencyAspect` runs first: there is no point
   measuring or retrying an execution that has to be skipped.
5. **It activates only when its port is implemented** (`@ConditionalOnBean`). An annotation that does
   nothing because a bean was missing is a bug you discover in production.

### 3.3 When NOT to use an aspect

This is the half that usually goes missing, and without it AOP becomes a problem:

- **Never for business logic.** An aspect makes the flow invisible to whoever reads the method. That
  is an acceptable price for a metric, never for a rule that decides what the customer sees.
- **Never inside the driver modules.** `piovra-crosscutting` depends on Spring; the drivers do not,
  and `DriverIndependenceTest` would fail. The aspect applies **at the boundary**, in the connector
  that invokes the driver. The driver only translates.
- **Beware of self-invocation.** Spring AOP works through proxies: when a public method calls another
  method **on the same class**, the proxy is bypassed and the annotation does nothing, with no error
  at all. This is the number-one way these mechanisms fail silently. The fix is to move the annotated
  method into a different bean. Only if that becomes genuinely impractical should compile-time
  **AspectJ weaving** be considered — it solves the problem but complicates the build and makes the
  source less faithful to what actually runs.
- **Only on public methods of Spring beans.** On `private`, `final`, or on a class the context does
  not manage, the annotation is silently inert.

> Practical rule: if someone reading the method could be surprised by what happens, the aspect is the
> wrong tool.

---

## 4. Internal module structure (hexagonal)

```
dev.piovra.<service>/
├── domain/         entities, value objects, rules. ZERO framework
│   ├── model/
│   └── service/    DiffCalculator, ChannelProjector, StockLedger…
├── application/    use cases, orchestration, transactions
│   ├── port/in/    use-case interfaces
│   └── port/out/   ProductRepository, EventPublisher (interfaces)
└── adapter/
    ├── in/kafka/   consumers
    ├── in/web/     REST controllers
    ├── out/persistence/  JPA, DB entities, mappers
    └── out/kafka/  outbox relay
```

**The pure-domain rule**, enforced by `ArchitectureTest.the_domain_does_not_know_spring`: `..domain..`
packages import neither Spring, nor JPA, nor Kafka.

This is not purism. `DiffCalculatorTest` runs 9 cases in **0.3 seconds** without booting anything:
that is what makes it feasible to actually cover the diff's edge cases instead of testing two of them
and hoping. A test that needs 8 seconds of Spring context gets written once and never again.

**Dependencies always point inward**: `adapter → application → domain`. The domain does not know who
calls it.

---

## 5. Coding conventions

### 5.1 Naming

- Events: noun plus past participle — `ProductChanged`, `OrderAccepted`. They are facts that happened.
- Commands: imperative verb — `UpsertListing`, `EndListing`. They are intents.
- Outbound ports: `<Thing>Repository`, `<Thing>Publisher`, `<Thing>Store`.
- Test names describe the behaviour: `an_identical_feed_produces_no_call_at_all()`. The test name is
  documentation.

### 5.2 Errors

- Every application exception extends `PiovraException` and carries an `ErrorClass` plus a `code`.
- **Every permanent error must carry a `suggestedAction`.** If it does not, either it is our bug or a
  translation is missing: this is a code review criterion, not a suggestion.
- Drivers translate in `translate(Exception)` by reading the marketplace's real codes, never by
  guessing from the HTTP status.

### 5.3 Concurrency

- **No thread pools.** `Executors.newVirtualThreadPerTaskExecutor()` or `StructuredTaskScope` for
  fail-fast fan-out. A pool of virtual threads is an anti-pattern that reintroduces the ceiling
  virtual threads remove.
- **`ReentrantLock`, not `synchronized`**, around blocking calls. On Java 25 pinning is gone
  ([JEP 491](https://openjdk.org/jeps/491)), but `ReentrantLock` remains preferable for `tryLock`
  with a timeout.
- **Parallelism is bounded by the rate limiter**, not by a pool size: the limit is then declared where
  it means something instead of being a side effect of configuration.
- **Never parallelize records from the same Kafka partition**: per-SKU ordering is the guarantee
  everything rests on ([ADR-0002](adr/0002-kafka-sku-key.md)).

### 5.4 Transactions

- **Never wrap an external HTTP call in `@Transactional`.** A slow marketplace holds a transaction and
  a pool connection open: with enough slow calls the database stops before the marketplace does.
- Database write plus Kafka publish: always the **outbox**, in the same local transaction. No sagas,
  no 2PC.
- `spring.jpa.open-in-view: false`, always. It is already in the configuration files.

### 5.5 Migrations

- A released migration is never modified.
- Always **backward compatible** (expand/contract): add the column, release the code, remove the old
  one in a later release. Rolling updates require it.
- Each module ships its own under `db/migration/<schema>/` and touches only its own schema — which is
  enforced at the Postgres level by grants, not by goodwill.

---

## 6. Testing

| Level | What it verifies | How long it may take |
|---|---|---|
| Domain | diffs, projections, stock rules, invariants | milliseconds, no context |
| Aspects | cross-cutting behaviour, with `AspectJProxyFactory` | milliseconds |
| Architecture | the rules in this document (ArchUnit) | seconds |
| Integration | consumers, repositories, outbox (Testcontainers) | tens of seconds |
| Driver contract | the TCK, against WireMock | seconds |
| Sandbox | the TCK against the real marketplaces | a separate nightly job |

**Writing rule**: a test must be able to fail for exactly one reason. If the name contains "and", it
is probably two tests.

**The test case that must never be missing**: receiving the same event twice. That is Kafka's normal
mode of operation, not an edge case.

---

## 7. What fails the build

These are not recommendations, they are pipeline gates
([CI](../.github/workflows/ci.yml)).

| Gate | What it prevents |
|---|---|
| `maven-enforcer-plugin` | building on a JDK older than 25 |
| `ArchitectureTest` | a service depending on another; a domain importing Spring; an impure SPI |
| `DriverIndependenceTest` | a driver importing Spring, Kafka or the events |
| `spotless:check` | inconsistent formatting |
| Schema compatibility | an event changed in a non-backward-compatible way |
| Driver TCK | a driver that does not honour the contract |

---

## 8. Code review checklist

- [ ] Does the value I introduced already exist somewhere? (§1)
- [ ] Are collections in records copied in the compact constructor? (§2.1)
- [ ] Are the invariants in the constructor rather than in the caller? (§2.2)
- [ ] Is a JPA entity crossing a module boundary? (§2.3)
- [ ] Is this repeated logic genuinely cross-cutting, or domain logic in disguise? (§3.2)
- [ ] Is the annotated method public, on a Spring bean, called from outside? (§3.3)
- [ ] Does the new domain code import anything from Spring? (§4)
- [ ] Does the permanent error I added carry a `suggestedAction`? (§5.2)
- [ ] Is there a `@Transactional` wrapped around an external call? (§5.4)
- [ ] Is there a test for the same event being delivered twice? (§6)
