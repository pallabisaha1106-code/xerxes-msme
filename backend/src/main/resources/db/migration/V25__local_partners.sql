-- ----------------------------------------------------------------------------
-- Local partners, step 1: independent sales partners who look after the shops in
-- the areas they cover (see the project notes). A partner is a user with the
-- new PARTNER role (belonging to the sentinel platform tenant, like Super Admin)
-- plus one row in `partners`.
--
--   partners        one row per partner (applicant, active, inactive or rejected).
--   partner_areas   the areas each partner covers (many to many).
--   review_actions  an append-only log of every decision on a shop: who did what,
--                   to which shop, when, and why. actor_user_id is empty when the
--                   system acted by itself (automatic partner assignment).
--   tenants         gain partner_id (who looks after the shop) and
--                   partner_assigned_at. Empty until a partner is assigned.
--
-- Nothing reads these yet except the code shipped with this step, so running it
-- changes no behaviour of existing shops.
--
-- RUN THIS BEFORE deploying the backend version that maps these tables and
-- columns: the app runs with ddl-auto=validate, so it will refuse to start if
-- they are missing.
--
-- The foreign keys to tenants cascade or clear, so deleting a whole tenant (as the
-- regression suite's cleanup does) keeps working.
-- ----------------------------------------------------------------------------

CREATE TABLE partners (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    name              VARCHAR(120) NOT NULL,
    whatsapp          VARCHAR(15) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'APPLIED'
                      CHECK (status IN ('APPLIED', 'ACTIVE', 'INACTIVE', 'REJECTED')),
    last_assigned_at  TIMESTAMPTZ,
    decided_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    decided_at        TIMESTAMPTZ,
    decision_reason   VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_partners_status ON partners(status);

CREATE TRIGGER trg_partners_updated_at
    BEFORE UPDATE ON partners
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE partner_areas (
    id          UUID PRIMARY KEY,
    partner_id  UUID NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
    area_id     UUID NOT NULL REFERENCES service_areas(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_partner_areas_partner_area UNIQUE (partner_id, area_id)
);

-- "Which partners cover this area?" is asked for every new shop application.
CREATE INDEX idx_partner_areas_area_id ON partner_areas(area_id);

ALTER TABLE tenants
    ADD COLUMN partner_id           UUID REFERENCES partners(id) ON DELETE SET NULL,
    ADD COLUMN partner_assigned_at  TIMESTAMPTZ;

CREATE INDEX idx_tenants_partner_id ON tenants(partner_id);

CREATE TABLE review_actions (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    partner_id     UUID REFERENCES partners(id) ON DELETE SET NULL,
    action         VARCHAR(40) NOT NULL
                   CHECK (action IN (
                       'ACCOUNT_ACTIVATED', 'ACCOUNT_SUSPENDED',
                       'LISTING_APPROVED', 'LISTING_REJECTED', 'LISTING_UNPUBLISHED',
                       'FEED_POST_APPROVED', 'FEED_POST_REJECTED',
                       'PARTNER_ASSIGNED', 'PARTNER_REASSIGNED')),
    subject_id     UUID,
    reason         VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A shop's history, newest first; and what one partner has done.
CREATE INDEX idx_review_actions_tenant_created ON review_actions(tenant_id, created_at);
CREATE INDEX idx_review_actions_partner_created ON review_actions(partner_id, created_at);

ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('OWNER', 'STAFF', 'SUPER_ADMIN', 'PARTNER'));

-- To undo (delete any PARTNER users first, or the last statement fails):
--   ALTER TABLE users DROP CONSTRAINT users_role_check;
--   ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('OWNER', 'STAFF', 'SUPER_ADMIN'));
--   DROP TABLE IF EXISTS review_actions;
--   DROP INDEX IF EXISTS idx_tenants_partner_id;
--   ALTER TABLE tenants DROP COLUMN IF EXISTS partner_assigned_at, DROP COLUMN IF EXISTS partner_id;
--   DROP TABLE IF EXISTS partner_areas;
--   DROP TABLE IF EXISTS partners;
