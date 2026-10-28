# Regression (Playwright)

End-to-end tests against the real frontend + backend + database. Playwright drives
a browser exactly like a user would, so these catch integration bugs unit tests can't
(wrong API wiring, CORS, routing, form validation shown correctly, etc).

Two of these specs (`catalog-apparel`, `vendor-purchase`) can also double as a **master
data seeding tool** — run with teardown skipped, they leave realistic categories,
products, vendors, and a purchase entry in place for manual use, since they create
that data by driving the real UI rather than inserting rows directly. See
[Running without teardown](#running-a-spec-without-teardown-master-data-setup) below.

## Prerequisites

Playwright auto-starts the frontend dev server for you, but it can't orchestrate the
Java/Postgres stack, so start those first:

```bash
# 1. Postgres (once; reuse the container after)
docker run -d --name xerxes-msme-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=xerxes_msme -p 5432:5432 postgres:16-alpine

# 2. Backend
cd ../backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run
```

## Setup

```bash
npm install
npx playwright install --with-deps chromium   # first time only
```

## Standing test account

`config.ts` holds credentials for a real, persistent account in the target database
(`veera@test.com` / `password1234`) - created once by hand, not by a test. **Any future
spec that needs to be logged in as an existing user should use this** rather than
hardcoding credentials or registering a throwaway account:

```ts
import { loginAsTestUser } from "./helpers/login";

test("...", async ({ page }) => {
  await loginAsTestUser(page);
  // page is now on "/", logged in as the standing test user
});
```

This is separate from `register-business.spec.ts`, which deliberately registers and
tears down its own fixed-but-disposable accounts (`veera@example.com`,
`veera.duplicate@example.com`) to test the registration flow itself.

## Super Admin login (required for `vendor-registration.spec.ts`)

`config.ts` also holds a standing `SUPER_ADMIN` login (`admin@xerxes.internal`), used by
`tests/helpers/adminLogin.ts`'s `loginAsSuperAdmin(page)`. The seed migration
(`V5__platform_admin.sql`) creates that account with a bcrypt hash whose plaintext isn't
recoverable, so **before running `vendor-registration.spec.ts` for the first time**, set
a known password by running this once against your dev DB:

```bash
docker exec xerxes-msme-postgres psql -U postgres -d xerxes_msme -c "UPDATE users SET password_hash = '\$2a\$10\$Gi94OpvnSDveQytfGE2C9utXMTLoH.Gb2gE8tPaic3Q86gdPUItdW' WHERE email = 'admin@xerxes.internal';"
```

That hash was generated and verified against this codebase's own `BCryptPasswordEncoder`
(not guessed) and corresponds to the password already in `config.ts`
(`SuperAdmin@123`). This only needs to be run once per database.

## The scripts

| Spec file | Purpose | Data it creates | Teardown |
|---|---|---|---|
| [`login.spec.ts`](tests/login.spec.ts) | Smoke test: the standing test user can log in and reach the dashboard. | None | N/A — doesn't create data |
| [`register-business.spec.ts`](tests/register-business.spec.ts) | Two cases: (1) a new owner can register a business end-to-end and land on the dashboard, (2) registering with an already-used email is rejected. | A throwaway tenant/user (`veera@example.com`) | Always torn down (fixed, not skippable) |
| [`catalog-apparel.spec.ts`](tests/catalog-apparel.spec.ts) | Category creation, then two products with variants, exercising the "Add attribute"/"Add Variant" dynamic form flows and GST/HSN inheritance from the product default. | 2 categories (Men's Apparel, Women's Apparel) + 2 products (Men's Crew Neck T-Shirt with 2 size variants, Women's Floral Dress with 1 variant) | Skippable via `SKIP_TEARDOWN=true` |
| [`vendor-purchase.spec.ts`](tests/vendor-purchase.spec.ts) | Vendor registration, then a purchase entry with two line items, exercising the "Add Line Item" flow and computed subtotal/tax/total. | 1 category (Footwear) + 1 product (Running Shoes, 1 variant) + 1 vendor (Global Footwear Distributors) + 1 purchase entry (2 line items, PO-2026-100) | Skippable via `SKIP_TEARDOWN=true` |
| [`vendor-registration.spec.ts`](tests/vendor-registration.spec.ts) | Cross-app: starts on **marketplace-web** (port 5174), crosses over to **frontend-web** (port 5173) for the admin steps, and back. Full journey: Step 1 apply form (business/owner details + password + 3 KYC document uploads) → registration code shown → Step 2 catalogue page reached via that code → switch to the Plus plan → fill listing details + shop photo → pick 15 starter products spread across many Grocery sub-categories (exercising the full bulk-imported catalog, not just a token pick) → submit → confirm login is rejected while still pending → log in as Super Admin → approve the vendor account (Admin → Vendors → Reactivate) → approve the marketplace listing itself (Admin → Marketplace Listings → Approve - a separate review step) → back on marketplace-web, search and confirm the shop is now publicly live **with its real shop photo actually loading** (not the placeholder - this doubles as an end-to-end check of the image proxy) → confirm the vendor can now log in. Records video (`video: "on"`) so the whole run can be watched back. | A throwaway tenant/user (`priya.marketplace@example.com`) | Skippable via `SKIP_TEARDOWN=true` |

Supporting files (not tests themselves):
- [`tests/helpers/login.ts`](tests/helpers/login.ts) — shared `loginAsTestUser(page)`.
- [`tests/helpers/adminLogin.ts`](tests/helpers/adminLogin.ts) — shared
  `loginAsSuperAdmin(page)`, always navigates to frontend-web's own origin by absolute
  URL (`FRONTEND_WEB_URL` in `config.ts`) so it works from any spec's baseURL.
- [`tests/helpers/vendorApplication.ts`](tests/helpers/vendorApplication.ts) — shared
  `applyForVendorAccount(page, input)`, drives marketplace-web's Step 1 form and returns
  the registration code.
- [`fixtures/`](fixtures/) — tiny placeholder PNGs used for the 3 KYC document uploads,
  plus a real 1200x700 JPEG (`shop-photo-listing.jpg`) for the shop photo specifically
  (the KYC uploads aren't dimension-checked server-side, but the listing photo is).
- [`tests/db.ts`](tests/db.ts) — direct-Postgres teardown helpers (`deleteTenantByEmail`,
  `deleteCategoryByName`, `deleteProductByName`, `deleteVendorByName`), used by the specs
  above since there's no delete-tenant/product/vendor API to call instead.
- [`config.ts`](config.ts) — the standing `TEST_USER` and `SUPER_ADMIN` credentials, and
  `FRONTEND_WEB_URL` (frontend-web's origin, for cross-app specs).
- [`playwright.config.ts`](playwright.config.ts) — base URL, browser project, and the
  `webServer` array that auto-starts both the frontend-web (5173) and marketplace-web
  (5174) dev servers. If you already started them yourself, Playwright detects the
  running servers and reuses them instead of starting duplicates.

## Commands to run them

All commands run from this `regression/` directory.

```bash
npm test                                    # whole suite, headless
npx playwright test tests/login.spec.ts     # a single spec file
npx playwright test catalog-apparel         # a spec by name fragment
npm run test:headed                         # whole suite, visible browser
npm run test:ui                             # Playwright's interactive UI mode
npm run report                              # open the last run's HTML report
```

Each test uses a fixed test identity (owner name "Veera", e.g. `veera@example.com`)
rather than a random one, and tears down its own data directly via Postgres before
*and* after each test (`tests/db.ts`) - before, in case a previous run crashed
mid-test and skipped its own teardown; after, so nothing lingers.

DB connection defaults match the local Docker setup (`localhost:5432`, `postgres`/`postgres`,
db `xerxes_msme`) - override with `PGHOST` / `PGPORT` / `PGUSER` / `PGPASSWORD` / `PGDATABASE`
if yours differs.

## Running a spec without teardown (master data setup)

`catalog-apparel.spec.ts` and `vendor-purchase.spec.ts`, run under the standing
`veera@test.com` account, respect `SKIP_TEARDOWN=true` to leave their created data in
place afterward instead of deleting it:

```bash
SKIP_TEARDOWN=true npx playwright test catalog-apparel
SKIP_TEARDOWN=true npx playwright test vendor-purchase
```

Because these specs create data by driving the real UI (filling in the same forms a
shopkeeper would, clicking the same buttons), running them this way is a quick way to
seed realistic master data into `veera@test.com`'s tenant for manual exploration, demos,
or as a starting point before testing something else by hand — rather than writing SQL
inserts or clicking through the UI yourself. After either run, log in as
`veera@test.com` / `password1234` and the categories/products (and vendor + purchase
entry, if you ran that one) are there in the UI.

`vendor-registration.spec.ts` supports the same flag, but for a different purpose: it
leaves the applied-but-not-yet-approved tenant (`priya.marketplace@example.com`) in the
database and visible in **Admin → Vendors** as a suspended/pending row, so you can
inspect the application, its KYC documents, and approve it by hand.

```bash
SKIP_TEARDOWN=true npx playwright test vendor-registration --headed
```

Caveats:

- This skips *both* the pre-test and post-test cleanup. It only works cleanly on a
  **fresh run** - running it again with `SKIP_TEARDOWN` set while the previous run's
  data is still there will fail with duplicate-category/SKU/vendor errors, since
  nothing gets cleared first either.
- `login.spec.ts` and `register-business.spec.ts` don't have this flag:
  `login.spec.ts` never creates data, and `register-business.spec.ts` always tears
  down since its whole purpose is testing the registration flow itself, not seeding
  data you'd want to keep.
- Unlike the other two, a skipped `vendor-registration.spec.ts` run is trivial to
  clean up afterward - it creates a whole separate tenant rather than adding rows to
  the standing `veera@test.com` one, so either running it again *without*
  `SKIP_TEARDOWN`, or one `DELETE FROM tenants WHERE email='priya.marketplace@example.com'`,
  clears everything at once (cascades to the user, listing, etc.); no ordering to
  worry about.
- To go back to a clean slate, either run the same spec again *without*
  `SKIP_TEARDOWN` (normal teardown will clean up what it created), or delete manually.
  **Order matters** - `stock_ledger` and `purchase_entries` reference product variants
  and vendors without cascading, so they have to go first:

```bash
docker exec xerxes-msme-postgres psql -U postgres -d xerxes_msme -c "
DELETE FROM stock_ledger WHERE tenant_id=(SELECT id FROM tenants WHERE email='veera@test.com');
DELETE FROM purchase_entries WHERE tenant_id=(SELECT id FROM tenants WHERE email='veera@test.com');
DELETE FROM vendors WHERE tenant_id=(SELECT id FROM tenants WHERE email='veera@test.com');
DELETE FROM products WHERE tenant_id=(SELECT id FROM tenants WHERE email='veera@test.com');
DELETE FROM categories WHERE tenant_id=(SELECT id FROM tenants WHERE email='veera@test.com');
"
```

(`tests/db.ts`'s `deleteProductByName`/`deleteVendorByName` handle this ordering
automatically when teardown is *not* skipped - clearing stock ledger, purchase/invoice
line items, or purchase entries for that product/vendor before deleting it.)

For `register-business.spec.ts`, which always tears down its own tenant regardless
(deleting the tenant row cascades to everything under it - user, products, invoices,
... - per the schema's `ON DELETE CASCADE` foreign keys), no manual cleanup is ever
needed.
