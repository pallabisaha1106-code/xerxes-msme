import { test, expect, type APIRequestContext } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { adminHeaders, createActivePartner, createTestArea } from "./helpers/partnerHelpers";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

interface Tenant {
  id: string;
  businessName: string;
}

async function signInHeaders(request: APIRequestContext, email: string): Promise<{ Authorization: string }> {
  const login = await request.post("/api/auth/login", { data: { email, password: "password1234" } });
  expect(login.status(), "partner sign-in").toBe(200);
  return { Authorization: `Bearer ${(await login.json()).token}` };
}

/**
 * What a local partner may do with the shops assigned to them, and nothing more. Two partners share a new area, so
 * the first shop goes to one and the second to the other; a third shop is in an area nobody covers. The first partner
 * activates their shop and approves its listing on the same screens Super Admin uses, sees none of the other two
 * shops, and the server refuses everything outside their shops and everything Super Admin-only. Leaves its data in
 * place (names carry a timestamp).
 */
test("a partner reviews only their own shops, on the shared screens", async ({ page, browser, request }) => {
  const suffix = timestampSuffix();
  const admin = await adminHeaders(request);
  const covered = `Partner Review Covered ${suffix}`;
  const uncovered = `Partner Review Uncovered ${suffix}`;
  const coveredAreaId = await createTestArea(request, admin, covered);
  await createTestArea(request, admin, uncovered);
  const partnerFor = (label: string, digit: number) => ({
    name: `Review Partner ${label} ${suffix}`,
    email: `review-partner-${label.toLowerCase()}-${suffix}@test.com`,
    whatsapp: `9${suffix.slice(-8)}${digit}`,
    areaIds: [coveredAreaId],
  });
  const one = partnerFor("One", 1);
  await createActivePartner(request, admin, one);
  await createActivePartner(request, admin, partnerFor("Two", 2));

  async function applyShop(label: string, area: string): Promise<string> {
    const businessName = `Partner Review Shop ${label} ${suffix}`;
    await applyForVendorAccount(page, {
      businessName,
      ownerName: "Test Owner",
      email: `partner-review-shop-${label}-${suffix}@test.com`,
      password: "password1234",
      area,
    });
    return businessName;
  }

  // Shop A (partner one) is taken through the catalogue so its listing is waiting for review.
  const shopA = await applyShop("A", covered);
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
  await page.getByRole("checkbox", { name: "Amul Milk" }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${shopA}`) })).toBeVisible({ timeout: 15_000 });
  const shopB = await applyShop("B", covered); // partner two's
  const shopC = await applyShop("C", uncovered); // nobody's

  const tenants = (await (await request.get("/api/admin/tenants", { headers: admin })).json()) as Tenant[];
  const idOf = (name: string) => tenants.find((tenant) => tenant.businessName === name)!.id;

  // On the dashboard, partner one sees shop A only, with no sales columns, and can activate it.
  const partnerPage = await (await browser.newContext()).newPage();
  await partnerPage.goto(`${FRONTEND_WEB_URL}/login`);
  await partnerPage.getByLabel("Email").fill(one.email);
  await partnerPage.getByLabel("Password", { exact: true }).fill("password1234");
  await partnerPage.getByRole("button", { name: "Sign In" }).click();
  await partnerPage.waitForURL(`${FRONTEND_WEB_URL}/admin`);
  await expect(partnerPage.getByRole("button", { name: shopA })).toBeVisible();
  await expect(partnerPage.getByText(shopB)).toHaveCount(0);
  await expect(partnerPage.getByText(shopC)).toHaveCount(0);
  await expect(partnerPage.getByRole("columnheader", { name: "Customers" })).toHaveCount(0);
  await expect(partnerPage.getByRole("columnheader", { name: "Invoices" })).toHaveCount(0);

  await partnerPage.getByRole("button", { name: shopA }).click();
  await expect(partnerPage.getByRole("heading", { name: shopA })).toBeVisible();
  await expect(partnerPage.getByLabel("Subscription Plan")).toHaveCount(0);
  await expect(partnerPage.getByRole("button", { name: "Generate Payment Link" })).toHaveCount(0);
  await expect(partnerPage.getByText(/^Invoices \(/)).toHaveCount(0);
  await partnerPage.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(partnerPage.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

  // ...and approve its listing: it goes LIVE. The other partner's and the unassigned shop's listings are not on the list.
  await partnerPage.getByRole("button", { name: "Marketplace Listings" }).click();
  await partnerPage.locator("#status-filter").selectOption("PENDING");
  const listingRow = partnerPage.getByRole("row", { name: new RegExp(shopA) });
  await expect(listingRow).toBeVisible();
  await expect(partnerPage.getByText(shopB)).toHaveCount(0);
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  // The server enforces it: only their own shop, no way to another partner's or an unassigned one, and nothing Super Admin-only.
  const partner = await signInHeaders(request, one.email);
  const mine = (await (await request.get("/api/admin/tenants", { headers: partner })).json()) as Tenant[];
  expect(mine.map((tenant) => tenant.businessName)).toEqual([shopA]);
  const myListings = (await (await request.get("/api/admin/marketplace/listings", { headers: partner })).json()) as { tenantId: string }[];
  expect(myListings.map((listing) => listing.tenantId)).toEqual([idOf(shopA)]);
  for (const other of [shopB, shopC]) {
    expect((await request.get(`/api/admin/tenants/${idOf(other)}`, { headers: partner })).status()).toBe(404);
    expect((await request.put(`/api/admin/tenants/${idOf(other)}/status`, { headers: partner, data: { active: true } })).status()).toBe(404);
    expect((await request.post(`/api/admin/marketplace/listings/${idOf(other)}/approve`, { headers: partner })).status()).toBe(404);
  }
  const shopAId = idOf(shopA);
  expect((await request.put(`/api/admin/tenants/${shopAId}/plan`, { headers: partner, data: { subscriptionPlan: "PLUS" } })).status()).toBe(403);
  expect((await request.get(`/api/admin/tenants/${shopAId}/invoices`, { headers: partner })).status()).toBe(403);
  expect((await request.get(`/api/admin/tenants/${shopAId}/customers`, { headers: partner })).status()).toBe(403);
  expect((await request.get("/api/admin/partners", { headers: partner })).status()).toBe(403);
  expect((await request.get("/api/admin/marketplace-categories", { headers: partner })).status()).toBe(403);

  // Super Admin is not narrowed: all three shops are still there for them.
  const everything = (await (await request.get("/api/admin/tenants", { headers: admin })).json()) as Tenant[];
  for (const name of [shopA, shopB, shopC]) expect(everything.map((tenant) => tenant.businessName)).toContain(name);
});
