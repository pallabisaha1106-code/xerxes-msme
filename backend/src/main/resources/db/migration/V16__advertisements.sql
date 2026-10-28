-- Super Admin-managed advertisements for the marketplace's 3 fixed ad
-- placements (see AdvertisementSlot): the top carousel and the two side
-- rails, all currently hardcoded in marketplace-web. Only one advertisement
-- is "live" per slot at a time - computed at read time as published=true AND
-- now() between starts_at/ends_at, not a stored status, so nothing needs a
-- background job to expire an ad. Multiple rows can exist per slot (past,
-- current, and future-scheduled), and the most recently *published* one
-- wins if more than one happens to be in range for the same slot - see
-- AdminAdvertisementService.publish().
CREATE TABLE advertisements (
    id              UUID PRIMARY KEY,
    slot            VARCHAR(20) NOT NULL,
    image_path      TEXT,
    link_url        TEXT,
    alt_text        TEXT NOT NULL,
    headline        TEXT,
    description     TEXT,
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_advertisements_slot ON advertisements(slot);
CREATE TRIGGER trg_advertisements_updated_at
    BEFORE UPDATE ON advertisements
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
