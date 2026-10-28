import { test, expect } from "@playwright/test";
import { TEST_USER } from "../config";
import { loginAsTestUser } from "./helpers/login";
import { deleteCategoryByName, deleteProductByName, deleteVendorByName } from "./db";

/**
 * End-to-end vendor + purchase entry flow: a category and a product with one
 * variant (so there's something to buy), a vendor, then a purchase entry
 * with two line items against that vendor - exercising vendor creation, the
 * "Add Line Item" dynamic form flow, and the purchase totals (subtotal/tax/
 * total) computed from each line's product-derived GST rate.
 */

const CATEGORY = "Footwear";
const PRODUCT = "Running Shoes";
const VENDOR = "Global Footwear Distributors";

// SKIP_TEARDOWN=true skips both the pre-test and post-test cleanup - same
// flag and same caveat as catalog-apparel.spec.ts: only works cleanly on a
// fresh run, since nothing gets cleared beforehand either.
const skipTeardown = process.env.SKIP_TEARDOWN === "true";

async function cleanUp() {
  if (skipTeardown) return;
  await deleteVendorByName(TEST_USER.email, VENDOR);
  await deleteProductByName(TEST_USER.email, PRODUCT);
  await deleteCategoryByName(TEST_USER.email, CATEGORY);
}

test.describe("Vendor + purchase entry", () => {
  test.beforeEach(cleanUp);
  test.afterEach(cleanUp);

  test("registers a vendor and records a purchase entry with two line items", async ({ page }) => {
    await loginAsTestUser(page);

    // --- Category + product (so there's something to purchase) ---
    await page.goto("/categories");
    await page.getByRole("button", { name: "New Category" }).click();
    await page.getByLabel("Name").fill(CATEGORY);
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Category created")).toBeVisible();

    await page.goto("/products/new");
    await page.getByLabel("Product name").fill(PRODUCT);
    await page.getByLabel("Category (optional)").selectOption({ label: CATEGORY });
    await page.getByLabel("Default HSN code (optional)").fill("6404");
    await page.getByLabel("Default GST rate (%)").fill("18");

    await page.getByLabel("SKU").fill("SHOES-RUN-42-WHT");
    await page.getByPlaceholder("Attribute (e.g. Size)").first().fill("Size");
    await page.getByPlaceholder("Value (e.g. M)").first().fill("42");
    await page.getByRole("button", { name: "Add attribute" }).click();
    await page.getByPlaceholder("Attribute (e.g. Size)").nth(1).fill("Color");
    await page.getByPlaceholder("Value (e.g. M)").nth(1).fill("White");
    await page.getByLabel("Purchase price").fill("1200");
    await page.getByLabel("Selling price").fill("2499");
    await page.getByLabel("Reorder threshold").fill("5");

    await page.getByRole("button", { name: "Create Product" }).click();
    await expect(page).toHaveURL("/products");
    await expect(page.getByText("Product created")).toBeVisible();

    // --- Vendor ---
    await page.goto("/vendors");
    await page.getByRole("button", { name: "New Vendor" }).click();
    await page.getByLabel("Name").fill(VENDOR);
    await page.getByLabel("GSTIN (optional)").fill("27AAAPF9988D1Z1");
    await page.getByLabel("GST state code (optional)").fill("27");
    await page.getByLabel("Contact person (optional)").fill("Anita Rao");
    await page.getByLabel("Phone (optional)").fill("9876501234");
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Vendor created")).toBeVisible();
    await expect(page.getByRole("cell", { name: VENDOR })).toBeVisible();

    // --- Purchase entry: two line items against the same vendor/variant ---
    await page.goto("/purchases/new");
    await page.getByLabel("Vendor").selectOption({ label: VENDOR });
    await page.getByLabel("Reference number (optional)").fill("PO-2026-100");

    await page.getByLabel("Product variant").selectOption({ label: `${PRODUCT} - SHOES-RUN-42-WHT` });
    await page.getByLabel("Quantity").fill("20");
    await page.getByLabel("Unit price").fill("1200");

    await page.getByRole("button", { name: "Add Line Item" }).click();
    await page.getByLabel("Product variant").nth(1).selectOption({ label: `${PRODUCT} - SHOES-RUN-42-WHT` });
    await page.getByLabel("Quantity").nth(1).fill("10");
    await page.getByLabel("Unit price").nth(1).fill("1150");

    await page.getByRole("button", { name: "Record Purchase" }).click();

    await expect(page).toHaveURL("/purchases");
    await expect(page.getByText("Purchase entry recorded")).toBeVisible();

    // subtotal: 20*1200 + 10*1150 = 24000 + 11500 = 35500
    // tax (18%): 4320 + 2070 = 6390
    // total: 41890
    const row = page.getByRole("row", { name: new RegExp(VENDOR) });
    await expect(row.getByRole("cell", { name: "₹35500.00" })).toBeVisible();
    await expect(row.getByRole("cell", { name: "₹6390.00" })).toBeVisible();
    await expect(row.getByRole("cell", { name: "₹41890.00" })).toBeVisible();

    // --- Open the line items dialog and confirm both lines resolve to the product/SKU label ---
    await row.getByRole("button", { name: "PO-2026-100" }).click();
    const lineItemsDialog = page.getByRole("dialog");
    await expect(lineItemsDialog).toBeVisible();
    await expect(lineItemsDialog.getByText(`${PRODUCT} - SHOES-RUN-42-WHT`).first()).toBeVisible();
  });
});
