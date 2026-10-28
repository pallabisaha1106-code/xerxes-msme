-- Marks which stored product photos also have a larger "zoom" copy (see ProductImageService). Only shops
-- whose category uses the DETAIL image profile (V17) get one, and only when the original photo is bigger
-- than the normal display copy. Existing photos are marked FALSE: they were saved before zoom copies
-- existed, so there is nothing larger to show for them.
--
-- RUN THIS BEFORE deploying the backend version that maps the new column (the app runs with
-- ddl-auto=validate and will refuse to start without it). Safe to re-run.

ALTER TABLE product_variant_images
    ADD COLUMN IF NOT EXISTS has_zoom BOOLEAN NOT NULL DEFAULT FALSE;

-- To undo:
--   ALTER TABLE product_variant_images DROP COLUMN IF EXISTS has_zoom;
