# Security Policy

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report privately through GitHub's
[private vulnerability reporting](https://github.com/giangian2/piovra/security/advisories/new)
for this repository. Include enough detail to reproduce the issue: affected version or commit,
component, and the impact you observed.

You can expect an acknowledgement within a few days. Please give us reasonable time to ship a fix
before disclosing publicly.

## Scope and sensitive areas

Piovra handles marketplace credentials, order data and buyer personal information. The areas where a
defect matters most:

- **Credential handling** — marketplace tokens and consumer secrets. They belong in Vault and must
  never be logged. `ChannelCredentials` deliberately has no meaningful `toString()`.
- **Personal data** — buyer names, addresses and email addresses arrive with every order. They are
  masked in logs and stored with restricted access.
- **Webhook endpoints** — signature verification (for instance `X-WC-Webhook-Signature`) is
  security-relevant, not a formality.
- **Multi-tenancy** — `tenantId` is present throughout; anything that lets one tenant read another
  tenant's data is a vulnerability.

## Not vulnerabilities

The default credentials in `deploy/local/docker-compose.yml` and in the `application.yml` files are
local development defaults, overridden by environment variables in every real deployment. They are
intentionally in the repository so `docker compose up` works out of the box.
