-- ----------------------------------------------------------------------------
-- Storefront settings: one row per tenant holding which template they've
-- chosen and their content overrides for that template's configurable text
-- (hero copy, about text, contact details, social links, etc). `content` is
-- a flat key -> value map (e.g. "hero.headline") rather than a normalized
-- table so a template can introduce new configurable fields without a
-- migration - the manifest shipped with the template is the source of truth
-- for which keys exist and what their defaults are; unknown/missing keys
-- here just fall back to the manifest default at render time.
-- ----------------------------------------------------------------------------

CREATE TABLE storefront_settings (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    template_id     VARCHAR(50) NOT NULL DEFAULT 'veera',
    content         JSONB NOT NULL DEFAULT '{}'::jsonb,
    published_at    TIMESTAMPTZ,          -- null until the first successful publish
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_storefront_settings_updated_at
    BEFORE UPDATE ON storefront_settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
