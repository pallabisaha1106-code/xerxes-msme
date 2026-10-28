import { Client } from "pg";
import { rm } from "node:fs/promises";
import { resolve } from "node:path";

/**
 * Direct DB access for test data teardown. There's no delete-tenant API
 * (and adding one solely for test convenience isn't worth the risk), so
 * cleanup goes straight to Postgres. Deleting the tenant row cascades to
 * everything that references it - users, products, invoices, stock ledger,
 * etc. - per the ON DELETE CASCADE foreign keys in V1__init_schema.sql, so
 * one DELETE is a full teardown of whatever a test run created.
 */
const connectionConfig = {
  host: process.env.PGHOST ?? "localhost",
  port: Number(process.env.PGPORT ?? 5432),
  user: process.env.PGUSER ?? "postgres",
  password: process.env.PGPASSWORD ?? "postgres",
  database: process.env.PGDATABASE ?? "xerxes_msme",
};

// Registration also creates an on-disk folder per tenant (product photos,
// later the generated storefront export) - a DB-only teardown would leave
// those orphaned on every run. Assumes the backend's default STORAGE_ROOT
// (./storage relative to backend/); override with STORAGE_ROOT if yours differs.
const storageRoot = resolve(process.env.STORAGE_ROOT ?? "../backend/storage");

export async function deleteTenantByEmail(email: string): Promise<void> {
  const client = new Client(connectionConfig);
  await client.connect();
  try {
    const { rows } = await client.query<{ id: string }>("SELECT id FROM tenants WHERE email = $1", [email]);
    await client.query("DELETE FROM tenants WHERE email = $1", [email]);
    for (const { id } of rows) {
      await rm(resolve(storageRoot, id), { recursive: true, force: true });
    }
  } finally {
    await client.end();
  }
}

/**
 * For tests that run against the standing test account (config.ts) rather
 * than a throwaway tenant - we can't delete the whole tenant, so these clean
 * up just the rows a specific test creates, scoped by tenant email + name.
 */
export async function deleteCategoryByName(tenantEmail: string, categoryName: string): Promise<void> {
  const client = new Client(connectionConfig);
  await client.connect();
  try {
    await client.query(
      "DELETE FROM categories WHERE tenant_id = (SELECT id FROM tenants WHERE email = $1) AND name = $2",
      [tenantEmail, categoryName],
    );
  } finally {
    await client.end();
  }
}

/**
 * Cascades to the product's variants (ON DELETE CASCADE on
 * product_variants.product_id) - but stock_ledger, purchase_line_items, and
 * invoice_line_items all reference variant_id WITHOUT cascade (deliberately:
 * they're audit trails that shouldn't silently lose rows), so anything that
 * recorded a real stock movement against this product's variants (a
 * purchase or a sale) has to be cleared first, or the variant delete fails
 * with a foreign key violation.
 */
export async function deleteProductByName(tenantEmail: string, productName: string): Promise<void> {
  const client = new Client(connectionConfig);
  await client.connect();
  try {
    const variantSubquery = `
      SELECT pv.id FROM product_variants pv
      JOIN products p ON p.id = pv.product_id
      WHERE p.tenant_id = (SELECT id FROM tenants WHERE email = $1) AND p.name = $2
    `;
    await client.query(`DELETE FROM stock_ledger WHERE variant_id IN (${variantSubquery})`, [
      tenantEmail,
      productName,
    ]);
    await client.query(`DELETE FROM purchase_line_items WHERE variant_id IN (${variantSubquery})`, [
      tenantEmail,
      productName,
    ]);
    await client.query(`DELETE FROM invoice_line_items WHERE variant_id IN (${variantSubquery})`, [
      tenantEmail,
      productName,
    ]);
    await client.query(
      "DELETE FROM products WHERE tenant_id = (SELECT id FROM tenants WHERE email = $1) AND name = $2",
      [tenantEmail, productName],
    );
  } finally {
    await client.end();
  }
}

/**
 * purchase_entries.vendor_id has no ON DELETE CASCADE, so any purchase
 * entry recorded against this vendor has to be deleted first (which
 * cascades away its own line items via purchase_entry_id ON DELETE CASCADE).
 */
export async function deleteVendorByName(tenantEmail: string, vendorName: string): Promise<void> {
  const client = new Client(connectionConfig);
  await client.connect();
  try {
    const vendorSubquery =
      "SELECT id FROM vendors WHERE tenant_id = (SELECT id FROM tenants WHERE email = $1) AND name = $2";
    await client.query(`DELETE FROM purchase_entries WHERE vendor_id IN (${vendorSubquery})`, [
      tenantEmail,
      vendorName,
    ]);
    await client.query(
      "DELETE FROM vendors WHERE tenant_id = (SELECT id FROM tenants WHERE email = $1) AND name = $2",
      [tenantEmail, vendorName],
    );
  } finally {
    await client.end();
  }
}
