import fs from "node:fs";
import path from "node:path";
import { test, expect, type Page } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount, FIXTURES_DIR } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the
// slowMo/headless rationale.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

interface LiveVendor {
  email: string;
  password: string;
  businessName: string;
}

/**
 * Registers a Ladies Wear vendor (the DETAIL photo profile, the shops this flow is for) on the Free plan with one
 * starter product, then has Super Admin approve the vendor and the listing, and logs the vendor in to frontend-web.
 */
async function registerLiveLadiesWearVendor(page: Page, suffix: string): Promise<LiveVendor> {
  const email = `quickadd-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Quick Add Steel Thread ${suffix}`;

  await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
    businessCategory: "Ladies Wear",
  });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();
  await page.getByRole("button", { name: "Kurtis & Kurta Sets", exact: true }).click();
  await page.getByRole("checkbox", { name: "Biba Anarkali Kurti" }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await expect(page.locator(".selection-card")).toHaveCount(1);

  // Checkout instead of WhatsApp, so the shop page shows Add buttons.
  await page.getByLabel("How will customers order?").selectOption("CHECKOUT");
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

  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  return { email, password, businessName };
}

/**
 * "Add products from photos" for a Ladies Wear vendor, start to finish. The vendor reaches the page from the
 * products page, drops four photos named the way the flow expects, and the review screen reads each one right -
 * name and category from the file name, an off-shape photo cropped, a too-small one skipped, a camera-named one
 * numbered - and the vendor fills in the category, price, sizes and stock. Confirming creates three products (the
 * skipped photo makes none): the new "Kurti" category exists once, and a shopper on the live shop page sees the
 * Blue Cotton kurti as two cards (M and L) that both carry the one photo, plus the Black and camera-named products, all
 * at the price from "Apply to all"; the vendor-side variant tables show the sizes and the stock typed in (zero where
 * none was entered).
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("ladies wear vendor creates products from photos, and a shopper sees them with sizes, photos and stock", async ({ page }) => {
  test.setTimeout(300_000);

  const vendor = await registerLiveLadiesWearVendor(page, timestampSuffix());

  // ---- The products page offers the flow to this shop (it would not to a Grocery shop) ----
  await page.goto(`${FRONTEND_WEB_URL}/products`);
  await page.getByRole("button", { name: "Add from photos" }).click();
  await expect(page.getByRole("heading", { name: "Add products from photos" })).toBeVisible();

  // ---- Drop four photos. Ladies Wear needs at least 720x900 and a shape between 0.55 and 0.9 (width / height). ----
  const kurtiPhoto = path.join(FIXTURES_DIR, "ladies-wear-kurti.jpg"); // 1800x2400, already the right shape
  const kurtiBytes = fs.readFileSync(kurtiPhoto);
  await page.locator('input[type="file"]').setInputFiles([
    { name: "IMG_Kurti_Blue Cotton.jpg", mimeType: "image/jpeg", buffer: kurtiBytes },
    // Far too wide: it should be cropped to the allowed shape, not rejected.
    { name: "IMG_Kurti_Black.png", mimeType: "image/png", buffer: solidPng(2400, 1000, [20, 20, 20]) },
    // Under the minimum size even before any crop: it should be skipped.
    { name: "IMG_Kurti_Tiny.png", mimeType: "image/png", buffer: solidPng(400, 500) },
    // A camera name carries no product name.
    { name: "DSC_0042.jpg", mimeType: "image/jpeg", buffer: kurtiBytes },
  ]);

  const card = (fileName: string) => page.getByRole("article").filter({ hasText: fileName });
  const blue = card("IMG_Kurti_Blue Cotton.jpg");
  const black = card("IMG_Kurti_Black.png");
  const tiny = card("IMG_Kurti_Tiny.png");
  const camera = card("DSC_0042.jpg");
  const confirmBar = page.locator(".qa-confirm");

  await expect(page.getByRole("article")).toHaveCount(4);

  // Name comes from the file name; the category the file name points at doesn't exist yet, so it's offered.
  await expect(blue.getByLabel("Name")).toHaveValue("Kurti Blue Cotton");
  await expect(blue.getByRole("button", { name: 'Create "Kurti" and use it' })).toBeVisible();
  await expect(black.getByLabel("Name")).toHaveValue("Kurti Black");
  await expect(black.getByText("Cropped to fit")).toBeVisible();
  await expect(tiny.getByText("Too small (400 x 500)")).toBeVisible();
  await expect(camera.getByLabel("Name")).toHaveValue("Product 1");

  await expect(confirmBar.getByText("3 products ready to create")).toBeVisible();
  await expect(confirmBar.getByText(/1 photo will be skipped/)).toBeVisible();
  // One starter product already uses one of the Free plan's 10 slots.
  await expect(confirmBar.getByText("9 of 10 product slots left")).toBeVisible();

  // ---- Review: one click makes the category for every card that named it ----
  await blue.getByRole("button", { name: 'Create "Kurti" and use it' }).click();
  await expect(blue.getByLabel("Category")).toHaveValue(/new:Kurti/);
  await expect(black.getByLabel("Category")).toHaveValue(/new:Kurti/);
  await expect(page.getByRole("button", { name: 'Create "Kurti" and use it' })).toHaveCount(0);

  // Same price for every product in one go.
  const applyAll = page.getByRole("region", { name: "Apply to all" });
  await applyAll.getByLabel("Price (₹)").fill("499");
  await applyAll.getByRole("button", { name: "Apply to all" }).click();
  await expect(blue.getByLabel("Price (₹)")).toHaveValue("499");
  await expect(black.getByLabel("Price (₹)")).toHaveValue("499");
  await expect(camera.getByLabel("Price (₹)")).toHaveValue("499");
  await expect(tiny.getByLabel("Price (₹)")).toHaveCount(0);

  // Sizes on one card: each size gets its own stock box, and the single Stock box goes away.
  await blue.getByRole("button", { name: "M", exact: true }).click();
  await blue.getByRole("button", { name: "L", exact: true }).click();
  await blue.getByLabel("Stock for size M").fill("5");
  await blue.getByLabel("Stock for size L").fill("3");
  await expect(blue.getByLabel("Stock", { exact: true })).toHaveCount(0);

  // A card without sizes keeps one Stock box.
  await camera.getByLabel("Stock", { exact: true }).fill("7");

  // Only the camera-named card is still without a category; every card has a price.
  await expect(confirmBar.getByText("3 products ready to create")).toBeVisible();
  await expect(confirmBar.getByText(/1 without a category/)).toBeVisible();
  await expect(confirmBar.getByText(/without a price/)).toHaveCount(0);

  // ---- Confirm: three products are created; the too-small photo makes none ----
  await page.getByRole("button", { name: "Create 3 products" }).click();
  await expect(page.getByRole("heading", { name: "3 products created" })).toBeVisible({ timeout: 90_000 });

  // ---- The vendor's catalogue: the products exist, and the "Kurti" category was created exactly once ----
  await page.getByRole("button", { name: "View products" }).click();

  // Each product's variants carry the size, the price from "Apply to all" and the stock the vendor typed
  // (zero for the Black kurti, where none was entered).
  async function expectVariant(productName: string, sku: string, attributes: string, stock: string, status: string) {
    await page.getByRole("button", { name: productName, exact: true }).click();
    const row = page.getByRole("dialog", { name: `Variants — ${productName}` }).getByRole("row", { name: new RegExp(sku) });
    await expect(row.getByRole("cell").nth(1)).toHaveText(attributes);
    await expect(row.getByRole("cell").nth(2)).toHaveText("₹499.00");
    await expect(row.getByRole("cell").nth(3)).toHaveText(stock);
    await expect(row.getByText(status, { exact: true })).toBeVisible();
    await page.keyboard.press("Escape");
  }
  await expectVariant("Kurti Blue Cotton", "KURTI-BLUE-COTTON-M", "Size: M", "5", "IN STOCK");
  await expectVariant("Kurti Blue Cotton", "KURTI-BLUE-COTTON-L", "Size: L", "3", "IN STOCK");
  await expectVariant("Kurti Black", "KURTI-BLACK", "-", "0", "OUT OF STOCK");
  await expectVariant("Product 1", "PRODUCT-1", "-", "7", "IN STOCK");

  await page.goto(`${FRONTEND_WEB_URL}/categories`);
  await expect(page.getByText("Kurti", { exact: true })).toHaveCount(1);

  // ---- A shopper opens the live shop ----
  await page.goto("http://localhost:5174/shops");
  // The "Welcome to Neighbourly" dialog only shows on a first visit per browser, so dismiss it only if present.
  const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
  if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
    await browseAllBusinesses.click();
  }
  await page.getByLabel("Search shops, products, areas or pincodes").fill(vendor.businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await page.getByRole("link", { name: new RegExp(`View all products from ${vendor.businessName}`) }).click();
  await expect(page.getByRole("heading", { name: vendor.businessName })).toBeVisible();

  const shopCard = (sku: string) => page.getByRole("article").filter({ hasText: `Ref ${sku}` });
  const price = /₹\s?499/;

  // Two sizes of one product are two cards, each with the photo (the second got its own copy) and in stock.
  for (const [size, sku] of [
    ["M", "KURTI-BLUE-COTTON-M"],
    ["L", "KURTI-BLUE-COTTON-L"],
  ]) {
    const sized = shopCard(sku);
    await expect(sized.getByRole("heading", { name: "Kurti Blue Cotton" })).toBeVisible();
    await expect(sized.getByText(`${size} · Ref ${sku}`)).toBeVisible();
    await expect(sized.getByText(price)).toBeVisible();
    await expect(sized.getByRole("button", { name: "Enlarge photo of Kurti Blue Cotton" })).toBeVisible();
    await expect(sized.getByRole("button", { name: "Add Kurti Blue Cotton to cart" })).toBeEnabled();
  }

  // The Black kurti (cropped from a too-wide photo) shows its photo and price. Whether a zero-stock item can be
  // ordered depends on the STOREFRONT_STOCK_CHECK_ENABLED setting, so that is checked on the vendor side above.
  const blackCard = shopCard("KURTI-BLACK");
  await expect(blackCard.getByText(price)).toBeVisible();
  await expect(blackCard.getByRole("button", { name: "Enlarge photo of Kurti Black" })).toBeVisible();

  // The camera-named product, numbered by the flow.
  const numbered = shopCard("PRODUCT-1");
  await expect(numbered.getByRole("heading", { name: "Product 1" })).toBeVisible();
  await expect(numbered.getByRole("button", { name: "Enlarge photo of Product 1" })).toBeVisible();
  await expect(numbered.getByRole("button", { name: "Add Product 1 to cart" })).toBeEnabled();
});
