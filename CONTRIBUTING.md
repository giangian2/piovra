# Contributing to Piovra

Thanks for considering a contribution. This document covers the practical parts; the reasoning behind
the rules lives in [docs/12-development-guidelines.md](docs/12-development-guidelines.md).

## Getting set up

The project requires **JDK 25** ([why](docs/10-stack-and-repo.md#1-stack)). You do not need to
install it — the repository brings its own environment:

```bash
docker compose -f deploy/local/docker-compose.yml up -d   # kafka, postgres, redis, minio, wiremock

./scripts/devshell            # a shell in the JDK 25 container, then ./mvnw as usual
./scripts/mvnd clean install  # or one Maven command in that container and exit
./mvnw clean install          # if your host already has a JDK 25
```

Opening the folder in VS Code and choosing "Reopen in Container" also works. Note that reaching the
host Docker socket from inside a container is a group-id matter and that gid differs per machine, so
if Testcontainers reports a permission error, run those tests via `./scripts/devshell`, which reads
the host gid at run time.

## Before opening a pull request

```bash
./scripts/mvnd spotless:apply   # formatting
./scripts/mvnd clean install    # all gates, exactly as CI runs them
```

CI enforces, and will fail on:

| Gate | What it prevents |
|---|---|
| `maven-enforcer-plugin` | building on a JDK older than 25 |
| `ArchitectureTest` | a service depending on another; a domain package importing Spring; an impure driver SPI |
| `DriverIndependenceTest` | a driver importing Spring, Kafka or the event contract |
| `spotless:check` | inconsistent formatting |
| Driver TCK | a driver that does not honour the contract |

## The rules most likely to trip you up

1. **No service depends on another service.** Services communicate only through Kafka events.
2. **Drivers know nothing about Spring or Kafka.** A driver translates; orchestration belongs to the
   connector.
3. **`..domain..` packages import no framework.** That is what keeps domain tests running in
   milliseconds.
4. **Every permanent error carries a `suggestedAction`** telling an operator what to do about it.
5. **Every Kafka consumer must be idempotent.** Receiving the same event twice is normal operation,
   not an edge case — and there should be a test for it.

## Commit messages and pull requests

- Write commit subjects in the imperative mood: "Add the WooCommerce order poller".
- One logical change per pull request. If the description needs an "and", consider splitting it.
- Describe *why*, not just *what*: the diff already says what.
- New behaviour needs a test. Bug fixes need a test that fails without the fix.

## Language

Code, comments, documentation, commit messages and issues are in **English**.

## Reporting security issues

Do not open a public issue. See [SECURITY.md](SECURITY.md).
