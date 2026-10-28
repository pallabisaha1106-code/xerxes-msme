-- ----------------------------------------------------------------------------
-- Feed posts: a short (<=140 char), Twitter-style update a vendor can publish
-- about a current offer/sale ("latest news"). Pre-publish reviewed by Super
-- Admin (status starts PENDING, same reviewed-by-staff shape as marketplace
-- listings) - unlike a listing, a rejected post is terminal, not resubmitted,
-- since it's one-shot ephemeral content rather than a long-lived draft. The
-- public feed query filters to APPROVED AND created_at within the last 14
-- days (app-side, see FeedPostService) - old posts are never deleted here,
-- just filtered out, so a vendor can still see their own post history.
-- One tenant can have many posts over time (one row per post), unlike
-- marketplace_listings' one-row-per-tenant shape.
-- ----------------------------------------------------------------------------

CREATE TABLE feed_posts (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    content             VARCHAR(140) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason    TEXT,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Covers both "this tenant's own posts, newest first" (vendor's own list +
-- the 1-post-per-7-days cooldown check) and is the leading column the public
-- feed query also filters on via a join.
CREATE INDEX idx_feed_posts_tenant_created ON feed_posts(tenant_id, created_at DESC);

-- The admin review queue's own filter (status = 'PENDING'), and the public
-- feed query's status/recency filter together.
CREATE INDEX idx_feed_posts_status_created ON feed_posts(status, created_at DESC);

CREATE TRIGGER trg_feed_posts_updated_at
    BEFORE UPDATE ON feed_posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
