-- Lets a Super Admin's plan change be scheduled for the tenant's next billing cycle instead of applying
-- immediately: pending_subscription_plan holds the new plan until SubscriptionService applies it (and
-- clears this column) when the next cycle's invoice is generated. NULL means no change is scheduled.
--
-- RUN THIS BEFORE deploying the backend version that maps the new column (ddl-auto=validate will refuse
-- to start otherwise). Safe to re-run.

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS pending_subscription_plan VARCHAR(20);

-- To undo:
--   ALTER TABLE tenants DROP COLUMN IF EXISTS pending_subscription_plan;
