-- A Super Admin-curated "starter catalog" of sub-categories and default
-- products under each marketplace category (e.g. Grocery -> Rice -> Kohinoor
-- Basmati Rice). A vendor self-registering on the marketplace can pick from
-- this list to seed their own tenant's product catalog instead of typing
-- every product by hand - see StarterCatalogService.

CREATE TABLE default_product_categories (
    id                          UUID PRIMARY KEY,
    marketplace_category_id    UUID NOT NULL REFERENCES marketplace_categories(id) ON DELETE CASCADE,
    name                        TEXT NOT NULL,
    display_order               INTEGER NOT NULL DEFAULT 0,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_default_product_categories_name UNIQUE (marketplace_category_id, name)
);
CREATE INDEX idx_default_product_categories_marketplace_category_id ON default_product_categories(marketplace_category_id);
CREATE TRIGGER trg_default_product_categories_updated_at
    BEFORE UPDATE ON default_product_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE default_products (
    id                              UUID PRIMARY KEY,
    default_product_category_id    UUID NOT NULL REFERENCES default_product_categories(id) ON DELETE CASCADE,
    name                            TEXT NOT NULL,
    display_order                   INTEGER NOT NULL DEFAULT 0,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_default_products_name UNIQUE (default_product_category_id, name)
);
CREATE INDEX idx_default_products_category_id ON default_products(default_product_category_id);
CREATE TRIGGER trg_default_products_updated_at
    BEFORE UPDATE ON default_products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- A small illustrative starter set for Grocery, matching the example given
-- when this feature was designed. Super Admin can add more via the admin UI.
INSERT INTO default_product_categories (id, marketplace_category_id, name, display_order)
SELECT gen_random_uuid(), id, 'Rice', 1 FROM marketplace_categories WHERE name = 'Grocery'
UNION ALL
SELECT gen_random_uuid(), id, 'Cooking Oil', 2 FROM marketplace_categories WHERE name = 'Grocery'
UNION ALL
SELECT gen_random_uuid(), id, 'Pulses', 3 FROM marketplace_categories WHERE name = 'Grocery';

INSERT INTO default_products (id, default_product_category_id, name, display_order)
SELECT gen_random_uuid(), dpc.id, product.name, product.display_order
FROM default_product_categories dpc
JOIN marketplace_categories mc ON mc.id = dpc.marketplace_category_id AND mc.name = 'Grocery'
JOIN (VALUES
    ('Rice', 'Kohinoor Basmati Rice', 1),
    ('Rice', 'India Gate Basmati Rice', 2),
    ('Rice', 'Daawat Basmati Rice', 3),
    ('Cooking Oil', 'Fortune Sunflower Oil', 1),
    ('Cooking Oil', 'Saffola Gold Oil', 2),
    ('Pulses', 'Toor Dal', 1),
    ('Pulses', 'Moong Dal', 2),
    ('Pulses', 'Chana Dal', 3)
) AS product(category_name, name, display_order) ON product.category_name = dpc.name;
