-- Adds an image_profile to each marketplace category, so photo handling can differ by kind of shop:
--   COMPACT - small pack shots (grocery, general goods); one photo is enough.
--   DETAIL  - products customers want to inspect closely (apparel, footwear, jewellery); needs sharper,
--             larger photos, several per product and zoom.
-- This script only adds the setting and sets the sensible starting values. Nothing reads it yet for
-- limits or zoom - that comes in later changes - so running it changes no behaviour by itself.
--
-- RUN THIS BEFORE deploying the backend version that maps the new column: the app runs with
-- ddl-auto=validate, so it will refuse to start if the column is missing.
--
-- Safe to re-run: the column is only added if missing, and the UPDATE is a plain set.

ALTER TABLE marketplace_categories
    ADD COLUMN IF NOT EXISTS image_profile VARCHAR(20) NOT NULL DEFAULT 'COMPACT';

-- Named constraint added separately so a re-run doesn't fail on it.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_marketplace_categories_image_profile'
    ) THEN
        ALTER TABLE marketplace_categories
            ADD CONSTRAINT chk_marketplace_categories_image_profile
            CHECK (image_profile IN ('COMPACT', 'DETAIL'));
    END IF;
END $$;

UPDATE marketplace_categories
SET image_profile = 'DETAIL'
WHERE name IN ('Ladies Wear', 'Men''s Wear', 'Kids Wear', 'Footwear', 'Jewellery');

-- To undo:
--   ALTER TABLE marketplace_categories DROP CONSTRAINT IF EXISTS chk_marketplace_categories_image_profile;
--   ALTER TABLE marketplace_categories DROP COLUMN IF EXISTS image_profile;
