import { test, expect } from "@playwright/test";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { adminHeaders, createActivePartner, createTestArea } from "./helpers/partnerHelpers";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

/**
 * A new shop application is given to an active partner covering its area, taking turns; a deactivated partner gets
 * no new shops; and a shop in an area nobody covers is told the platform team will be in touch. Partners and areas
 * are created by the spec for this run only (names carry a timestamp), so other runs and real areas are untouched.
 */
test("shops are shared between the partners of an area, skipping inactive ones and areas with none", async ({ page, request }) => {
  const suffix = timestampSuffix();
  const admin = await adminHeaders(request);
  const covered = `Partner Spec Covered ${suffix}`;
  const uncovered = `Partner Spec Uncovered ${suffix}`;
  const coveredAreaId = await createTestArea(request, admin, covered);
  await createTestArea(request, admin, uncovered);

  const partnerFor = (label: string, digit: number) => ({
    name: `Spec Partner ${label} ${suffix}`,
    email: `spec-partner-${label.toLowerCase()}-${suffix}@test.com`,
    whatsapp: `9${suffix.slice(-8)}${digit}`,
    areaIds: [coveredAreaId],
  });
  const one = await createActivePartner(request, admin, partnerFor("One", 1));
  const two = await createActivePartner(request, admin, partnerFor("Two", 2));

  async function applyShop(label: string, area: string): Promise<string> {
    const businessName = `Partner Spec Shop ${label} ${suffix}`;
    await applyForVendorAccount(page, {
      businessName,
      ownerName: "Test Owner",
      email: `partner-spec-shop-${label}-${suffix}@test.com`,
      password: "password1234",
      area,
    });
    return businessName;
  }
  const card = page.locator(".partner-card");

  // Equal turns: the partner who applied first gets the first shop, then it alternates.
  const shopOne = await applyShop("1", covered);
  await expect(card).toContainText(one.name);
  await applyShop("2", covered);
  await expect(card).toContainText(two.name);
  await applyShop("3", covered);
  await expect(card).toContainText(one.name);

  // Super Admin sees the partner on the vendor, and a partner's shop count follows.
  const tenants = (await (await request.get("/api/admin/tenants", { headers: admin })).json()) as { businessName: string; partnerName: string | null }[];
  expect(tenants.find((tenant) => tenant.businessName === shopOne)?.partnerName).toBe(one.name);
  const partners = (await (await request.get("/api/admin/partners?status=ACTIVE", { headers: admin })).json()) as { id: string; assignedShopCount: number }[];
  expect(partners.find((partner) => partner.id === one.id)?.assignedShopCount).toBe(2);
  expect(partners.find((partner) => partner.id === two.id)?.assignedShopCount).toBe(1);

  // An inactive partner is skipped, even when it is their turn.
  expect((await request.post(`/api/admin/partners/${one.id}/deactivate`, { headers: admin })).status()).toBe(200);
  await applyShop("4", covered);
  await expect(card).toContainText(two.name);

  // An area with no partner: the platform team steps in.
  await applyShop("5", uncovered);
  await expect(card).toContainText("Our platform team will contact you shortly");
});
