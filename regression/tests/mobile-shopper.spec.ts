import { test, expect, type APIRequestContext, type Page } from "@playwright/test";
import { SUPER_ADMIN } from "../config";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the slowMo/headless rationale. This is a
// phone-layout journey: it only runs in the Android phone project (`npm run test:mobile`) and skips itself on the
// desktop browser. Shops are set up through the API so the shopper's screens are the only UI under test.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

interface Shop {
  tenantId: string;
  businessName: string;
  vendor: { Authorization: string };
  productId: string;
  variantId: string;
}

/** A live shop in the given marketplace category with one priced product; `photo` gives its variant a real photo. */
async function createShop(
  request: APIRequestContext,
  suffix: string,
  label: string,
  categoryName: string,
  orderMode: "CHECKOUT" | "WHATSAPP",
  product: { name: string; price: number; attributes: Record<string, string> },
  withPhoto: boolean,
): Promise<Shop> {
  const categories = (await (await request.get("/api/public/marketplace/categories")).json()) as { id: string; name: string }[];
  const category = categories.find((c) => c.name === categoryName);
  expect(category, `the ${categoryName} category exists`).toBeTruthy();

  const businessName = `Phone Journey ${label} ${suffix}`;
  const registered = await request.post("/api/auth/register", {
    data: { businessName, stateCode: "27", ownerName: "Test Owner", email: `phone-${label.toLowerCase()}-${suffix}@test.com`, password: "password1234" },
  });
  expect(registered.status()).toBe(201);
  const { token, tenantId } = await registered.json();
  const vendor = { Authorization: `Bearer ${token}` };

  expect((await request.put("/api/marketplace/listing", { headers: vendor, data: { categoryId: category!.id, area: "Kothrud", enquiryWhatsapp: "9876543210", orderMode } })).status()).toBe(200);
  const shopPhoto = await request.post("/api/marketplace/listing/photo", {
    headers: vendor,
    multipart: { file: { name: "shopfront.png", mimeType: "image/png", buffer: solidPng(1600, 900) } },
  });
  expect(shopPhoto.status(), "shop photo upload").toBe(200);
  expect((await request.post("/api/marketplace/listing/submit", { headers: vendor })).status()).toBe(200);
  const admin = { Authorization: `Bearer ${(await (await request.post("/api/auth/login", { data: { email: SUPER_ADMIN.email, password: SUPER_ADMIN.password } })).json()).token}` };
  expect((await request.post(`/api/admin/marketplace/listings/${tenantId}/approve`, { headers: admin })).status()).toBe(200);

  const created = await request.post("/api/products", {
    headers: vendor,
    data: {
      name: product.name,
      defaultGstRate: 0,
      defaultUom: "PCS",
      variants: [{ sku: `${label.toUpperCase()}-${suffix}`, attributes: product.attributes, purchasePrice: 0, sellingPrice: product.price }],
    },
  });
  expect(created.status()).toBe(201);
  const body = await created.json();
  const variantId = body.variants[0].id as string;
  expect((await request.post(`/api/stock/variants/${variantId}/adjustments`, { headers: vendor, data: { signedDelta: 25, remarks: "Opening stock" } })).status()).toBe(201);

  if (withPhoto) {
    // Ladies Wear photos must be portrait and at least 720x900.
    const photo = await request.post(`/api/products/${body.id}/variants/${variantId}/images`, {
      headers: vendor,
      multipart: { file: { name: "kurti.png", mimeType: "image/png", buffer: solidPng(900, 1200) } },
    });
    expect(photo.status(), "product photo upload").toBe(201);
  }
  return { tenantId, businessName, vendor, productId: body.id, variantId };
}

/** A phone screen must never scroll sideways: that is the commonest way a web page breaks on a phone. */
async function expectNoSidewaysScroll(page: Page, where: string) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow, `sideways scroll on ${where}`).toBeLessThanOrEqual(1);
}

/**
 * The shopper's journey on a phone-sized screen (Pixel 7): the menu behind the hamburger button, finding a shop by
 * search, a checkout-mode grocery shop (add to cart, the cart drawer, the checkout dialog, placing an order that the
 * vendor then receives), and a Ladies Wear shop's photo opening in the click-to-zoom view. After each screen it checks
 * the page does not scroll sideways. Part of the baseline for the Capacitor Android app: the app shows these same
 * pages inside a native shell, so a layout that holds here holds there.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a shopper explores, orders and views photos on a phone-sized screen", async ({ page, request, isMobile }) => {
  test.skip(!isMobile, "Phone-layout journey: run it with `npm run test:mobile`.");
  test.setTimeout(240_000);

  const suffix = timestampSuffix();
  const grocery = await createShop(request, suffix, "Grocery", "Grocery", "CHECKOUT", { name: "Amul Milk", price: 60, attributes: {} }, false);
  const ladies = await createShop(request, suffix, "Ladies", "Ladies Wear", "WHATSAPP", { name: "Biba Kurti", price: 650, attributes: { Size: "M" } }, true);

  // ---- The menu sits behind the hamburger button on a phone ----
  await page.goto("/shops");
  const browseAll = page.getByRole("button", { name: "Browse all businesses" });
  if (await browseAll.isVisible({ timeout: 5_000 }).catch(() => false)) await browseAll.click();
  await expectNoSidewaysScroll(page, "Explore Shops");
  const menu = page.getByRole("navigation", { name: "Main navigation" });
  await expect(menu.getByRole("link", { name: "Trending" })).toBeHidden();
  await page.getByRole("button", { name: "Toggle navigation" }).click();
  await expect(menu.getByRole("link", { name: "Explore Shops" })).toBeVisible();
  await menu.getByRole("link", { name: "Trending" }).click();
  await expect(page.getByRole("heading", { name: "Latest offers near you" })).toBeVisible();
  await expectNoSidewaysScroll(page, "Trending");

  // ---- Finding a shop by search ----
  await page.goto("/shops");
  if (await browseAll.isVisible({ timeout: 5_000 }).catch(() => false)) await browseAll.click();
  await page.getByLabel("Search shops, products, areas or pincodes").fill(grocery.businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await expect(page.getByRole("article").filter({ hasText: grocery.businessName })).toBeVisible();
  await page.getByRole("link", { name: new RegExp(`View all products from ${grocery.businessName}`) }).click();
  await expect(page.getByRole("heading", { name: grocery.businessName })).toBeVisible();
  await expectNoSidewaysScroll(page, "the shop page");

  // ---- Cart and checkout on a phone ----
  await page.getByRole("button", { name: "Add Amul Milk to cart" }).click();
  await page.getByRole("button", { name: "Add one more Amul Milk" }).click();
  await page.getByRole("button", { name: "Cart, 2 items" }).click();
  const cart = page.getByRole("dialog", { name: "Your cart" });
  await expect(cart.getByText("2 items across 1 products")).toBeVisible();
  await expectNoSidewaysScroll(page, "the cart drawer");
  await cart.getByRole("button", { name: "Checkout" }).click();
  const checkout = page.getByRole("dialog", { name: "Checkout" });
  await checkout.getByLabel("Name").fill("Rahul Sharma");
  await checkout.getByLabel("Phone").fill("9123456780");
  await checkout.getByLabel("Address").fill("12 MG Road");
  await checkout.getByLabel("City").fill("Pune");
  await checkout.getByLabel("Pincode").fill("411038");
  await expectNoSidewaysScroll(page, "the checkout dialog");
  await checkout.getByRole("button", { name: "Place order" }).click();
  await expect(page.getByText(/Order placed!/)).toBeVisible();

  const orders = (await (await request.get("/api/storefront/orders", { headers: grocery.vendor })).json()) as { customerName: string; status: string }[];
  expect(orders).toHaveLength(1);
  expect(orders[0]).toMatchObject({ customerName: "Rahul Sharma", status: "NEW" });

  // ---- A Ladies Wear photo opens in the zoom view and the page stays put ----
  await page.goto(`/shop/${ladies.tenantId}`);
  await expect(page.getByRole("heading", { name: ladies.businessName })).toBeVisible();
  await expectNoSidewaysScroll(page, "the Ladies Wear shop page");
  await page.getByRole("button", { name: "Enlarge photo of Biba Kurti" }).click();
  const zoom = page.locator("#photo-zoom");
  await expect(zoom).toBeVisible();
  await expectNoSidewaysScroll(page, "the photo zoom");
  await zoom.locator("img").click();
  await expect(zoom.locator(".zoom-frame.is-zoomed")).toBeVisible();
  await zoom.getByRole("button", { name: "Close photo" }).click();
  await expect(zoom).toBeHidden();
});
