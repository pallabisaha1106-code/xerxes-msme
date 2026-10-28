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
 * The core "steel thread", Checkout counterpart to grocery-vendor-free-whatsapp.spec.ts:
 * a Grocery vendor registers on the Free plan with Checkout order mode, is approved by
 * Super Admin, builds a catalogue of 5 categories x 2 starter products each (10 total -
 * the Free plan's cap), prices/stocks two of them, and two separate guests each place a
 * multi-item order (3 units across both products) - one order is approved by the vendor,
 * the other rejected - proving the vendor isn't just "listed" but actually orderable, with
 * a real multi-product cart and both outcomes of the review step. Pricing/stocking/ordering
 * deliberately stays scoped to 2 products even though the catalogue has 10 - see
 * grocery-vendor-category-filter.spec.ts for the spec that exercises more than one category
 * at the shopper-facing filter level.
 *
 * Deliberately leaves its data in place afterward (no teardown) - see
 * grocery-vendor-free-whatsapp.spec.ts for why the timestamp suffix makes that safe.
 */
test("grocery vendor registers free with checkout; a multi-item order gets approved, another gets rejected", async ({
  page,
}) => {
  // No PublicApiRateLimiter pacing waits needed - app.public-api.rate-limit-enabled
  // is off for local dev (application-local.yml), where this spec is meant to run.
  // Generous headroom for the flow itself - two products, two full checkouts and
  // two vendor review actions, and slowMo adds ~200ms to every single action in
  // all of it. The previous 10-minute budget wasn't enough even before accounting
  // for the (now-removed) two 60s waits, so this leaves real margin.
  test.setTimeout(900_000);

  const suffix = timestampSuffix();
  const email = `grocery-checkout-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Grocery Checkout Steel Thread ${suffix}`;
  const PRODUCT_1_NAME = "Amul Milk";
  const PRODUCT_2_NAME = "Amul Paneer";
  // Matches StarterCatalogService.generateUniqueSku's derivation from the product name.
  const PRODUCT_1_SKU = "AMUL-MILK";
  const PRODUCT_2_SKU = "AMUL-PANEER";

  // ---- Step 1: apply ----
  await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
  });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  // ---- Step 2: Free plan, 5 categories x 2 starter products each (10 total - the Free
  // plan's cap), Checkout order mode. Only PRODUCT_1/PRODUCT_2 (Milk & Dairy) go on to be
  // priced, stocked and ordered below - the other 4 categories only prove the catalogue
  // picker and plan cap, same as grocery-vendor-free-whatsapp.spec.ts. ----
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  const CATALOGUE: { category: string; products: string[] }[] = [
    { category: "Milk & Dairy Products", products: [PRODUCT_1_NAME, PRODUCT_2_NAME] },
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

  // Checkout instead of WhatsApp - the enquiry-number field only shows for WHATSAPP mode.
  await page.getByLabel("How will customers order?").selectOption("CHECKOUT");
  await expect(page.getByLabel("WhatsApp number for enquiries")).toHaveCount(0);

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

  // ---- Vendor logs in and prices + stocks both products, so they're actually orderable ----
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  async function priceAndStock(productName: string, sku: string) {
    await page.goto(`${FRONTEND_WEB_URL}/products`);
    await page.getByRole("button", { name: productName }).click();
    await page.getByRole("button", { name: `Edit ${sku}` }).click();
    await page.getByLabel("Purchase price").fill("40");
    await page.getByLabel("Selling price").fill("60");
    await page.getByRole("button", { name: "Save Changes" }).click();
    await expect(page.getByText("Variant updated")).toBeVisible();

    await page.goto(`${FRONTEND_WEB_URL}/stock`);
    await page.getByLabel("Product variant").selectOption({ label: `${productName} - ${sku}` });
    await page.getByRole("button", { name: "Adjust Stock" }).click();
    await page.getByLabel("Quantity change").fill("25");
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Stock adjustment recorded")).toBeVisible();
  }

  await priceAndStock(PRODUCT_1_NAME, PRODUCT_1_SKU);
  await priceAndStock(PRODUCT_2_NAME, PRODUCT_2_SKU);

  async function placeMultiItemOrder(customerName: string, phone: string) {
    await page.goto("http://localhost:5174/shops");
    // The "Welcome to Neighbourly" dialog only shows on a first visit per browser
    // (DiscoveryWelcomeDialog checks localStorage) - our first customer's visit
    // already dismissed and saved that preference, so it won't show again for a
    // second customer in this same browser session. Dismiss it only if present.
    const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
    if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await browseAllBusinesses.click();
    }
    await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
    await page.getByRole("button", { name: "Find shops" }).click();
    await page.getByRole("link", { name: new RegExp(`View all products from ${businessName}`) }).click();

    await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
    // 2x Amul Milk ("Add to cart", then the card's + stepper), 1x Amul Paneer - 3 items across 2 products.
    await page.getByRole("button", { name: `Add ${PRODUCT_1_NAME} to cart` }).click();
    await page.getByRole("button", { name: `Add one more ${PRODUCT_1_NAME}` }).click();
    await page.getByRole("button", { name: `Add ${PRODUCT_2_NAME} to cart` }).click();
    // The header cart icon's label carries the item count; the cart itself lives in a drawer.
    await page.getByRole("button", { name: "Cart, 3 items" }).click();
    const cartDrawer = page.getByRole("dialog", { name: "Your cart" });
    await expect(cartDrawer.getByText("3 items across 2 products")).toBeVisible();

    await cartDrawer.getByRole("button", { name: "Checkout" }).click();
    const checkoutDialog = page.getByRole("dialog", { name: "Checkout" });
    await checkoutDialog.getByLabel("Name").fill(customerName);
    await checkoutDialog.getByLabel("Phone").fill(phone);
    await checkoutDialog.getByLabel("Address").fill("12 MG Road");
    await checkoutDialog.getByLabel("City").fill("Pune");
    await checkoutDialog.getByLabel("Pincode").fill("411038");
    await checkoutDialog.getByRole("button", { name: "Place order" }).click();
    await expect(page.getByText(/Order placed!/)).toBeVisible();
  }

  // ---- Order 1: a customer places a multi-item order, and the vendor approves it ----
  await placeMultiItemOrder("Rahul Sharma", "9123456780");

  await page.goto(`${FRONTEND_WEB_URL}/storefront/orders`);
  const approvedRow = page.getByRole("row", { name: /Rahul Sharma/ });
  await expect(approvedRow).toBeVisible();
  await expect(approvedRow.getByText("NEW")).toBeVisible();

  await approvedRow.getByRole("button", { name: "Rahul Sharma" }).click();
  await expect(page.getByRole("heading", { name: "Order — Rahul Sharma" })).toBeVisible();
  await expect(page.getByRole("row", { name: new RegExp(`${PRODUCT_1_NAME} - ${PRODUCT_1_SKU}`) })).toContainText("2");
  await expect(page.getByRole("row", { name: new RegExp(`${PRODUCT_2_NAME} - ${PRODUCT_2_SKU}`) })).toContainText("1");
  await page.keyboard.press("Escape");

  await approvedRow.getByRole("button", { name: "Confirm" }).click();
  await expect(page.getByText("Order confirmed")).toBeVisible();
  await expect(approvedRow.getByText("CONFIRMED")).toBeVisible();

  // ---- Order 2: a second customer places the same multi-item order, and the vendor rejects it ----
  await placeMultiItemOrder("Sunita Verma", "9988776655");

  await page.goto(`${FRONTEND_WEB_URL}/storefront/orders`);
  const rejectedRow = page.getByRole("row", { name: /Sunita Verma/ });
  await expect(rejectedRow).toBeVisible();
  await expect(rejectedRow.getByText("NEW")).toBeVisible();

  await rejectedRow.getByRole("button", { name: "Reject" }).click();
  await page.getByLabel("Reason (optional)").fill("Out of delivery range");
  await page.getByRole("dialog", { name: "Reject Order" }).getByRole("button", { name: "Reject" }).click();
  await expect(page.getByText("Order rejected")).toBeVisible();
  await expect(rejectedRow.getByText("REJECTED")).toBeVisible();

  await rejectedRow.getByRole("button", { name: "Sunita Verma" }).click();
  await expect(page.getByRole("heading", { name: "Order — Sunita Verma" })).toBeVisible();
  await expect(page.getByText("Rejection reason:")).toBeVisible();
  await expect(page.getByText("Out of delivery range")).toBeVisible();
  await expect(page.getByRole("row", { name: new RegExp(`${PRODUCT_1_NAME} - ${PRODUCT_1_SKU}`) })).toContainText("2");
  await expect(page.getByRole("row", { name: new RegExp(`${PRODUCT_2_NAME} - ${PRODUCT_2_SKU}`) })).toContainText("1");
});
