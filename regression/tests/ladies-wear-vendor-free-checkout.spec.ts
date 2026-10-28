import path from "node:path";
import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount, FIXTURES_DIR } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-checkout.spec.ts - see that file for the
// slowMo/headless rationale.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

/**
 * Checkout counterpart to ladies-wear-vendor-free-whatsapp.spec.ts. Same
 * steel thread as grocery-vendor-free-checkout.spec.ts (two priced/stocked
 * products, one order approved, one rejected), plus the one thing specific
 * to Ladies Wear: it's the sole marketplace category on the DETAIL image
 * profile, so this also has the vendor upload a real photo through the
 * actual product UI and confirms it renders through the DETAIL zoom
 * experience (PhotoZoomDialog), not the plain Lightbox COMPACT categories
 * use.
 *
 * Deliberately leaves its data in place afterward - see
 * grocery-vendor-free-whatsapp.spec.ts for why the timestamp suffix makes
 * that safe.
 */
test("ladies wear vendor registers free with checkout; uploads a photo, one order gets approved, another gets rejected", async ({
  page,
}) => {
  // No PublicApiRateLimiter pacing waits needed locally. Same generous headroom as
  // grocery-vendor-free-checkout.spec.ts, plus one extra product-page round trip for
  // the photo upload.
  test.setTimeout(900_000);

  const suffix = timestampSuffix();
  const email = `ladieswear-checkout-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Ladies Wear Checkout Steel Thread ${suffix}`;
  const PRODUCT_1_NAME = "Biba Anarkali Kurti";
  const PRODUCT_2_NAME = "Aurelia Cotton Kurti";
  // Matches StarterCatalogService.generateUniqueSku's derivation from the product name.
  const PRODUCT_1_SKU = "BIBA-ANARKALI-KURTI";
  const PRODUCT_2_SKU = "AURELIA-COTTON-KURTI";

  // ---- Step 1: apply ----
  await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
    businessCategory: "Ladies Wear",
  });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  // ---- Step 2: Free plan, two starter products (same category tile), Checkout order mode ----
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  await page.getByRole("button", { name: "Kurtis & Kurta Sets", exact: true }).click();
  await page.getByRole("checkbox", { name: PRODUCT_1_NAME }).check();
  await page.getByRole("checkbox", { name: PRODUCT_2_NAME }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await expect(page.locator(".selection-card")).toHaveCount(2);

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
    await page.getByLabel("Purchase price").fill("400");
    await page.getByLabel("Selling price").fill("650");
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

  // ---- Vendor uploads a real photo for product 1 through the actual product UI ----
  // Ladies Wear's starter catalogue ships with no reference photos at all, so every
  // variant starts with zero images (not even a starter placeholder) until the vendor
  // adds one here - this is the DETAIL-profile path the Grocery specs never exercise.
  await page.goto(`${FRONTEND_WEB_URL}/products`);
  await page.getByRole("button", { name: PRODUCT_1_NAME }).click();
  await page.getByRole("button", { name: `Edit ${PRODUCT_1_SKU}` }).click();
  const editDialog = page.getByRole("dialog", { name: "Edit Variant" });
  await editDialog.locator('input[type="file"]').setInputFiles(path.join(FIXTURES_DIR, "ladies-wear-kurti.jpg"));
  await expect(editDialog.getByText("Looks good.")).toBeVisible();
  await editDialog.getByRole("button", { name: "Upload Photo" }).click();
  await expect(page.getByText("Photo uploaded")).toBeVisible();
  await editDialog.getByRole("button", { name: "Cancel" }).click();

  async function placeMultiItemOrder(
    customerName: string,
    phone: string,
    options?: { verifyZoomForProduct?: string },
  ) {
    await page.goto("http://localhost:5174/shops");
    // The "Welcome to Neighbourly" dialog only shows on a first visit per browser
    // (DiscoveryWelcomeDialog checks localStorage) - dismiss it only if present, since
    // a second customer in this same browser session won't see it again.
    const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
    if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await browseAllBusinesses.click();
    }
    await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
    await page.getByRole("button", { name: "Find shops" }).click();
    await page.getByRole("link", { name: new RegExp(`View all products from ${businessName}`) }).click();
    await expect(page.getByRole("heading", { name: businessName })).toBeVisible();

    if (options?.verifyZoomForProduct) {
      // Ladies Wear is on the DETAIL image profile - confirm the vendor's uploaded photo
      // renders through PhotoZoomDialog (see LiveShopPage.tsx's imageProfile branch) and
      // that clicking it actually zooms in, not just that a photo shows up at all.
      await page.getByRole("button", { name: `Enlarge photo of ${options.verifyZoomForProduct}` }).click();
      const zoomDialog = page.locator("#photo-zoom");
      await expect(zoomDialog).toBeVisible();
      await expect(zoomDialog.getByText("Click the photo to zoom in.")).toBeVisible();
      await zoomDialog.locator("img").click();
      await expect(zoomDialog.locator(".zoom-frame.is-zoomed")).toBeVisible();
      await zoomDialog.getByRole("button", { name: "Close photo" }).click();
    }

    // 2x Biba Anarkali Kurti, 1x Aurelia Cotton Kurti - 3 items across 2 products.
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
  await placeMultiItemOrder("Rahul Sharma", "9123456780", { verifyZoomForProduct: PRODUCT_1_NAME });

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
