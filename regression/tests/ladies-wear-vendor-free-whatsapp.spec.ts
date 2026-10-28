import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the
// slowMo/headless rationale. This one proves the steel thread also holds for
// a category other than Grocery.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

/**
 * Same steel thread as grocery-vendor-free-whatsapp.spec.ts, but for Ladies
 * Wear - the one marketplace category on the DETAIL image profile. This spec
 * itself doesn't touch photos (see ladies-wear-vendor-free-checkout.spec.ts
 * for the photo-upload + zoom coverage); it only confirms the base
 * register -> approve -> list -> login journey still holds for this category.
 */
test("ladies wear vendor registers free, is approved by Super Admin, and is viewable on the marketplace (WhatsApp)", async ({
  page,
}) => {
  test.setTimeout(180_000);

  const suffix = timestampSuffix();
  const email = `ladieswear-wa-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Ladies Wear WhatsApp Steel Thread ${suffix}`;

  // ---- Step 1: apply ----
  const registrationCode = await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
    businessCategory: "Ladies Wear",
  });
  expect(registrationCode).toMatch(/^[A-Z0-9]{8}$/);

  await expect(page.getByText(registrationCode)).toBeVisible();
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
  await expect(page).toHaveURL(new RegExp(`/list-your-business/catalogue/${registrationCode}$`));

  // ---- Step 2: Free plan, one starter product, WhatsApp order mode (the page's default) ----
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  await page.getByRole("button", { name: "Kurtis & Kurta Sets", exact: true }).click();
  await page.getByRole("checkbox", { name: "Biba Anarkali Kurti" }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await expect(page.locator(".selection-card")).toHaveCount(1);

  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({
    timeout: 15_000,
  });

  // ---- Still pending: login is rejected until Super Admin approves the tenant ----
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page.getByText(/suspended/i)).toBeVisible();

  // ---- Super Admin reviews and approves the vendor account itself ----
  await loginAsSuperAdmin(page);
  await page.getByRole("button", { name: businessName }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
  await page.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

  // ---- Separately, Super Admin approves the marketplace listing itself ----
  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
  await page.locator("#status-filter").selectOption("PENDING");
  const listingRow = page.getByRole("row", { name: new RegExp(businessName) });
  await expect(listingRow).toBeVisible();
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  // ---- Now publicly visible on the marketplace ----
  await page.goto("http://localhost:5174/shops");
  await page.getByRole("button", { name: "Browse all businesses" }).click();
  await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await expect(page.locator("article.business-card", { hasText: businessName })).toBeVisible();

  // ---- Now that the vendor is approved, login succeeds ----
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);
});
