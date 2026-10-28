import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the
// slowMo/headless rationale.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

/**
 * First step of appointment booking: some marketplace categories are "bookable" (a salon, a clinic), and Super Admin
 * decides which. The seeded "Salon & Spa" and "Doctors & Clinics" categories start bookable, everything else does not;
 * Super Admin can switch any category and the choice survives a reload. The toggle is exercised on "Other" and put
 * back in a finally block, so no run leaves a category changed.
 */
test("Super Admin sees the bookable categories and can switch a category's booking mode", async ({ page }) => {
  test.setTimeout(120_000);

  await loginAsSuperAdmin(page);
  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace-categories`);

  const bookingMode = (category: string) => page.getByLabel(`Shops in ${category} take`);
  await expect(bookingMode("Salon & Spa")).toHaveValue("true");
  await expect(bookingMode("Doctors & Clinics")).toHaveValue("true");
  await expect(bookingMode("Grocery")).toHaveValue("false");

  try {
    await bookingMode("Other").selectOption("true");
    await expect(page.getByText("Booking mode updated")).toBeVisible();

    await page.reload();
    await expect(bookingMode("Other")).toHaveValue("true");
  } finally {
    await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace-categories`);
    await bookingMode("Other").selectOption("false");
    await expect(bookingMode("Other")).toHaveValue("false");
  }
});

/**
 * A "Salon & Spa" vendor registers on the Free plan with no starter products (a bookable category has no default
 * catalogue to pick from), Super Admin approves the vendor and the listing, and the shop goes live. The public
 * listing the marketplace reads then says the shop is bookable.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a Salon & Spa vendor registers without a starter catalogue and its public listing is bookable", async ({
  page,
}) => {
  test.setTimeout(180_000);

  const suffix = timestampSuffix();
  const businessName = `Glow Salon Steel Thread ${suffix}`;

  await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email: `salon-${suffix}@test.com`,
    password: "password1234",
    businessCategory: "Salon & Spa",
  });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  // No default catalogue exists for this category, so there is nothing to pick.
  await expect(page.getByRole("checkbox")).toHaveCount(0);
  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({
    timeout: 15_000,
  });

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

  // A shopper finds the live shop; the public listing for it says it is bookable.
  await page.goto("http://localhost:5174/shops");
  // The "Welcome to Neighbourly" dialog only shows on a first visit per browser, so dismiss it only if present.
  const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
  if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
    await browseAllBusinesses.click();
  }
  await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await page.getByRole("link", { name: new RegExp(`View all products from ${businessName}`) }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();

  const tenantId = new URL(page.url()).pathname.split("/").pop();
  const listing = await (await page.request.get(`/api/public/marketplace/listings/${tenantId}`)).json();
  expect(listing.businessName).toBe(businessName);
  expect(listing.categoryName).toBe("Salon & Spa");
  expect(listing.bookable).toBe(true);
});
