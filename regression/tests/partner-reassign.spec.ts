import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { adminHeaders, createActivePartner, createTestArea } from "./helpers/partnerHelpers";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

interface Tenant {
  id: string;
  businessName: string;
  partnerName: string | null;
  partnerActive: boolean | null;
}

/**
 * Handing out shops that need a partner. Two shops apply in a new area nobody covers, then two partners are approved
 * for it: "Shops Needing a Partner" lists the area and one click gives each shop to a different partner. When the first
 * partner is switched off their shop is waiting again and the same button moves it to the other. On a shop's vendor
 * page Super Admin can take a shop back ("No partner"), and a partner may not change assignments. Leaves its data in
 * place (names carry a timestamp).
 */
test("Super Admin hands out waiting shops, reclaims one, and partners cannot reassign", async ({ page, request }) => {
  const suffix = timestampSuffix();
  const admin = await adminHeaders(request);
  const areaName = `Partner Reassign Area ${suffix}`;
  const areaId = await createTestArea(request, admin, areaName);

  async function applyShop(label: string): Promise<string> {
    const businessName = `Partner Reassign Shop ${label} ${suffix}`;
    await applyForVendorAccount(page, {
      businessName,
      ownerName: "Test Owner",
      email: `partner-reassign-shop-${label}-${suffix}@test.com`,
      password: "password1234",
      area: areaName,
    });
    return businessName;
  }
  const shopOne = await applyShop("1");
  const shopTwo = await applyShop("2");

  const partnerFor = (label: string, digit: number) => ({
    name: `Reassign Partner ${label} ${suffix}`,
    email: `reassign-partner-${label.toLowerCase()}-${suffix}@test.com`,
    whatsapp: `9${suffix.slice(-8)}${digit}`,
    areaIds: [areaId],
  });
  const partnerOne = await createActivePartner(request, admin, partnerFor("One", 1));
  const partnerTwo = await createActivePartner(request, admin, partnerFor("Two", 2));

  async function tenantsByName(): Promise<Map<string, Tenant>> {
    const all = (await (await request.get("/api/admin/tenants", { headers: admin })).json()) as Tenant[];
    return new Map(all.map((tenant) => [tenant.businessName, tenant]));
  }
  expect((await tenantsByName()).get(shopOne)?.partnerName, "nobody covered the area when the shop applied").toBeNull();

  // One click gives each waiting shop to a partner, in turns.
  await loginAsSuperAdmin(page);
  await page.goto(`${FRONTEND_WEB_URL}/admin/partner-waiting`);
  const areaRow = page.getByRole("row", { name: new RegExp(areaName) });
  const waitingCount = areaRow.getByRole("cell").nth(2);
  await expect(waitingCount).toHaveText("2");
  await areaRow.getByRole("button", { name: "Assign shops" }).click();
  await expect(page.getByText("2 shops assigned")).toBeVisible();
  await expect(areaRow).toBeHidden();
  let shops = await tenantsByName();
  expect(shops.get(shopOne)?.partnerName).toBe(partnerOne.name);
  expect(shops.get(shopTwo)?.partnerName).toBe(partnerTwo.name);

  // Switching the first partner off puts their shop back on the list; the same button moves it to the other partner.
  expect((await request.post(`/api/admin/partners/${partnerOne.id}/deactivate`, { headers: admin })).status()).toBe(200);
  expect((await tenantsByName()).get(shopOne)?.partnerActive).toBe(false);
  await page.goto(`${FRONTEND_WEB_URL}/admin/partner-waiting`);
  await expect(waitingCount).toHaveText("1");
  await areaRow.getByRole("button", { name: "Assign shops" }).click();
  await expect(page.getByText("1 shop assigned")).toBeVisible();
  shops = await tenantsByName();
  expect(shops.get(shopOne)?.partnerName).toBe(partnerTwo.name);
  expect(shops.get(shopOne)?.partnerActive).toBe(true);

  // On the vendor page Super Admin takes the shop back, and it is waiting again.
  await page.goto(`${FRONTEND_WEB_URL}/admin/vendors/${shops.get(shopOne)!.id}`);
  await page.getByLabel("Local Partner").selectOption({ label: "No partner (Super Admin's queue)" });
  await page.getByRole("button", { name: "Save Partner" }).click();
  await expect(page.getByText("Local partner updated")).toBeVisible();
  expect((await tenantsByName()).get(shopOne)?.partnerName).toBeNull();
  await page.goto(`${FRONTEND_WEB_URL}/admin/partner-waiting`);
  await expect(waitingCount).toHaveText("1");

  // A partner cannot move shops, not even their own, and cannot reach the waiting list.
  const login = await request.post("/api/auth/login", { data: { email: partnerTwo.email, password: "password1234" } });
  const partner = { Authorization: `Bearer ${(await login.json()).token}` };
  expect((await request.put(`/api/admin/tenants/${shops.get(shopTwo)!.id}/partner`, { headers: partner, data: { partnerId: null } })).status()).toBe(403);
  expect((await request.get("/api/admin/partner-waiting-areas", { headers: partner })).status()).toBe(403);
});
