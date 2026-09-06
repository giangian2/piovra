-- GPSR manufacturer/responsible-person contacts, reusable across products (referenced by id from
-- CanonicalProduct.manufacturerProfileId / responsiblePersonProfileId). Flat columns, not JSONB
-- like products: profiles are looked up by (tenant_id, type), not by a single point lookup.
CREATE TABLE catalog.compliance_profile (
    id           TEXT PRIMARY KEY,
    tenant_id    TEXT NOT NULL,
    type         TEXT NOT NULL,
    name         TEXT NOT NULL,
    street       TEXT NOT NULL,
    city         TEXT NOT NULL,
    postal_code  TEXT NOT NULL,
    country_code TEXT NOT NULL,
    email        TEXT NOT NULL,
    phone        TEXT,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ON catalog.compliance_profile (tenant_id, type);
