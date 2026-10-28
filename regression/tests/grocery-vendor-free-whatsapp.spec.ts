import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// marketplace-web runs on a different port/app than the frontend-web dashboard
// the rest of the suite targets - see regression/playwright.config.ts.
// headless: false shows the browser window; slowMo puts a visible pause between
// every action (click, fill, navigation) so a human can actually follow each step
// rather than watching it flash by - useful both live and, via the recorded
// video, for training.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

/**
 * The core "steel thread": a Grocery vendor registers on the Free plan,
 * Super Admin reviews and approves them, and the vendor is then viewable on
 * the public marketplace. WhatsApp order mode - the Checkout counterpart is
 * grocery-vendor-free-checkout.spec.ts.
 *
 * Deliberately leaves its data in place afterward (no teardown) - the
 * timestamp suffix on both the email and the business name means a fresh
 * run never collides with a previous run's leftovers, so there's nothing to
 * clean up beforehand either.
 */
test("grocery vendor registers free, is approved by Super Admin, and is viewable on the marketplace (WhatsApp)", async ({
  page,
}) => {
  // No PublicApiRateLimiter pacing wait needed - app.public-api.rate-limit-enabled
  // is off for local dev (application-local.yml), where this spec is meant to run.
  // Generous headroom for the flow itself since slowMo adds ~200ms to every
  // single action (click/fill/select) in it.
  test.setTimeout(180_000);

  const suffix = timestampSuffix();
  const email = `grocery-wa-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Grocery WhatsApp Steel Thread ${suffix}`;

  // ---- Step 1: apply ----
  const registrationCode = await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
  });
  expect(registrationCode).toMatch(/^[A-Z0-9]{8}$/);

  await expect(page.getByText(registrationCode)).toBeVisible();
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
  await expect(page).toHaveURL(new RegExp(`/list-your-business/catalogue/${registrationCode}$`));

  // ---- Step 2: Free plan, 5 categories x 2 starter products each (10 total - the Free
  // plan's cap), WhatsApp order mode (the page's default) ----
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  const CATALOGUE: { category: string; products: string[] }[] = [
    { category: "Milk & Dairy Products", products: ["Amul Milk", "Amul Paneer"] },
    { category: "Atta, Flour, Rice & Staples", products: ["Aashirvaad Atta", "Pillsbury Atta"] },
    { category: "Cooking Oil", products: ["Fortune Sunflower Oil", "Saffola Gold Oil"] },
    { category: "Pulses", products: ["Toor Dal", "Moong Dal"] },
    { category: "Biscuits, Cookies & Rusks", products: ["Parle-G", "Britannia Marie Gold"] },
  ];
  for (const { category, products } of CATALOGUE) {
    await page.getByRole("button", { name: category, exact: true }).click();
    for (const product of products) {
      await page.getByRole("checkbox", { name: product }).check();
    }
    await page.getByRole("button", { name: "Done" }).click();
  }
  await expect(page.locator(".selection-card")).toHaveCount(10);

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
