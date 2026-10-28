-- Resets the local dev database to a clean slate for manual testing.
--
-- Deletes every tenant except the seeded platform tenant. Every other
-- table's tenant_id column is ON DELETE CASCADE (see
-- backend/src/main/resources/db/migration), so this one statement also
-- removes all users, products, variants, stock, vendors, customers,
-- purchases, invoices, storefront settings/orders, and marketplace
-- listings belonging to those tenants.
--
-- Left untouched (platform reference data, not tenant data):
--   - marketplace_categories, default_product_categories, default_products
--   - the platform tenant + its Super Admin login (admin@xerxes.internal)
--
-- This does NOT remove uploaded files on disk under backend/storage/<tenantId>/
-- for deleted tenants - clean those up separately, or use
-- scripts/reset-test-data.sh, which runs this file and then does that too.

DELETE FROM tenants WHERE is_platform = FALSE;
