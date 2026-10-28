-- Makes the GST state list backend-managed, like service_cities/service_areas
-- already are, instead of the fixed GST_STATE_CODES constant duplicated in
-- both frontend apps. Keyed by the state's own 2-digit GST code (not a
-- synthetic UUID) since that code is already the natural, stable identifier
-- and nothing else needs to foreign-key onto it - service_cities.state_code
-- stays a plain string column, just now validated against this table instead
-- of trusted blindly. VARCHAR (not CHARACTER) - see V14's fix for why
-- CHARACTER(n)/bpchar mismatches Hibernate's plain String mapping.
CREATE TABLE service_states (
    code            VARCHAR(2) PRIMARY KEY,
    name            TEXT NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_service_states_updated_at
    BEFORE UPDATE ON service_states
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Seeds the full official CGST state/UT list (same data GST_STATE_CODES held
-- client-side), all active by default so existing registration flows keep
-- working unchanged until a Super Admin deliberately disables one.
INSERT INTO service_states (code, name, display_order) VALUES
    ('01', 'Jammu and Kashmir', 1),
    ('02', 'Himachal Pradesh', 2),
    ('03', 'Punjab', 3),
    ('04', 'Chandigarh', 4),
    ('05', 'Uttarakhand', 5),
    ('06', 'Haryana', 6),
    ('07', 'Delhi', 7),
    ('08', 'Rajasthan', 8),
    ('09', 'Uttar Pradesh', 9),
    ('10', 'Bihar', 10),
    ('11', 'Sikkim', 11),
    ('12', 'Arunachal Pradesh', 12),
    ('13', 'Nagaland', 13),
    ('14', 'Manipur', 14),
    ('15', 'Mizoram', 15),
    ('16', 'Tripura', 16),
    ('17', 'Meghalaya', 17),
    ('18', 'Assam', 18),
    ('19', 'West Bengal', 19),
    ('20', 'Jharkhand', 20),
    ('21', 'Odisha', 21),
    ('22', 'Chhattisgarh', 22),
    ('23', 'Madhya Pradesh', 23),
    ('24', 'Gujarat', 24),
    ('26', 'Dadra and Nagar Haveli and Daman and Diu', 25),
    ('27', 'Maharashtra', 26),
    ('29', 'Karnataka', 27),
    ('30', 'Goa', 28),
    ('31', 'Lakshadweep', 29),
    ('32', 'Kerala', 30),
    ('33', 'Tamil Nadu', 31),
    ('34', 'Puducherry', 32),
    ('35', 'Andaman and Nicobar Islands', 33),
    ('36', 'Telangana', 34),
    ('37', 'Andhra Pradesh', 35),
    ('38', 'Ladakh', 36);
