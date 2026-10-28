import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the
// slowMo/headless rationale.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

/**
 * A shopper on a Grocery vendor's public shop page can filter products by category,
 * and only categories the vendor actually has a product in ever appear as an option
 * (see LiveShopPage.tsx's productCategories derivation). Needs a vendor with products
 * spread across at least two different category tiles - the WhatsApp/Checkout steel
 * threads only ever add products from one tile, so don't exercise this at all.
 *
 * Deliberately leaves its data in place afterward - see
 * grocery-vendor-free-whatsapp.spec.ts for why the timestamp suffix makes that safe.
 */
test("shopper can filter a grocery vendor's shop page by category", async ({ page }) => {
  test.setTimeout(180_000);

  const suffix = timestampSuffix();
  const email = `grocery-category-filter-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Grocery Category Filter Steel Thread ${suffix}`;

  // ---- Step 1: apply ----
  await applyForVendorAccount(page, { businessName, ownerName: "Test Owner", email, password });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  // ---- Step 2: Free plan, one product from each of two different category tiles ----
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
  await page.getByRole("checkbox", { name: "Amul Milk" }).check();
  await page.getByRole("button", { name: "Done" }).click();

  await page.getByRole("button", { name: "Atta, Flour, Rice & Staples", exact: true }).click();
  await page.getByRole("checkbox", { name: "Aashirvaad Atta" }).check();
  await page.getByRole("button", { name: "Done" }).click();

  await expect(page.locator(".selection-card")).toHaveCount(2);

  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({
    timeout: 15_000,
  });

  // ---- Super Admin approves both the vendor account and the listing ----
  await loginAsSuperAdmin(page);
  await page.getByRole("button", { name: businessName }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
  await page.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
  await page.locator("#status-filter").selectOption("PENDING");
  const listingRow = page.getByRole("row", { name: new RegExp(businessName) });
  await expect(listingRow).toBeVisible();
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  // ---- A shopper finds the shop and filters its catalogue by category ----
  await page.goto("http://localhost:5174/shops");
  const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
  if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
    await browseAllBusinesses.click();
  }
  await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await page.getByRole("link", { name: new RegExp(`View all products from ${businessName}`) }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();

  const categoryFilter = page.getByLabel("Filter by category");
  await expect(categoryFilter).toBeVisible();
  // Both products visible before filtering.
  await expect(page.getByText("2 products")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Amul Milk" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Aashirvaad Atta" })).toBeVisible();

  await categoryFilter.selectOption("Milk & Dairy Products");
  await expect(page.getByText("1 products")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Amul Milk" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Aashirvaad Atta" })).toHaveCount(0);

  await categoryFilter.selectOption("All");
  await expect(page.getByText("2 products")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Amul Milk" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Aashirvaad Atta" })).toBeVisible();
});
