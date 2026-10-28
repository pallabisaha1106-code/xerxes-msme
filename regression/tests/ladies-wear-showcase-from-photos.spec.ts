import path from "node:path";
import { test, expect, type Page } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount, FIXTURES_DIR } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
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
 * Registers a Ladies Wear vendor on the Free plan with no starter products (the shop's whole catalogue comes from
 * photos later), enquiring by WhatsApp like a real listing shop, then has Super Admin approve the vendor and the
 * listing so the shop is live, and logs the vendor in to frontend-web.
 */
async function registerLiveLadiesWearShop(page: Page, suffix: string): Promise<LiveVendor> {
  const email = `rangoli-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Rangoli Fashions ${suffix}`;

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

  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  return { email, password, businessName };
}

/**
 * A showcase Ladies Wear shop built from real product photos. The vendor registers, the shop goes live, and the whole
 * catalogue - five kurti sets and a chaniya choli, with sizes and prices - is added through "Add from photos".
 * A shopper then finds the shop on the marketplace and sees the products.
 *
 * Deliberately leaves its data in place afterward, as a realistic demo shop - see
 * grocery-vendor-free-whatsapp.spec.ts for why the timestamp suffix makes that safe.
 */
test("ladies wear showcase shop is built from real photos with Add from photos and shows on the marketplace", async ({
  page,
}) => {
  test.setTimeout(300_000);

  const vendor = await registerLiveLadiesWearShop(page, timestampSuffix());

  // The shop starts empty - nothing in the catalogue yet - and the flow is offered to it.
  await page.goto(`${FRONTEND_WEB_URL}/products`);
  await expect(page.getByText("No products yet")).toBeVisible();
  await expect(page.getByRole("button", { name: "Add from photos" })).toBeVisible();

  // ---- Drop the five product photos. Their file names carry the name and category (IMG_<Category>_<Name>.jpg). ----
  await page.getByRole("button", { name: "Add from photos" }).click();
  await expect(page.getByRole("heading", { name: "Add products from photos" })).toBeVisible();

  const products = [
    { file: "IMG_Kurti_Pink Brocade Set.jpg", name: "Kurti Pink Brocade Set", price: "1299", sizes: true },
    { file: "IMG_Kurti_Teal Anarkali Set.jpg", name: "Kurti Teal Anarkali Set", price: "1299", sizes: true },
    { file: "IMG_Chaniya Choli_Black Navratri.jpg", name: "Chaniya Choli Black Navratri", price: "1499", sizes: false },
    { file: "IMG_Kurti_Orange Block Print.jpg", name: "Kurti Orange Block Print", price: "1199", sizes: true },
    { file: "IMG_Kurti_Black Ajrakh Set.jpg", name: "Kurti Black Ajrakh Set", price: "1199", sizes: true },
  ];
  await page.locator('input[type="file"]').setInputFiles(products.map((product) => path.join(FIXTURES_DIR, product.file)));

  const card = (file: string) => page.getByRole("article").filter({ hasText: file });
  const confirmBar = page.locator(".qa-confirm");

  // Every photo is already portrait and large enough, so none is cropped or skipped.
  await expect(page.getByRole("article")).toHaveCount(5);
  for (const product of products) {
    await expect(card(product.file).getByLabel("Name")).toHaveValue(product.name);
  }
  await expect(page.getByText("Cropped to fit")).toHaveCount(0);
  await expect(confirmBar.getByText("5 products ready to create")).toBeVisible();
  await expect(confirmBar.getByText("10 of 10 product slots left")).toBeVisible();

  // ---- The two categories named in the file names don't exist yet: one click each makes them for every card ----
  await card(products[0].file).getByRole("button", { name: 'Create "Kurti" and use it' }).click();
  await card(products[2].file).getByRole("button", { name: 'Create "Chaniya Choli" and use it' }).click();
  await expect(page.getByRole("button", { name: /Create ".*" and use it/ })).toHaveCount(0);
  await expect(confirmBar.getByText(/without a category/)).toHaveCount(0);

  // ---- Sizes S to XL with 3 pieces of each, for every card at once; then the chaniya choli (one size) drops them ----
  const applyAll = page.getByRole("region", { name: "Apply to all" });
  for (const size of ["S", "M", "L", "XL"]) {
    await applyAll.getByRole("button", { name: size, exact: true }).click();
  }
  await applyAll.getByLabel("Stock per size").fill("3");
  await applyAll.getByRole("button", { name: "Apply to all" }).click();
  await expect(card(products[0].file).getByLabel("Stock for size XL")).toHaveValue("3");

  const choli = card(products[2].file);
  for (const size of ["S", "M", "L", "XL"]) {
    await choli.getByRole("button", { name: size, exact: true }).click();
  }
  await expect(choli.getByLabel("Stock for size M")).toHaveCount(0);
  await choli.getByLabel("Stock", { exact: true }).fill("2");

  // ---- Each product's own price ----
  for (const product of products) {
    await card(product.file).getByLabel("Price (₹)").fill(product.price);
  }
  await expect(confirmBar.getByText(/without a price/)).toHaveCount(0);

  // ---- Confirm ----
  await confirmBar.getByRole("button", { name: "Create 5 products" }).click();
  await expect(page.getByRole("heading", { name: "5 products created" })).toBeVisible({ timeout: 120_000 });

  // ---- The vendor's catalogue: five products, the right categories (once each), and a variant per size ----
  await page.getByRole("button", { name: "View products" }).click();
  for (const product of products) {
    await expect(page.getByRole("button", { name: product.name, exact: true })).toBeVisible();
  }

  await page.goto(`${FRONTEND_WEB_URL}/categories`);
  await expect(page.getByText("Kurti", { exact: true })).toHaveCount(1);
  await expect(page.getByText("Chaniya Choli", { exact: true })).toHaveCount(1);

  await page.goto(`${FRONTEND_WEB_URL}/products`);
  await page.getByRole("button", { name: "Kurti Pink Brocade Set", exact: true }).click();
  const variants = page.getByRole("dialog", { name: "Variants — Kurti Pink Brocade Set" });
  for (const size of ["S", "M", "L", "XL"]) {
    const row = variants.getByRole("row", { name: new RegExp(`KURTI-PINK-BROCADE-SET-${size}$|KURTI-PINK-BROCADE-SET-${size} `) });
    await expect(row.getByRole("cell").nth(1)).toHaveText(`Size: ${size}`);
    await expect(row.getByRole("cell").nth(2)).toHaveText("₹1299.00");
    await expect(row.getByRole("cell").nth(3)).toHaveText("3");
  }
  await page.keyboard.press("Escape");

  // ---- A shopper finds the shop on the marketplace ----
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

  // One card per size: 4 kurti sets x 4 sizes + the chaniya choli = 17, and every one carries the photo
  // (the first size's upload was copied to the others).
  await expect(page.locator("article.catalogue-product")).toHaveCount(17);
  await expect(page.getByRole("button", { name: /^Enlarge photo of / })).toHaveCount(17);
  await expect(page.getByRole("link", { name: /^Enquire with .* on WhatsApp about / })).toHaveCount(17);

  const shopCard = (sku: string) => page.locator("article.catalogue-product").filter({ hasText: `Ref ${sku}` });

  // A sized card: name, size, price, photo, enquiry link.
  const pinkM = shopCard("KURTI-PINK-BROCADE-SET-M");
  await expect(pinkM.getByRole("heading", { name: "Kurti Pink Brocade Set" })).toBeVisible();
  await expect(pinkM.getByText("M · Ref KURTI-PINK-BROCADE-SET-M")).toBeVisible();
  await expect(pinkM.getByText(/₹\s?1,?299/)).toBeVisible();
  await expect(pinkM.getByRole("link", { name: /Enquire with .* on WhatsApp about Kurti Pink Brocade Set/ })).toBeVisible();

  // The other products at their own prices, and the unsized chaniya choli as a single card.
  await expect(shopCard("KURTI-TEAL-ANARKALI-SET-S").getByText(/₹\s?1,?299/)).toBeVisible();
  await expect(shopCard("KURTI-ORANGE-BLOCK-PRINT-XL").getByText(/₹\s?1,?199/)).toBeVisible();
  await expect(shopCard("KURTI-BLACK-AJRAKH-SET-L").getByText(/₹\s?1,?199/)).toBeVisible();
  const choliCard = shopCard("CHANIYA-CHOLI-BLACK-NAVRATRI");
  await expect(choliCard).toHaveCount(1);
  await expect(choliCard.getByRole("heading", { name: "Chaniya Choli Black Navratri" })).toBeVisible();
  await expect(choliCard.getByText(/₹\s?1,?499/)).toBeVisible();

  // Ladies Wear shows photos through the click-to-zoom view, on a copied photo too (the L size, not the first).
  await shopCard("KURTI-PINK-BROCADE-SET-L").getByRole("button", { name: "Enlarge photo of Kurti Pink Brocade Set" }).click();
  const zoomDialog = page.locator("#photo-zoom");
  await expect(zoomDialog).toBeVisible();
  await zoomDialog.locator("img").click();
  await expect(zoomDialog.locator(".zoom-frame.is-zoomed")).toBeVisible();
  await zoomDialog.getByRole("button", { name: "Close photo" }).click();
});
