import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { deleteTenantByEmail } from "./db";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";

// marketplace-web runs on a different port/app than the frontend-web dashboard
// the rest of the suite targets - see regression/playwright.config.ts.
test.use({ baseURL: "http://localhost:5174", video: "on" });

// SKIP_TEARDOWN=true skips both the pre-test and post-test cleanup, so the
// applied tenant is left in the database (and visible in Admin) for manual
// inspection - same flag and same caveat as catalog-apparel.spec.ts: only
// works cleanly on a fresh run, since nothing gets cleared beforehand either.
const skipTeardown = process.env.SKIP_TEARDOWN === "true";

// Both tests here hammer the same public, unauthenticated marketplace/storefront
// endpoints (categories, service-states/cities/areas, default-catalog, storefront
// products/orders...) - each on its own is well under PublicApiRateLimiter's
// 30-requests/60s-per-IP budget, but run at the same time (Playwright's
// fullyParallel default) their combined bursts trip it, breaking dropdowns
// with "did not find some options". .serial() keeps them in one worker, one
// after another, without slowing down the rest of the suite, which doesn't
// touch these endpoints.
test.describe.serial("Marketplace vendor registration - apply through Super Admin approval and public listing", () => {
  const email = "priya.marketplace@example.com";
  const password = "password1234";
  const checkoutEmail = "anita.checkout@example.com";
  const checkoutPassword = "password1234";
  const checkoutBusinessName = "Anita Checkout Store";

  // Defensive: clean slate even if a previous run crashed mid-test and never
  // reached its own teardown - same pattern as register-business.spec.ts.
  // Deletes both tenants unconditionally (a no-op for whichever test isn't
  // running yet) rather than maintaining separate hook pairs per test.
  test.beforeEach(() => {
    if (skipTeardown) return;
    return Promise.all([deleteTenantByEmail(email), deleteTenantByEmail(checkoutEmail)]);
  });
  test.afterEach(() => {
    if (skipTeardown) return;
    return Promise.all([deleteTenantByEmail(email), deleteTenantByEmail(checkoutEmail)]);
  });

  test("owner applies, builds their catalogue, gets approved by Super Admin, and goes live on the marketplace", async ({
    page,
  }) => {
    // ---- Step 1: apply ----
    const registrationCode = await applyForVendorAccount(page, {
      businessName: "Priya General Store",
      ownerName: "Priya",
      email,
      password,
    });
    expect(registrationCode).toMatch(/^[A-Z0-9]{8}$/);

    // ---- Registration code hand-off screen ----
    await expect(page.getByText(registrationCode)).toBeVisible();
    await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
    await expect(page).toHaveURL(new RegExp(`/list-your-business/catalogue/${registrationCode}$`));

    // ---- Step 2: pick a plan, fill the listing, seed starter products ----
    await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();

    // .click() rather than .check(): this radio is bound to server-confirmed
    // state (selecting it awaits an API call before React re-renders it as
    // checked), and .check()'s own built-in verification isn't patient enough
    // for that round-trip. The toBeChecked() assertion below retries with its
    // own timeout budget instead, so it waits out the request correctly.
    await page.getByRole("radio", { name: /^Plus/ }).click();
    await expect(page.getByRole("radio", { name: /^Plus/ })).toBeChecked();

    // Category, area and shop photo are no longer picked here - they're all
    // set at Step 1 (apply()) now: category and area are pre-filled onto the
    // draft listing, and the shop photo is seeded directly from the Step 1
    // KYC shop-front photo (see MarketplaceListingService.adoptPhotoWithoutGuidelines).
    // Nothing on this page surfaces them anymore, so there's nothing to
    // assert here beyond the flow completing.

    // 15 products spread across many categories, not just two - exercises
    // the full bulk-imported catalog (~200 products across 18 categories)
    // rather than a token pick. Categories are picked via their tile's
    // aria-label (set explicitly in DefaultCatalogPicker to just the plain
    // category name) rather than its visible text, since the tile's visible
    // text also includes a "N products · M selected" suffix that changes as
    // selections are made - matching against that directly would be both
    // unstable and ambiguous (the bulk-imported reference catalog overlaps
    // the original V7-seeded categories by name, e.g. "Pulses" is a prefix
    // of "Pulses, Dal & Legumes").
    const starterPicks: { group: string; product: string }[] = [
      { group: "Rice", product: "Kohinoor Basmati Rice" },
      { group: "Milk & Dairy Products", product: "Amul Milk" },
      { group: "Milk & Dairy Products", product: "Amul Paneer" },
      { group: "Atta, Flour, Rice & Staples", product: "Aashirvaad Atta" },
      { group: "Cooking Oil", product: "Fortune Sunflower Oil" },
      { group: "Pulses, Dal & Legumes", product: "Tata Sampann Toor Dal" },
      { group: "Pulses", product: "Toor Dal" },
      { group: "Sugar, Salt & Sweeteners", product: "Tata Sugar" },
      { group: "Spices & Masala", product: "Everest Turmeric" },
      { group: "Tea, Coffee & Health Drinks", product: "Tata Tea Premium" },
      { group: "Biscuits, Cookies & Rusks", product: "Parle-G" },
      { group: "Snacks & Namkeen", product: "Lay’s Chips" },
      { group: "Instant & Packaged Foods", product: "Maggi Noodles" },
      { group: "Sauces, Spreads & Condiments", product: "Kissan Tomato Ketchup" },
      { group: "Beverages & Packaged Drinks", product: "Bisleri Water" },
    ];
    let openGroup: string | null = null;
    for (const { group, product } of starterPicks) {
      if (group !== openGroup) {
        if (openGroup !== null) await page.getByRole("button", { name: "Done" }).click();
        await page.getByRole("button", { name: group, exact: true }).click();
        openGroup = group;
      }
      // Scoped to role=checkbox: a "Remove <product>" button for an
      // already-picked item in the same category can otherwise substring-match
      // too (e.g. "Toor Dal" inside "Remove Tata Sampann Toor Dal").
      await page.getByRole("checkbox", { name: product }).check();
    }
    await page.getByRole("button", { name: "Done" }).click();
    // Checkboxes only exist inside whichever category's dialog is currently
    // open (now closed), so the count of selected products is read from the
    // "Your catalogue" grid instead - each pick renders as its own card there.
    await expect(page.locator(".selection-card")).toHaveCount(starterPicks.length);

    await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");

    await page.getByRole("button", { name: "Submit Listing" }).click();

    // Submit chains 3 sequential API calls (listing, starter products,
    // submit), and the picker now renders the full bulk-imported catalog
    // (~200 products) rather than the original 8 - slower than the default
    // 5s assertion timeout, so this one gets a longer budget explicitly.
    await expect(page.getByRole("heading", { name: /You.re all set, Priya General Store/ })).toBeVisible({
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
    await page.getByRole("button", { name: "Priya General Store" }).click();
    await expect(page.getByRole("heading", { name: "Priya General Store" })).toBeVisible();
    await page.getByRole("button", { name: "Reactivate Vendor" }).click();
    await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

    // ---- Separately, Super Admin approves the marketplace listing itself -
    // reviewing the vendor's KYC/business application and approving their
    // storefront listing are two distinct actions (see AdminMarketplaceListingsPage
    // vs AdminVendorDetailPage) - both are needed before the shop is publicly live. ----
    await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
    await page.locator("#status-filter").selectOption("PENDING");
    const listingRow = page.getByRole("row", { name: /Priya General Store/ });
    await expect(listingRow).toBeVisible();
    await listingRow.getByRole("button", { name: "Approve" }).click();
    await expect(listingRow.getByText("LIVE")).toBeVisible();

    // ---- Now publicly visible on the marketplace, with a real (not
    // placeholder) shop photo - this also doubles as end-to-end coverage of
    // the private-GCS-bucket image proxy (MediaImageController). ----
    await page.goto("http://localhost:5174/shops");
    await page.getByRole("button", { name: "Browse all businesses" }).click();
    await page.getByLabel("Search shops, products, areas or pincodes").fill("Priya General Store");
    await page.getByRole("button", { name: "Find shops" }).click();

    const card = page.locator("article.business-card", { hasText: "Priya General Store" });
    await expect(card).toBeVisible();
    const photo = card.locator("img.shop-image");
    await expect(photo).toBeVisible();
    // If MediaImageController (or the mediaUrl it's fed) is broken, BusinessCard's
    // onError handler swaps the src to this same placeholder - so this is a real
    // end-to-end check that the photo actually loaded, not just that an <img> exists.
    await expect(photo).not.toHaveAttribute("src", /placeholder\.svg/);

    // ---- Now that the vendor is approved, login succeeds and lands on the dashboard ----
    await page.goto(`${FRONTEND_WEB_URL}/login`);
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Sign In" }).click();
    await page.waitForURL(`${FRONTEND_WEB_URL}/`);
  });

  // Covers the "Checkout on the marketplace" order mode - the counterpart to
  // the WhatsApp-enquiry flow above. Carries a placed order all the way
  // through: register with orderMode=CHECKOUT, price+stock the one starter
  // product (starter products land with zero price/stock - see
  // StarterCatalogService - so nothing is actually orderable until the
  // vendor sets both), place a real guest order from the public shop page,
  // then confirm it as the vendor and verify it turns into an invoice.
  test("owner registers with checkout order mode, prices a product, and a customer places a real order", async ({
    page,
  }) => {
    // Two deliberate 60s rate-limit-window waits (see comments below) plus
    // the actual flow itself.
    test.setTimeout(240_000);

    // .serial() alone stops the two tests' public-API bursts from overlapping
    // in time, but PublicApiRateLimiter's window is a full 60s and this
    // test's own burst (Step 1 + Step 2 + guest checkout, ~15+ calls) can
    // still land in the *tail* of the previous test's window if it starts
    // right away - the two combined approach the 30-request budget. Waiting
    // out the exact window first guarantees a clean slate rather than
    // relying on how fast the previous test happened to run.
    await page.waitForTimeout(60_000);

    const PRODUCT_NAME = "Amul Milk";
    // Matches StarterCatalogService.generateUniqueSku's derivation from the
    // product name: uppercase, non-alphanumeric runs collapsed to "-".
    const PRODUCT_SKU = "AMUL-MILK";

    // ---- Step 1: apply ----
    await applyForVendorAccount(page, {
      businessName: checkoutBusinessName,
      ownerName: "Anita",
      email: checkoutEmail,
      password: checkoutPassword,
    });
    await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

    // ---- Step 2: pick a plan, one starter product, and Checkout order mode ----
    await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
    await page.getByRole("radio", { name: /^Plus/ }).click();
    await expect(page.getByRole("radio", { name: /^Plus/ })).toBeChecked();

    await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
    await page.getByRole("checkbox", { name: PRODUCT_NAME }).check();
    await page.getByRole("button", { name: "Done" }).click();
    await expect(page.locator(".selection-card")).toHaveCount(1);

    // Checkout instead of WhatsApp - the enquiry-number field only shows for
    // WHATSAPP mode, so switching away removes it rather than leaving it
    // optional (see VendorCataloguePage.tsx).
    await page.getByLabel("How will customers order?").selectOption("CHECKOUT");
    await expect(page.getByLabel("WhatsApp number for enquiries")).toHaveCount(0);

    await page.getByRole("button", { name: "Submit Listing" }).click();
    await expect(
      page.getByRole("heading", { name: new RegExp(`You.re all set, ${checkoutBusinessName}`) }),
    ).toBeVisible({ timeout: 15_000 });

    // ---- Super Admin approves both the vendor account and the listing ----
    await loginAsSuperAdmin(page);
    await page.getByRole("button", { name: checkoutBusinessName }).click();
    await expect(page.getByRole("heading", { name: checkoutBusinessName })).toBeVisible();
    await page.getByRole("button", { name: "Reactivate Vendor" }).click();
    await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

    await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
    await page.locator("#status-filter").selectOption("PENDING");
    const listingRow = page.getByRole("row", { name: new RegExp(checkoutBusinessName) });
    await expect(listingRow).toBeVisible();
    await listingRow.getByRole("button", { name: "Approve" }).click();
    await expect(listingRow.getByText("LIVE")).toBeVisible();

    // ---- Vendor logs in and prices + stocks the one product, so it's
    // actually orderable: checkout's "Add to cart" is disabled unless
    // currentStock > 0 (StorefrontCatalogService.toResponse), and a $0
    // product would make for a pointless order anyway. ----
    await page.goto(`${FRONTEND_WEB_URL}/login`);
    await page.getByLabel("Email").fill(checkoutEmail);
    await page.getByLabel("Password", { exact: true }).fill(checkoutPassword);
    await page.getByRole("button", { name: "Sign In" }).click();
    await page.waitForURL(`${FRONTEND_WEB_URL}/`);

    await page.goto(`${FRONTEND_WEB_URL}/products`);
    await page.getByRole("button", { name: PRODUCT_NAME }).click();
    await page.getByRole("button", { name: `Edit ${PRODUCT_SKU}` }).click();
    await page.getByLabel("Purchase price").fill("40");
    await page.getByLabel("Selling price").fill("60");
    await page.getByRole("button", { name: "Save Changes" }).click();
    await expect(page.getByText("Variant updated")).toBeVisible();

    await page.goto(`${FRONTEND_WEB_URL}/stock`);
    await page.getByLabel("Product variant").selectOption({ label: `${PRODUCT_NAME} - ${PRODUCT_SKU}` });
    await page.getByRole("button", { name: "Adjust Stock" }).click();
    await page.getByLabel("Quantity change").fill("25");
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Stock adjustment recorded")).toBeVisible();

    // Step 1 + Step 2 alone (apply, application lookup, plan, catalog,
    // submit chain) already approach PublicApiRateLimiter's 30-req/60s
    // budget on their own once marketplace-web's React.StrictMode dev
    // double-invoke of mount-time effects is accounted for (every fetch
    // on first mount fires twice, not once). A second wait here lets that
    // earlier burst age out before the customer-facing pages start a fresh
    // one, instead of the two overlapping and 429-ing getMarketplaceListing/
    // listStorefrontProducts (which LiveShopPage treats the same as a real
    // 404 - see its catch block - so the symptom is "Shop unavailable").
    await page.waitForTimeout(60_000);

    // ---- A customer finds the shop and checks out, unauthenticated ----
    await page.goto("http://localhost:5174/shops");
    await page.getByRole("button", { name: "Browse all businesses" }).click();
    await page.getByLabel("Search shops, products, areas or pincodes").fill(checkoutBusinessName);
    await page.getByRole("button", { name: "Find shops" }).click();
    await page.getByRole("link", { name: new RegExp(`View all products from ${checkoutBusinessName}`) }).click();

    await expect(page.getByRole("heading", { name: checkoutBusinessName })).toBeVisible();
    await page.getByRole("button", { name: `Add ${PRODUCT_NAME} to cart` }).click();
    // The header cart icon opens the cart drawer; its label carries the item count.
    await page.getByRole("button", { name: "Cart, 1 item" }).click();
    const cartDrawer = page.getByRole("dialog", { name: "Your cart" });
    await expect(cartDrawer.getByRole("heading", { name: "Your cart" })).toBeVisible();

    await cartDrawer.getByRole("button", { name: "Checkout" }).click();
    const checkoutDialog = page.getByRole("dialog", { name: "Checkout" });
    await checkoutDialog.getByLabel("Name").fill("Rahul Sharma");
    await checkoutDialog.getByLabel("Phone").fill("9123456780");
    await checkoutDialog.getByLabel("Address").fill("12 MG Road");
    await checkoutDialog.getByLabel("City").fill("Pune");
    await checkoutDialog.getByLabel("Pincode").fill("411038");
    await checkoutDialog.getByRole("button", { name: "Place order" }).click();

    await expect(page.getByText(/Order placed!/)).toBeVisible();

    // ---- Vendor sees the guest order and confirms it, which creates a
    // real Customer + Invoice (StorefrontOrderService.confirm) ----
    await page.goto(`${FRONTEND_WEB_URL}/storefront/orders`);
    const orderRow = page.getByRole("row", { name: /Rahul Sharma/ });
    await expect(orderRow).toBeVisible();
    await expect(orderRow.getByText("NEW")).toBeVisible();
    await orderRow.getByRole("button", { name: "Confirm" }).click();
    await expect(page.getByText("Order confirmed")).toBeVisible();
    await expect(orderRow.getByText("CONFIRMED")).toBeVisible();
  });
});
