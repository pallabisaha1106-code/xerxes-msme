-- Tracks every issued JWT (by its own id, used as the token's "jti" claim)
-- so a token can be revoked on logout before its natural expiry - a plain
-- JWT can't be invalidated on its own, since anyone holding it stays
-- "valid" until it expires. See AuthTokenService / JwtAuthenticationFilter.
--
-- created_at doubles as issued_at; there's no updated_at because a row only
-- ever has one field change once (revoked_at) - same append-mostly shape as
-- stock_ledger.
--
-- NOTE: this project no longer runs migrations automatically (Flyway was
-- removed) - apply this file by hand against the database.

CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_user_id ON auth_tokens(user_id);
-- Speeds up the per-request "is this jti still valid" check the auth filter does.
CREATE INDEX idx_auth_tokens_active ON auth_tokens(id) WHERE revoked_at IS NULL;
