-- ----------------------------------------------------------------------------
-- Appointment booking, step 1: mark which marketplace categories are "bookable".
-- A shop in a bookable category (a salon, a doctor's clinic) takes appointment
-- requests instead of orders; Super Admin decides which categories are bookable.
--
-- This script only adds the setting and two new bookable categories. Nothing
-- reads the flag yet, so running it changes no behaviour of existing shops.
--
-- RUN THIS BEFORE deploying the backend version that maps the new column: the app
-- runs with ddl-auto=validate, so it will refuse to start if the column is missing.
--
-- Safe to re-run: the column is only added if missing, and each category is
-- inserted or, if it already exists, just marked bookable.
-- ----------------------------------------------------------------------------

ALTER TABLE marketplace_categories
    ADD COLUMN IF NOT EXISTS bookable BOOLEAN NOT NULL DEFAULT FALSE;

-- "Other" stays the last pill in the list, so it moves behind the two new categories.
UPDATE marketplace_categories SET display_order = 17 WHERE name = 'Other';

INSERT INTO marketplace_categories (id, name, icon, display_order, bookable) VALUES
    (gen_random_uuid(), 'Salon & Spa', '✂', 15, TRUE),
    (gen_random_uuid(), 'Doctors & Clinics', '✚', 16, TRUE)
ON CONFLICT (name) DO UPDATE SET bookable = TRUE;

-- To undo:
--   DELETE FROM marketplace_categories WHERE name IN ('Salon & Spa', 'Doctors & Clinics')
--       AND id NOT IN (SELECT category_id FROM marketplace_listings WHERE category_id IS NOT NULL);
--   UPDATE marketplace_categories SET display_order = 15 WHERE name = 'Other';
--   ALTER TABLE marketplace_categories DROP COLUMN IF EXISTS bookable;
