-- ----------------------------------------------------------------------------
-- Storefront orders: a guest visitor's order request from the (future)
-- generated storefront site. Deliberately NOT an invoice - no stock moves and
-- no GST is charged until the shop owner reviews it and confirms (which
-- creates a real Customer if needed and a real Invoice via the existing
-- invoice flow) or rejects it. No payment is collected at any point here.
-- ----------------------------------------------------------------------------

CREATE TABLE storefront_orders (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'CONFIRMED', 'REJECTED')),
    customer_name           TEXT NOT NULL,
    customer_phone          VARCHAR(15) NOT NULL,
    customer_email          TEXT,
    address_line1           TEXT,
    address_line2           TEXT,
    city                    TEXT,
    pincode                 VARCHAR(10),
    state_code              CHAR(2),          -- optional; defaults to the tenant's own state code at confirm time
    notes                   TEXT,
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    rejection_reason        TEXT,
    confirmed_invoice_id    UUID REFERENCES invoices(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_storefront_orders_tenant_id ON storefront_orders(tenant_id);
CREATE INDEX idx_storefront_orders_tenant_status ON storefront_orders(tenant_id, status);

CREATE TRIGGER trg_storefront_orders_updated_at
    BEFORE UPDATE ON storefront_orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- variant_id deliberately has no FK: these are pre-invoice, non-binding
-- requests (no stock or money has moved), so a variant hard-delete shouldn't
-- be blocked by a pending/old order referencing it. Confirming an order for
-- a since-deleted variant fails cleanly (ResourceNotFoundException) instead.
CREATE TABLE storefront_order_line_items (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES storefront_orders(id) ON DELETE CASCADE,
    variant_id      UUID NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_storefront_order_line_items_order_id ON storefront_order_line_items(order_id);
