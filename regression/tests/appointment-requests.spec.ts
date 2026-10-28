import { test, expect, type APIRequestContext } from "@playwright/test";
import { SUPER_ADMIN, TEST_USER } from "../config";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// An API-level spec - no browser. It talks to the backend directly (BACKEND_URL overrides the local default), so it
// needs the backend running with the V22, V23 and V24 migrations applied and the Super Admin password set (see README).
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
test.use({ baseURL: BACKEND_URL });

interface InboxRow {
  id: string;
  customerPhone: string;
  status: string;
  date: string;
  time: string;
  othersAtSameTime: number;
  timeAlreadyBooked: boolean;
  serviceName: string;
}

/** A date (YYYY-MM-DD) in India, the time zone appointment booking runs on, a number of days from today. */
function indiaDate(plusDays = 0): string {
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + plusDays);
  return date.toISOString().slice(0, 10);
}

/**
 * Appointment requests from both sides, against a real "Salon & Spa" shop built through the API. Several shoppers
 * can ask for the same time (nothing is held), the business sees them side by side and can approve only one - the
 * other is refused with "already booked" and rejected instead. An approved time leaves the availability list, and
 * cancelling brings it back. Also: a phone number with three requests waiting is refused a fourth, a repeat request
 * for the same time is refused, bad input and unavailable times are refused, and another business cannot touch the
 * request.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("shoppers request appointment slots, the business approves one per time, and the rules are enforced", async ({ request }) => {
  test.setTimeout(120_000);

  const suffix = timestampSuffix();
  const customerA = "98765 43210"; // typed with a space; stored as 9876543210
  const customerB = "9123456780";
  const customerC = "9988776655";

  // ---- A live Salon & Spa shop with one service and every day 10:00-12:00 published ----
  const categories = (await (await request.get("/api/public/marketplace/categories")).json()) as { id: string; name: string }[];
  const salon = categories.find((category) => category.name === "Salon & Spa");
  expect(salon, "the Salon & Spa category (V22) exists").toBeTruthy();

  const registered = await request.post("/api/auth/register", {
    data: { businessName: `Glow Requests ${suffix}`, stateCode: "27", ownerName: "Test Owner", email: `salon-req-${suffix}@test.com`, password: "password1234" },
  });
  expect(registered.status()).toBe(201);
  const { token, tenantId } = await registered.json();
  const vendor = { Authorization: `Bearer ${token}` };

  expect((await request.put("/api/marketplace/listing", { headers: vendor, data: { categoryId: salon!.id, area: "Kothrud", enquiryWhatsapp: "9876543210", orderMode: "WHATSAPP" } })).status()).toBe(200);
  const photo = await request.post("/api/marketplace/listing/photo", {
    headers: vendor,
    multipart: { file: { name: "shopfront.png", mimeType: "image/png", buffer: solidPng(1600, 900) } },
  });
  expect(photo.status(), "shop photo upload").toBe(200);
  expect((await request.post("/api/marketplace/listing/submit", { headers: vendor })).status()).toBe(200);
  const admin = { Authorization: `Bearer ${(await (await request.post("/api/auth/login", { data: { email: SUPER_ADMIN.email, password: SUPER_ADMIN.password } })).json()).token}` };
  expect((await request.post(`/api/admin/marketplace/listings/${tenantId}/approve`, { headers: admin })).status()).toBe(200);

  const product = await request.post("/api/products", {
    headers: vendor,
    data: {
      name: "Haircut",
      defaultGstRate: 0,
      defaultUom: "PCS",
      variants: [{ sku: `HAIRCUT-MEN-${suffix}`, attributes: { Type: "Men" }, purchasePrice: 0, sellingPrice: 300 }],
    },
  });
  expect(product.status()).toBe(201);
  const variantId = (await product.json()).variants[0].id as string;

  const put = async (url: string, data: unknown) => expect((await request.put(url, { headers: vendor, data })).status(), url).toBe(200);
  await put("/api/appointments/settings", { slotMinutes: 30 });
  await put("/api/appointments/hours", { ranges: [1, 2, 3, 4, 5, 6, 7].map((dayOfWeek) => ({ dayOfWeek, opensAt: "10:00", closesAt: "12:00" })) });
  expect((await request.post("/api/appointments/open-days/publish-next", { headers: vendor, data: { days: 14 } })).status()).toBe(200);

  // ---- Helpers ----
  const requestsUrl = `/api/public/appointments/${tenantId}/requests`;
  const ask = (phone: string, date: string, time: string, extra: Record<string, unknown> = {}) =>
    request.post(requestsUrl, { data: { variantId, date, time, name: "Shopper", phone, note: "first visit", ...extra } });
  const refusedWith = async (response: Awaited<ReturnType<typeof ask>>, status: number, message: string) => {
    expect(response.status()).toBe(status);
    expect((await response.json()).message).toContain(message);
  };
  const freeTimes = async (date: string): Promise<string[]> =>
    ((await (await request.get(`/api/public/appointments/${tenantId}/availability`)).json()).days as { date: string; times: string[] }[]).find((day) => day.date === date)?.times ?? [];
  const inbox = async (): Promise<InboxRow[]> => (await request.get("/api/appointments/requests", { headers: vendor })).json();
  const decide = (id: string, action: "approve" | "reject" | "cancel", headers = vendor) =>
    request.post(`/api/appointments/requests/${id}/${action}`, { headers });

  const day = indiaDate(3); // far enough ahead that the two-hour notice never matters

  // ---- Two shoppers ask for the same time: both are accepted, nothing is held ----
  const first = await ask(customerA, day, "10:30");
  expect(first.status()).toBe(201);
  expect(await first.json()).toEqual({ serviceName: "Haircut (Men)", date: day, time: "10:30", slotMinutes: 30 });
  expect((await ask(customerB, day, "10:30")).status()).toBe(201);
  expect(await freeTimes(day), "waiting requests block nothing").toEqual(["10:00", "10:30", "11:00", "11:30"]);

  // ---- The business sees both side by side ----
  const waiting = (await inbox()).filter((row) => row.status === "PENDING");
  expect(waiting).toHaveLength(2);
  for (const row of waiting) {
    expect(row.date).toBe(day);
    expect(row.time).toBe("10:30");
    expect(row.serviceName).toBe("Haircut (Men)");
    expect(row.othersAtSameTime).toBe(1);
    expect(row.timeAlreadyBooked).toBe(false);
  }
  expect(waiting.map((row) => row.customerPhone).sort()).toEqual(["9123456780", "9876543210"]);
  expect((await (await request.get("/api/appointments/requests/pending-count", { headers: vendor })).json()).pending).toBe(2);
  const requestA = waiting.find((row) => row.customerPhone === "9876543210")!;
  const requestB = waiting.find((row) => row.customerPhone === "9123456780")!;

  // ---- Another business cannot see or decide on it ----
  const otherVendor = { Authorization: `Bearer ${(await (await request.post("/api/auth/login", { data: { email: TEST_USER.email, password: TEST_USER.password } })).json()).token}` };
  expect((await decide(requestA.id, "approve", otherVendor)).status()).toBe(404);

  // ---- Approving one makes approving the other impossible ----
  const approved = await decide(requestA.id, "approve");
  expect(approved.status()).toBe(200);
  expect((await approved.json()).status).toBe("APPROVED");
  await refusedWith(await decide(requestB.id, "approve"), 422, "already booked");
  expect((await inbox()).find((row) => row.id === requestB.id)).toMatchObject({ status: "PENDING", timeAlreadyBooked: true });

  // The approved time has left the list, and nobody else can take it.
  expect(await freeTimes(day)).toEqual(["10:00", "11:00", "11:30"]);
  await refusedWith(await ask(customerC, day, "10:30"), 422, "no longer available");

  // The other request is rejected instead, once; deciding it again is refused.
  expect((await (await decide(requestB.id, "reject")).json()).status).toBe("REJECTED");
  await refusedWith(await decide(requestB.id, "reject"), 422, "Only a waiting request");
  await refusedWith(await decide(requestB.id, "approve"), 422, "Only a waiting request");

  // ---- One phone: no repeat of the same time, and at most three waiting ----
  expect((await ask(customerA, day, "11:00")).status()).toBe(201);
  await refusedWith(await ask(customerA, day, "11:00"), 422, "already requested this time");
  expect((await ask(customerA, day, "11:30")).status()).toBe(201);
  expect((await ask(customerA, indiaDate(4), "10:00")).status()).toBe(201);
  await refusedWith(await ask(customerA, indiaDate(4), "10:30"), 422, "already have 3 requests waiting");
  expect((await ask(customerB, indiaDate(4), "10:30")).status(), "the limit is per phone number").toBe(201);

  // ---- Cancelling the approved appointment brings its time back ----
  expect((await (await decide(requestA.id, "cancel")).json()).status).toBe("CANCELLED");
  expect(await freeTimes(day)).toEqual(["10:00", "10:30", "11:00", "11:30"]);
  await refusedWith(await decide(requestA.id, "cancel"), 422, "Only an approved appointment");

  // ---- Bad input and unavailable times ----
  await refusedWith(await ask("12345", indiaDate(5), "10:00"), 422, "valid 10-digit mobile number");
  await refusedWith(await ask(customerC, indiaDate(5), "12:00"), 422, "no longer available"); // 12:00 would run past closing
  await refusedWith(await ask(customerC, indiaDate(5), "10:15"), 422, "no longer available"); // off the 30-minute grid
  await refusedWith(await ask(customerC, indiaDate(20), "10:00"), 422, "no longer available"); // beyond the 14-day window
  await refusedWith(await ask(customerC, indiaDate(5), "10:00", { variantId: crypto.randomUUID() }), 422, "not available");
  expect((await ask(customerC, indiaDate(5), "10am")).status()).toBe(400);
  expect((await ask(customerC, indiaDate(5), "10:00", { name: "" })).status()).toBe(400);
  expect((await ask(customerC, indiaDate(5), "10:00", { note: "x".repeat(201) })).status()).toBe(400);

  await put(`/api/appointments/open-days/${indiaDate(6)}`, { open: false });
  await refusedWith(await ask(customerC, indiaDate(6), "10:00"), 422, "no longer available"); // an unpublished date

  // ---- A shop that cannot take appointments is refused outright ----
  const groceryTenantId = (await (await request.post("/api/auth/login", { data: { email: TEST_USER.email, password: TEST_USER.password } })).json()).tenantId;
  const response = await request.post(`/api/public/appointments/${groceryTenantId}/requests`, {
    data: { variantId, date: indiaDate(5), time: "10:00", name: "Shopper", phone: customerC },
  });
  expect(response.status()).toBe(404);
});
