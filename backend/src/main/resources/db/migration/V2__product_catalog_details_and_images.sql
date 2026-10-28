-- ----------------------------------------------------------------------------
-- Descriptive product-level fields that don't vary by size/colour variant
-- (fabric and care instructions are the same across a garment's sizes), plus
-- storage for photos of the actual stocked item. Every variant can have
-- several images (front/back/detail); the first by display_order is the
-- primary/cover photo.
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN fabric TEXT,
    ADD COLUMN care_instructions TEXT,
    ADD COLUMN included_items TEXT;

CREATE TABLE product_variant_images (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    variant_id      UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,  -- e.g. "3f2a1c9b.jpg"; the -480 thumbnail sits alongside it on disk
    width           INTEGER NOT NULL,
    height          INTEGER NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_variant_images_tenant_id ON product_variant_images(tenant_id);
CREATE INDEX idx_product_variant_images_variant_id ON product_variant_images(variant_id);
