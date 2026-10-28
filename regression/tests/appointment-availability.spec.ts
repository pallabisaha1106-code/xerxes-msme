import { test, expect } from "@playwright/test";
import { SUPER_ADMIN, TEST_USER } from "../config";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// An API-level spec - no browser. It talks to the backend directly (BACKEND_URL overrides the local default), so it
// needs the backend running with the V22 and V23 migrations applied and the Super Admin password set (see README).
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
test.use({ baseURL: BACKEND_URL });

interface Day {
  date: string;
  times: string[];
}

/** A date (YYYY-MM-DD) in India, the time zone appointment booking runs on, a number of days from today. */
function indiaDate(plusDays = 0): string {
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + plusDays);
  return date.toISOString().slice(0, 10);
}

/** Minutes since midnight in India right now. */
function indiaMinutesNow(): number {
  const [hours, minutes] = new Date().toLocaleTimeString("en-GB", { timeZone: "Asia/Kolkata", hour12: false }).split(":");
  return Number(hours) * 60 + Number(minutes);
}

const minutesOf = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3));

/**
 * What a shopper is shown when picking an appointment, for a real "Salon & Spa" shop built entirely through the API:
 * the vendor registers, lists the shop, Super Admin approves it, and the vendor sets an appointment length, weekly
 * hours and published dates. The public availability endpoint then lists each published date with exactly the right
 * start times - the grid from the opening hours, nothing for unpublished dates, nothing sooner than two hours from
 * now - follows a change of appointment length, and refuses shops that cannot take appointments.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a live Salon & Spa shop shows shoppers its published dates with the right free times, and other shops are refused", async ({
  request,
}) => {
  test.setTimeout(90_000);

  const suffix = timestampSuffix();
  const email = `salon-api-${suffix}@test.com`;
  const password = "password1234";

  // ---- A Salon & Spa vendor registers and lists the shop ----
  const categories = (await (await request.get("/api/public/marketplace/categories")).json()) as { id: string; name: string }[];
  const salonCategory = categories.find((category) => category.name === "Salon & Spa");
  expect(salonCategory, "the Salon & Spa category (V22) exists").toBeTruthy();

  const registered = await request.post("/api/auth/register", {
    data: { businessName: `Glow Salon API ${suffix}`, stateCode: "27", ownerName: "Test Owner", email, password },
  });
  expect(registered.status()).toBe(201);
  const { token, tenantId } = await registered.json();
  const vendor = { Authorization: `Bearer ${token}` };

  const listing = await request.put("/api/marketplace/listing", {
    headers: vendor,
    data: { categoryId: salonCategory!.id, area: "Kothrud", enquiryWhatsapp: "9876543210", orderMode: "WHATSAPP" },
  });
  expect(listing.status()).toBe(200);
  const photo = await request.post("/api/marketplace/listing/photo", {
    headers: vendor,
    // A shop photo must be landscape (about 4:3 to 2:1) and at least 960x480.
    multipart: { file: { name: "shopfront.png", mimeType: "image/png", buffer: solidPng(1600, 900) } },
  });
  expect(photo.status(), "shop photo upload").toBe(200);
  expect((await request.post("/api/marketplace/listing/submit", { headers: vendor })).status()).toBe(200);

  // ---- Super Admin approves it; until then the public side knows nothing ----
  const availabilityUrl = `/api/public/appointments/${tenantId}/availability`;
  expect((await request.get(availabilityUrl)).status(), "a listing under review is not public").toBe(404);

  const adminLogin = await request.post("/api/auth/login", { data: { email: SUPER_ADMIN.email, password: SUPER_ADMIN.password } });
  expect(adminLogin.ok()).toBe(true);
  const admin = { Authorization: `Bearer ${(await adminLogin.json()).token}` };
  expect((await request.post(`/api/admin/marketplace/listings/${tenantId}/approve`, { headers: admin })).status()).toBe(200);

  // ---- Live and bookable, but nothing published yet: an answer with no dates ----
  const beforeSetup = await (await request.get(availabilityUrl)).json();
  expect(beforeSetup).toEqual({ slotMinutes: 30, days: [] });

  // ---- The vendor sets 10:00-12:00 every day and publishes the whole window ----
  const put = async (url: string, data: unknown) => expect((await request.put(url, { headers: vendor, data })).status(), url).toBe(200);
  await put("/api/appointments/settings", { slotMinutes: 30 });
  await put("/api/appointments/hours", {
    ranges: [1, 2, 3, 4, 5, 6, 7].map((dayOfWeek) => ({ dayOfWeek, opensAt: "10:00", closesAt: "12:00" })),
  });
  expect((await request.post("/api/appointments/open-days/publish-next", { headers: vendor, data: { days: 14 } })).status()).toBe(200);

  const fetchDays = async (): Promise<{ slotMinutes: number; days: Day[] }> => (await request.get(availabilityUrl)).json();

  const full = await fetchDays();
  expect(full.slotMinutes).toBe(30);
  const byDate = new Map(full.days.map((day) => [day.date, day.times]));
  // Every date from tomorrow on has the whole grid: 10:00, 10:30, 11:00, 11:30 (12:00 would run past closing).
  for (let offset = 1; offset <= 13; offset++) {
    expect(byDate.get(indiaDate(offset)), indiaDate(offset)).toEqual(["10:00", "10:30", "11:00", "11:30"]);
  }
  // Today keeps only starts at least two hours away - possibly none, in which case today is left out entirely.
  const earliestToday = indiaMinutesNow() + 120;
  const todays = byDate.get(indiaDate(0)) ?? [];
  for (const time of todays) expect(minutesOf(time)).toBeGreaterThanOrEqual(earliestToday);
  expect(todays).toEqual(["10:00", "10:30", "11:00", "11:30"].filter((time) => minutesOf(time) >= earliestToday));
  expect(full.days.map((day) => day.date)).toEqual([...byDate.keys()].sort()); // earliest date first

  // ---- A date the vendor takes off disappears; a longer appointment changes the times ----
  const holiday = indiaDate(2);
  await put(`/api/appointments/open-days/${holiday}`, { open: false });
  expect((await fetchDays()).days.map((day) => day.date)).not.toContain(holiday);

  await put("/api/appointments/settings", { slotMinutes: 45 });
  const forty5 = await fetchDays();
  expect(forty5.slotMinutes).toBe(45);
  expect(forty5.days.find((day) => day.date === indiaDate(3))?.times).toEqual(["10:00", "10:45"]); // 11:30 would end at 12:15

  // ---- Shops that cannot take appointments are refused, and so are unknown ones ----
  const groceryLogin = await request.post("/api/auth/login", { data: { email: TEST_USER.email, password: TEST_USER.password } });
  const groceryTenantId = (await groceryLogin.json()).tenantId;
  expect((await request.get(`/api/public/appointments/${groceryTenantId}/availability`)).status(), "not a bookable shop").toBe(404);
  expect((await request.get(`/api/public/appointments/${crypto.randomUUID()}/availability`)).status(), "unknown shop").toBe(404);
  expect((await request.get("/api/public/appointments/not-a-uuid/availability")).status()).toBe(400);
});
