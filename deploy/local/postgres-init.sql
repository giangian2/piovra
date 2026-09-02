-- One schema and one user per service module, even while they run in the same process.
--
-- This is not pedantry: it is what makes the "no cross-service joins" rule enforceable at
-- runtime rather than left to discipline, and what turns the future split into separate
-- deployables into a packaging change instead of a refactor (docs/10-stack-and-repo.md).

CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS publication;
CREATE SCHEMA IF NOT EXISTS channel_config;
CREATE SCHEMA IF NOT EXISTS feed;
CREATE SCHEMA IF NOT EXISTS connector_woocommerce;
CREATE SCHEMA IF NOT EXISTS connector_ebay;

-- Locally one application user per deployable; in production one per service, each granted
-- only on its own schema.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'piovra_core') THEN
    CREATE ROLE piovra_core LOGIN PASSWORD 'piovra_core';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'piovra_feed') THEN
    CREATE ROLE piovra_feed LOGIN PASSWORD 'piovra_feed';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'piovra_connector_woocommerce') THEN
    CREATE ROLE piovra_connector_woocommerce LOGIN PASSWORD 'piovra_connector_woocommerce';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'piovra_connector_ebay') THEN
    CREATE ROLE piovra_connector_ebay LOGIN PASSWORD 'piovra_connector_ebay';
  END IF;
END $$;

GRANT ALL ON SCHEMA catalog, inventory, orders, publication, channel_config TO piovra_core;
GRANT ALL ON SCHEMA feed TO piovra_feed;
GRANT ALL ON SCHEMA connector_woocommerce TO piovra_connector_woocommerce;
GRANT ALL ON SCHEMA connector_ebay TO piovra_connector_ebay;
