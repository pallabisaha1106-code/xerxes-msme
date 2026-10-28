-- Remap existing free-text subscription_plan values onto the new 3-tier
-- MembershipType enum (FREE/PLUS/PREMIUM) BEFORE the app starts reading this
-- column as an enum - Hibernate's @Enumerated(STRING) throws on any stored
-- value that isn't a real enum constant, so any tenant still holding the old
-- STARTER/PRO/ENTERPRISE labels would break on next read otherwise.
-- Tier mapping here is a judgement call (STARTER/PRO -> PLUS, ENTERPRISE ->
-- PREMIUM) - adjust before running against production if these don't match
-- what any existing tenants were actually sold.
UPDATE tenants SET subscription_plan = 'PLUS' WHERE subscription_plan IN ('STARTER', 'PRO');
UPDATE tenants SET subscription_plan = 'PREMIUM' WHERE subscription_plan = 'ENTERPRISE';
UPDATE tenants SET subscription_plan = 'FREE' WHERE subscription_plan NOT IN ('FREE', 'PLUS', 'PREMIUM');

-- Two-step vendor registration: Step 1 creates the tenant inactive and hands
-- back registration_code (an API-key-style credential, not a JWT - see
-- VendorApplicationService) for Step 2. Cleared back to NULL once a Super
-- Admin approves the application, so it stops granting access once the
-- vendor can log in normally.
ALTER TABLE tenants
    ADD COLUMN registration_code VARCHAR(12) UNIQUE,
    ADD COLUMN business_type VARCHAR(20),
    ADD COLUMN shop_front_photo_url TEXT,
    ADD COLUMN gst_certificate_url TEXT,
    ADD COLUMN owner_id_document_url TEXT;
