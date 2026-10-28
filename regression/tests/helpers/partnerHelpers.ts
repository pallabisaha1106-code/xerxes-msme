import { expect, type APIRequestContext } from "@playwright/test";
import { SUPER_ADMIN } from "../../config";

export interface AdminPartner {
  id: string;
  name: string;
  email: string;
  status: string;
}

export async function adminHeaders(request: APIRequestContext): Promise<{ Authorization: string }> {
  const login = await request.post("/api/auth/login", { data: { email: SUPER_ADMIN.email, password: SUPER_ADMIN.password } });
  return { Authorization: `Bearer ${(await login.json()).token}` };
}

/**
 * A new service area in Pune that nobody else uses, so a spec's partners are the only ones covering it - the partner
 * lists and assignment order of other runs (and of the 600 Kothrud shops) cannot interfere.
 */
export async function createTestArea(request: APIRequestContext, admin: { Authorization: string }, name: string): Promise<string> {
  const cities = (await (await request.get("/api/public/marketplace/service-cities?stateCode=27")).json()) as { id: string; name: string }[];
  const pune = cities.find((city) => city.name === "Pune");
  expect(pune, "Pune is seeded (V13)").toBeTruthy();
  const created = await request.post("/api/admin/service-areas", {
    headers: admin,
    data: { cityId: pune!.id, name, pincode: "411999", displayOrder: 999 },
  });
  expect(created.status(), "create the test area").toBe(201);
  return (await created.json()).id as string;
}

/** Applies as a partner through the public API, then has Super Admin approve it. Returns the partner as Super Admin sees it. */
export async function createActivePartner(
  request: APIRequestContext,
  admin: { Authorization: string },
  input: { name: string; email: string; whatsapp: string; areaIds: string[] },
): Promise<AdminPartner> {
  const applied = await request.post("/api/public/partner-applications", { data: { ...input, password: "password1234" } });
  expect(applied.status(), "partner application").toBe(201);
  const partnerId = (await applied.json()).partnerId as string;
  const approved = await request.post(`/api/admin/partners/${partnerId}/approve`, { headers: admin });
  expect(approved.status(), "approve the partner").toBe(200);
  return (await approved.json()) as AdminPartner;
}
