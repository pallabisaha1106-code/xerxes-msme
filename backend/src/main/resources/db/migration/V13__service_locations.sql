-- Super Admin-curated hierarchy of where the marketplace is actually
-- supported: State (reuses the existing fixed GST state codes - not a table,
-- see GST_STATE_CODES in both frontend apps) -> City -> Area. Area carries
-- its own pincode (one per area, e.g. Nigdi = 411044), so there's no
-- separate pincode table. Vendor registration is gated on picking a real,
-- active City/Area from here instead of free-typing one - see
-- VendorApplicationService.apply().

CREATE TABLE service_cities (
    id              UUID PRIMARY KEY,
    state_code      CHARACTER(2) NOT NULL,
    name            TEXT NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_cities_state_name UNIQUE (state_code, name)
);
CREATE INDEX idx_service_cities_state_code ON service_cities(state_code);
CREATE TRIGGER trg_service_cities_updated_at
    BEFORE UPDATE ON service_cities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE service_areas (
    id              UUID PRIMARY KEY,
    city_id         UUID NOT NULL REFERENCES service_cities(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    pincode         VARCHAR(10) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_areas_city_name UNIQUE (city_id, name)
);
CREATE INDEX idx_service_areas_city_id ON service_areas(city_id);
CREATE TRIGGER trg_service_areas_updated_at
    BEFORE UPDATE ON service_areas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Tenant gets structured city/area references + free-text landmark. The
-- pre-existing free-text tenants.city/pincode columns are left exactly as
-- they are (still used by the separate instant-signup /api/auth/register
-- flow - see AuthService - which this change doesn't touch); the new
-- vendor-application flow additionally derives those legacy columns from
-- the chosen city/area so existing consumers (invoices, storefront address,
-- Admin's vendor view) keep working unchanged.
ALTER TABLE tenants ADD COLUMN city_id UUID REFERENCES service_cities(id);
ALTER TABLE tenants ADD COLUMN area_id UUID REFERENCES service_areas(id);
ALTER TABLE tenants ADD COLUMN landmark TEXT;

-- Seed Pune (Maharashtra) with a starter set of real PCMC/Pune localities -
-- including "Kothrud", which the Playwright regression suite already
-- exercises - so the app is usable immediately. Super Admin adds more via
-- Admin -> Service Locations.
INSERT INTO service_cities (id, state_code, name, display_order) VALUES
    (gen_random_uuid(), '27', 'Pune', 1);

INSERT INTO service_areas (id, city_id, name, pincode, display_order)
SELECT gen_random_uuid(), sc.id, area.name, area.pincode, area.display_order
FROM service_cities sc
JOIN (VALUES
    ('Nigdi', '411044', 1),
    ('Akurdi', '411035', 2),
    ('Pimpri', '411017', 3),
    ('Chinchwad East', '411019', 4),
    ('Wakad', '411057', 5),
    ('Hinjawadi', '411057', 6),
    ('Koregaon Park', '411001', 7),
    ('Kothrud', '411038', 8),
    ('Baner', '411045', 9),
    ('Viman Nagar', '411014', 10)
) AS area(name, pincode, display_order) ON true
WHERE sc.name = 'Pune' AND sc.state_code = '27';
