import { test, expect } from "@playwright/test";
import { TEST_USER } from "../config";
import { loginAsTestUser } from "./helpers/login";
import { deleteCategoryByName, deleteProductByName } from "./db";

/**
 * End-to-end catalog setup for an apparel shop (men's and women's clothing):
 * two categories, then a product with two size variants under one category
 * and a product with one variant under the other - exercising category
 * creation, the "Add attribute" and "Add Variant" dynamic form flows, and
 * the effective GST/HSN rate that variants inherit from their product.
 */

const CATEGORY_MEN = "Men's Apparel";
const CATEGORY_WOMEN = "Women's Apparel";
const PRODUCT_TSHIRT = "Men's Crew Neck T-Shirt";
const PRODUCT_DRESS = "Women's Floral Dress";

// SKIP_TEARDOWN=true skips both the pre-test and post-test cleanup, so the
// created categories/products are left in the database for manual
// inspection. Note this only works cleanly on a fresh run - re-running with
// SKIP_TEARDOWN set while last run's data is still there will fail with
// "already registered"-style duplicate errors, since nothing gets cleared
// first either.
const skipTeardown = process.env.SKIP_TEARDOWN === "true";

async function cleanUp() {
  if (skipTeardown) return;
  await deleteProductByName(TEST_USER.email, PRODUCT_TSHIRT);
  await deleteProductByName(TEST_USER.email, PRODUCT_DRESS);
  await deleteCategoryByName(TEST_USER.email, CATEGORY_MEN);
  await deleteCategoryByName(TEST_USER.email, CATEGORY_WOMEN);
}

test.describe("Catalog - apparel shop setup", () => {
  test.beforeEach(cleanUp);
  test.afterEach(cleanUp);

  test("creates men's and women's categories, then products with variants under each", async ({ page }) => {
    await loginAsTestUser(page);

    // --- Categories ---
    await page.goto("/categories");

    await page.getByRole("button", { name: "New Category" }).click();
    await page.getByLabel("Name").fill(CATEGORY_MEN);
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Category created")).toBeVisible();

    await page.getByRole("button", { name: "New Category" }).click();
    await page.getByLabel("Name").fill(CATEGORY_WOMEN);
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Category created")).toBeVisible();

    // exact: names match by substring otherwise, and "Women's Apparel" contains "Men's Apparel".
    await expect(page.getByRole("cell", { name: CATEGORY_MEN, exact: true })).toBeVisible();
    await expect(page.getByRole("cell", { name: CATEGORY_WOMEN, exact: true })).toBeVisible();

    // --- Product 1: T-shirt under Men's Apparel, two size variants ---
    await page.goto("/products/new");

    await page.getByLabel("Product name").fill(PRODUCT_TSHIRT);
    await page.getByLabel("Brand (optional)").fill("Xerxes Basics");
    await page.getByLabel("Category (optional)").selectOption({ label: CATEGORY_MEN });
    await page.getByLabel("Default HSN code (optional)").fill("6109");
    await page.getByLabel("Default GST rate (%)").fill("12");
    await page.getByLabel("Fabric (optional)").fill("100% Cotton");
    await page.getByLabel("Care instructions (optional)").fill("Machine wash cold, do not bleach");
    await page.getByLabel("Included items (optional)").fill("1 T-Shirt");

    // Variant 1: Size M / Color Black
    await page.getByLabel("SKU").fill("TSHIRT-CREW-M-BLK");
    await page.getByPlaceholder("Attribute (e.g. Size)").first().fill("Size");
    await page.getByPlaceholder("Value (e.g. M)").first().fill("M");
    await page.getByRole("button", { name: "Add attribute" }).click();
    await page.getByPlaceholder("Attribute (e.g. Size)").nth(1).fill("Color");
    await page.getByPlaceholder("Value (e.g. M)").nth(1).fill("Black");
    await page.getByLabel("Purchase price").fill("299");
    await page.getByLabel("Selling price").fill("599");
    await page.getByLabel("Reorder threshold").fill("10");

    // Variant 2: Size L / Color Black. Variant 2 starts with its own single
    // blank attribute row, so its "Add attribute" button (the second one on
    // the page) has to be clicked before a second attribute row exists.
    await page.getByRole("button", { name: "Add Variant" }).click();
    await page.getByLabel("SKU").nth(1).fill("TSHIRT-CREW-L-BLK");
    await page.getByPlaceholder("Attribute (e.g. Size)").nth(2).fill("Size");
    await page.getByPlaceholder("Value (e.g. M)").nth(2).fill("L");
    await page.getByRole("button", { name: "Add attribute" }).nth(1).click();
    await page.getByPlaceholder("Attribute (e.g. Size)").nth(3).fill("Color");
    await page.getByPlaceholder("Value (e.g. M)").nth(3).fill("Black");
    await page.getByLabel("Purchase price").nth(1).fill("299");
    await page.getByLabel("Selling price").nth(1).fill("599");
    await page.getByLabel("Reorder threshold").nth(1).fill("10");

    await page.getByRole("button", { name: "Create Product" }).click();

    await expect(page).toHaveURL("/products");
    await expect(page.getByText("Product created")).toBeVisible();
    await expect(page.getByRole("cell", { name: PRODUCT_TSHIRT })).toBeVisible();

    // --- Product 2: Dress under Women's Apparel, one variant ---
    await page.goto("/products/new");

    await page.getByLabel("Product name").fill(PRODUCT_DRESS);
    await page.getByLabel("Category (optional)").selectOption({ label: CATEGORY_WOMEN });
    await page.getByLabel("Default HSN code (optional)").fill("6204");
    await page.getByLabel("Default GST rate (%)").fill("5");
    await page.getByLabel("Fabric (optional)").fill("Georgette");
    await page.getByLabel("Care instructions (optional)").fill("Dry clean only");
    await page.getByLabel("Included items (optional)").fill("1 Dress");

    await page.getByLabel("SKU").fill("DRESS-FLORAL-S-RED");
    await page.getByPlaceholder("Attribute (e.g. Size)").first().fill("Size");
    await page.getByPlaceholder("Value (e.g. M)").first().fill("S");
    await page.getByRole("button", { name: "Add attribute" }).click();
    await page.getByPlaceholder("Attribute (e.g. Size)").nth(1).fill("Color");
    await page.getByPlaceholder("Value (e.g. M)").nth(1).fill("Red");
    await page.getByLabel("Purchase price").fill("450");
    await page.getByLabel("Selling price").fill("899");
    await page.getByLabel("Reorder threshold").fill("5");

    await page.getByRole("button", { name: "Create Product" }).click();

    await expect(page).toHaveURL("/products");
    await expect(page.getByText("Product created")).toBeVisible();
    await expect(page.getByRole("cell", { name: PRODUCT_DRESS })).toBeVisible();

    // --- Verify both products list with the right variant counts ---
    const tshirtRow = page.getByRole("row", { name: new RegExp(PRODUCT_TSHIRT) });
    await expect(tshirtRow.getByRole("cell", { name: "2", exact: true })).toBeVisible();

    const dressRow = page.getByRole("row", { name: new RegExp(PRODUCT_DRESS) });
    await expect(dressRow.getByRole("cell", { name: "1", exact: true })).toBeVisible();

    // --- Open the T-shirt's variants dialog and confirm both variants + attributes ---
    await tshirtRow.getByRole("button", { name: PRODUCT_TSHIRT }).click();
    const variantsDialog = page.getByRole("dialog", { name: `Variants — ${PRODUCT_TSHIRT}` });
    await expect(variantsDialog).toBeVisible();
    await expect(variantsDialog.getByText("TSHIRT-CREW-M-BLK")).toBeVisible();
    await expect(variantsDialog.getByText("TSHIRT-CREW-L-BLK")).toBeVisible();
    await expect(variantsDialog.getByText("Size: M, Color: Black")).toBeVisible();
    await expect(variantsDialog.getByText("Size: L, Color: Black")).toBeVisible();

    // --- Confirm the product-level fabric/care/included-items fields round-tripped ---
    await variantsDialog.getByRole("button", { name: "Edit Product" }).click();
    const editDialog = page.getByRole("dialog", { name: "Edit Product" });
    await expect(editDialog).toBeVisible();
    await expect(editDialog.getByLabel("Fabric (optional)")).toHaveValue("100% Cotton");
    await expect(editDialog.getByLabel("Care instructions (optional)")).toHaveValue("Machine wash cold, do not bleach");
    await expect(editDialog.getByLabel("Included items (optional)")).toHaveValue("1 T-Shirt");
    await editDialog.getByRole("button", { name: "Cancel" }).click();
  });
});
