    -- ----------------------------------------------------------------------------
    -- Appointment booking, step 2: what a business sets up before it can take
    -- appointment requests (salons, clinics - see V22's `bookable` categories).
    --
    --   appointment_settings   one row per business: how long each appointment is.
    --   appointment_hours      the weekly template: the opening ranges for each
    --                          weekday (several per day allowed, e.g. 10-13 and 17-20).
    --   appointment_open_days  the dates the business has published. A date with no
    --                          row here shows no slots, which is how a holiday or
    --                          leave is handled - no holiday calendar is kept.
    --
    -- Times are local wall-clock times for the business (one time zone, India, in
    -- v1), so hours are plain TIME values. The requests themselves arrive in a
    -- later migration. Nothing reads these tables yet, so running this changes no
    -- behaviour of existing shops.
    --
    -- RUN THIS BEFORE deploying the backend version that maps these tables: the app
    -- runs with ddl-auto=validate, so it will refuse to start if they are missing.
    -- ----------------------------------------------------------------------------

    CREATE TABLE appointment_settings (
        id              UUID PRIMARY KEY,
        tenant_id       UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
        slot_minutes    SMALLINT NOT NULL CHECK (slot_minutes IN (10, 15, 20, 30, 45, 60, 90, 120)),
        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
        updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
    );

    CREATE TRIGGER trg_appointment_settings_updated_at
        BEFORE UPDATE ON appointment_settings
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();

    -- day_of_week is ISO: 1 = Monday ... 7 = Sunday. Rows are replaced as a whole week.
    CREATE TABLE appointment_hours (
        id              UUID PRIMARY KEY,
        tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
        day_of_week     SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
        opens_at        TIME NOT NULL,
        closes_at       TIME NOT NULL,
        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
        CONSTRAINT chk_appointment_hours_order CHECK (opens_at < closes_at)
    );

    CREATE INDEX idx_appointment_hours_tenant_day ON appointment_hours(tenant_id, day_of_week, opens_at);

    CREATE TABLE appointment_open_days (
        id              UUID PRIMARY KEY,
        tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
        day             DATE NOT NULL,
        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
        CONSTRAINT uq_appointment_open_days_tenant_day UNIQUE (tenant_id, day)
    );

    -- To undo:
    --   DROP TABLE IF EXISTS appointment_open_days;
    --   DROP TABLE IF EXISTS appointment_hours;
    --   DROP TABLE IF EXISTS appointment_settings;
