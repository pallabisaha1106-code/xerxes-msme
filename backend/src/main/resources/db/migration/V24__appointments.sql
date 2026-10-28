-- ----------------------------------------------------------------------------
-- Appointment booking, step 3: the requests themselves. A shopper picks a service
-- (a product variant), a published date and a free time and sends a request; the
-- business approves or rejects it. Nothing is locked: several people can request
-- the same time, only APPROVED rows block a slot (see AppointmentRequestsService).
--
--   variant_id     the service requested. Kept as a plain id (not removed with the
--                  variant), because the request stays a record of what was asked.
--   service_name   what the shopper saw ("Haircut (Men)") and price - copied at
--   price          request time so later edits to the product change nothing here.
--   starts_at /    the appointment, as real instants. ends_at = starts_at plus the
--   ends_at        business's appointment length at that moment.
--   status         PENDING until the business decides. A PENDING row whose
--                  starts_at has passed is shown as EXPIRED when read (no job
--                  rewrites it); EXPIRED is in the list so a later job could.
--
-- Nothing reads this table until the backend that maps it is deployed.
--
-- RUN THIS BEFORE deploying the backend version that maps this table: the app runs
-- with ddl-auto=validate, so it will refuse to start if it is missing.
-- ----------------------------------------------------------------------------

CREATE TABLE appointments (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    variant_id      UUID,
    service_name    VARCHAR(200) NOT NULL,
    price           NUMERIC(12, 2),
    customer_name   VARCHAR(120) NOT NULL,
    customer_phone  VARCHAR(20) NOT NULL,
    note            VARCHAR(200),
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
    decided_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_appointments_order CHECK (starts_at < ends_at)
);

-- The vendor's inbox and the availability check both read one business's rows by time.
CREATE INDEX idx_appointments_tenant_starts ON appointments(tenant_id, starts_at);
-- "How many pending requests does this phone already have with this business?"
CREATE INDEX idx_appointments_tenant_phone_status ON appointments(tenant_id, customer_phone, status);

CREATE TRIGGER trg_appointments_updated_at
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- To undo:
--   DROP TABLE IF EXISTS appointments;
