-- ----------------------------------------------------------------------------
-- Two optional fields a vendor can attach to a feed post, next to its (<=140
-- char) message, which stays the headline:
--   discount_label  short badge text shown on the offer card ("20% off",
--                   "Buy 1 Get 1") - capped at 30 chars so it fits a badge.
--   valid_until     the last day (inclusive) the offer stays on the public
--                   feed. NULL keeps the original rule: visible for 14 days
--                   from created_at. When set it replaces that window, so a
--                   vendor can run an offer shorter or longer than 14 days.
-- Both nullable, so every existing post keeps behaving exactly as before.
-- ----------------------------------------------------------------------------

ALTER TABLE feed_posts
    ADD COLUMN discount_label VARCHAR(30),
    ADD COLUMN valid_until    DATE;
