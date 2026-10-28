-- ----------------------------------------------------------------------------
-- Marketplace listings: a tenant's opt-in profile for the public,
-- cross-tenant marketplace (marketplace-web) - separate from Tenant itself
-- since it has its own lifecycle (draft -> pending -> live) and a bunch of
-- marketing-facing fields (area, shop photo, social links) that have no
-- bearing on the tenant's own billing/business identity. One row per tenant.
--
-- marketplace_categories is a small, platform-curated taxonomy (seeded
-- below) - deliberately separate from the tenant-private `categories` table,
-- which organizes one tenant's own products rather than classifying a
-- storefront for cross-tenant browsing/filtering.
-- ----------------------------------------------------------------------------

CREATE TABLE marketplace_categories (
    id              UUID PRIMARY KEY,
    name            TEXT NOT NULL,
    icon            TEXT NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_marketplace_categories_name UNIQUE (name)
);

CREATE TRIGGER trg_marketplace_categories_updated_at
    BEFORE UPDATE ON marketplace_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO marketplace_categories (id, name, icon, display_order) VALUES
    (gen_random_uuid(), 'Ladies Wear', '♧', 1),
    (gen_random_uuid(), 'Men''s Wear', '♧', 2),
    (gen_random_uuid(), 'Kids Wear', '☆', 3),
    (gen_random_uuid(), 'Footwear', '♧', 4),
    (gen_random_uuid(), 'Jewellery', '◇', 5),
    (gen_random_uuid(), 'Beauty & Cosmetics', '✿', 6),
    (gen_random_uuid(), 'Electronics', '▣', 7),
    (gen_random_uuid(), 'Mobile Shops', '▯', 8),
    (gen_random_uuid(), 'Grocery', '♧', 9),
    (gen_random_uuid(), 'Home & Kitchen', '⌂', 10),
    (gen_random_uuid(), 'Furniture', '⊞', 11),
    (gen_random_uuid(), 'Restaurants', '♨', 12),
    (gen_random_uuid(), 'Services', '⚙', 13),
    (gen_random_uuid(), 'Computer/Laptop shop', '▣', 14),
    (gen_random_uuid(), 'Other', '⋯', 15);

-- variant_id-style "no FK to a mutable business record" isn't a concern here;
-- category_id IS a real FK because reassigning/removing a category should
-- deliberately be constrained (ON DELETE RESTRICT via the default), not
-- silently orphan a live listing's filter facet.
CREATE TABLE marketplace_listings (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    category_id         UUID REFERENCES marketplace_categories(id),
    area                TEXT,
    shop_photo_file_name TEXT,
    shop_photo_alt      TEXT,
    instagram_url       TEXT,
    facebook_url        TEXT,
    youtube_url         TEXT,
    google_maps_url     TEXT,
    whatsapp_url        TEXT,
    website_url         TEXT,
    enquiry_whatsapp    VARCHAR(20),
    order_mode          VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP' CHECK (order_mode IN ('WHATSAPP', 'CHECKOUT')),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'LIVE')),
    rejection_reason    TEXT,
    submitted_at        TIMESTAMPTZ,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_marketplace_listings_status ON marketplace_listings(status);

CREATE TRIGGER trg_marketplace_listings_updated_at
    BEFORE UPDATE ON marketplace_listings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
