-- Adds an optional image to each starter-catalog default product. The new
-- bulk-import endpoint (AdminDefaultProductCatalogService.importCatalog)
-- upserts by name so a re-run is idempotent - both default_product_categories
-- (uq_default_product_categories_name) and default_products
-- (uq_default_products_name) already have the unique-by-name constraints
-- that requires, from V7.

ALTER TABLE default_products ADD COLUMN image_path TEXT;
