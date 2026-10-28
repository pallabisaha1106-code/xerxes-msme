-- Subscription status/period tracking on tenants, plus a history of the
-- monthly invoices (Razorpay Payment Links) generated to collect payment.
--
-- DEFAULT 'ACTIVE' with a generous grandfathered current_period_end (not
-- 'TRIALING') so this column addition doesn't retroactively affect any
-- tenant that already exists (the platform sentinel tenant included) - they
-- keep working exactly as today. New tenants get 'TRIALING' set explicitly
-- in AuthService.register() at insert time, overriding this default.
ALTER TABLE tenants
    ADD COLUMN subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN trial_ends_at TIMESTAMPTZ,
    ADD COLUMN current_period_end TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '1 year'),
    ADD COLUMN grace_period_ends_at TIMESTAMPTZ;

CREATE TABLE subscription_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    razorpay_payment_link_id VARCHAR(100),
    razorpay_payment_link_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ
);

CREATE INDEX idx_subscription_invoices_tenant ON subscription_invoices(tenant_id);
CREATE INDEX idx_subscription_invoices_payment_link ON subscription_invoices(razorpay_payment_link_id);
