-- ----------------------------------------------------------------------------
-- Super admin (platform staff) mode: a platform admin logs in through the
-- exact same /api/auth/login as any tenant user (same table, same JWT
-- shape), but their user row belongs to one sentinel "platform" tenant
-- rather than a real business, and their role authorizes /api/admin/**
-- only - see SecurityConfig. Keeping them in the same users/tenants tables
-- (instead of a separate admin identity system) means no changes were
-- needed anywhere that already reads AuthenticatedPrincipal.tenantId().
--
-- is_platform marks that sentinel row so it can be excluded from the
-- "list all vendors" admin view and from anywhere else tenants are listed.
-- ----------------------------------------------------------------------------

ALTER TABLE tenants ADD COLUMN is_platform BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('OWNER', 'STAFF', 'SUPER_ADMIN'));

INSERT INTO tenants (
    id, business_name, state_code, email, subscription_plan, is_active, is_platform, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001', 'Xerxes Platform', '00', 'platform@xerxes.internal', 'INTERNAL',
    TRUE, TRUE, now(), now()
);

-- Password is "SuperAdmin@123" - change it in every real environment before
-- go-live; this is a seed for local development only.
INSERT INTO users (
    id, tenant_id, name, email, password_hash, role, is_active, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Platform Admin',
    'admin@xerxes.internal', '$2a$10$6kTLGw6g0p3rjiilTqERpOB/1VyYnbKxZcHfCEEXOGVYCIjP6yWFW', 'SUPER_ADMIN', TRUE,
    now(), now()
);
