import { test, expect, type APIRequestContext } from "@playwright/test";
import { TEST_USER } from "../config";

// An API-level spec - no browser. It talks to the backend directly (BACKEND_URL overrides the local default), so it
// needs the backend running with the V23 migration applied.
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
test.use({ baseURL: BACKEND_URL });

interface OpenDay {
  date: string;
  open: boolean;
  weekdayHasHours: boolean;
}
interface Range {
  dayOfWeek: number;
  opensAt: string;
  closesAt: string;
}

/** Today's date (YYYY-MM-DD) in India, the time zone appointment booking runs on, plus a number of days. */
function indiaDate(plusDays = 0): string {
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + plusDays);
  return date.toISOString().slice(0, 10);
}

/** ISO weekday of a YYYY-MM-DD date: 1 = Monday ... 7 = Sunday. */
function isoWeekday(date: string): number {
  return ((new Date(`${date}T00:00:00Z`).getUTCDay() + 6) % 7) + 1;
}

/**
 * A business sets up its appointment availability: the appointment length, the weekly opening hours and the dates it
 * publishes. Checks the rules the backend enforces (allowed lengths, no overlapping or too-short ranges, the 14-day
 * window, "publish next days" opening only weekdays that have hours) and that everything reads back as saved.
 *
 * Runs as the standing test vendor and puts its original setup back in a finally block, so the account ends up as it
 * started (apart from a stored appointment length, which is harmless).
 */
test("a business sets up appointment length, weekly hours and published dates, and the backend enforces the rules", async ({
  request,
}) => {
  test.setTimeout(60_000);

  const login = await request.post("/api/auth/login", { data: { email: TEST_USER.email, password: TEST_USER.password } });
  expect(login.ok()).toBe(true);
  const headers = { Authorization: `Bearer ${(await login.json()).token}` };

  const get = async (path: string) => (await request.get(`/api/appointments/${path}`, { headers })).json();
  const putOk = async (path: string, data: unknown) => {
    const response = await request.put(`/api/appointments/${path}`, { headers, data });
    expect(response.status(), `PUT ${path}`).toBe(200);
    return response.json();
  };
  const days = async (): Promise<OpenDay[]> => (await get("open-days")).days;

  // Not signed in: refused.
  expect([401, 403]).toContain((await request.get("/api/appointments/settings")).status());

  const original = { slot: (await get("settings")).slotMinutes, hours: (await get("hours")).ranges as Range[], days: await days() };
  try {
    // ---- Appointment length: only the agreed lengths ----
    const badLength = await request.put("/api/appointments/settings", { headers, data: { slotMinutes: 25 } });
    expect(badLength.status()).toBe(422);
    expect((await badLength.json()).message).toContain("must be one of");
    expect((await request.put("/api/appointments/settings", { headers, data: {} })).status()).toBe(400);

    expect((await putOk("settings", { slotMinutes: 20 })).slotMinutes).toBe(20);
    const settings = await get("settings");
    expect(settings.slotMinutes).toBe(20);
    expect(settings.allowedSlotMinutes).toEqual([10, 15, 20, 30, 45, 60, 90, 120]);

    // ---- No weekly hours yet: nothing to publish from ----
    await putOk("hours", { ranges: [] });
    expect((await get("hours")).ranges).toEqual([]);
    const noHours = await request.post("/api/appointments/open-days/publish-next", { headers, data: { days: 7 } });
    expect(noHours.status()).toBe(422);
    expect((await noHours.json()).message).toBe("Set your weekly opening hours first.");

    // ---- Weekly hours: rules, then a split shift on Monday and mornings Tuesday to Friday ----
    const rejected = async (ranges: Range[], message: string) => {
      const response = await request.put("/api/appointments/hours", { headers, data: { ranges } });
      expect(response.status()).toBe(422);
      expect((await response.json()).message).toContain(message);
    };
    await rejected([{ dayOfWeek: 1, opensAt: "13:00", closesAt: "10:00" }], "Monday: the opening time must be before the closing time");
    await rejected(
      [{ dayOfWeek: 3, opensAt: "10:00", closesAt: "13:00" }, { dayOfWeek: 3, opensAt: "12:30", closesAt: "15:00" }],
      "Wednesday: opening ranges must not overlap",
    );
    await rejected([{ dayOfWeek: 4, opensAt: "10:00", closesAt: "10:10" }], "at least one appointment (20 minutes) long");

    const week: Range[] = [
      { dayOfWeek: 1, opensAt: "10:00", closesAt: "13:00" },
      { dayOfWeek: 1, opensAt: "17:00", closesAt: "20:00" },
      ...[2, 3, 4, 5].map((dayOfWeek) => ({ dayOfWeek, opensAt: "10:00", closesAt: "13:00" })),
    ];
    // Sent out of order on purpose: the week comes back ordered by day, then opening time.
    await putOk("hours", { ranges: [...week].reverse() });
    expect((await get("hours")).ranges).toEqual(week);

    // ---- Published dates: start clean, then "publish the next 7 days" opens Monday to Friday only ----
    for (const day of await days()) if (day.open) await putOk(`open-days/${day.date}`, { open: false });
    expect((await days()).filter((day) => day.open)).toEqual([]);

    const published: OpenDay[] = (await (await request.post("/api/appointments/open-days/publish-next", { headers, data: { days: 7 } })).json()).days;
    expect(published).toHaveLength(14);
    expect(published[0].date).toBe(indiaDate());
    expect(published[13].date).toBe(indiaDate(13));
    const expectedOpen = Array.from({ length: 7 }, (_, i) => indiaDate(i)).filter((date) => isoWeekday(date) <= 5);
    expect(published.filter((day) => day.open).map((day) => day.date)).toEqual(expectedOpen);
    for (const day of published) expect(day.weekdayHasHours).toBe(isoWeekday(day.date) <= 5);
    expect((await days()).filter((day) => day.open).map((day) => day.date)).toEqual(expectedOpen);

    // ---- One date at a time: unpublish and publish again; only the 14-day window can be published ----
    const holiday = expectedOpen[0];
    expect((await putOk(`open-days/${holiday}`, { open: false })).open).toBe(false);
    expect((await days()).find((day) => day.date === holiday)?.open).toBe(false);
    expect((await putOk(`open-days/${holiday}`, { open: true })).open).toBe(true);

    for (const outside of [indiaDate(-1), indiaDate(14)]) {
      const response = await request.put(`/api/appointments/open-days/${outside}`, { headers, data: { open: true } });
      expect(response.status(), outside).toBe(422);
    }
    expect((await request.put("/api/appointments/open-days/not-a-date", { headers, data: { open: true } })).status()).toBe(400);
    for (const bad of [0, 15]) {
      expect((await request.post("/api/appointments/open-days/publish-next", { headers, data: { days: bad } })).status()).toBe(400);
    }
  } finally {
    await request.put("/api/appointments/settings", { headers, data: { slotMinutes: original.slot } });
    await request.put("/api/appointments/hours", { headers, data: { ranges: original.hours } });
    const current = (await (await request.get("/api/appointments/open-days", { headers })).json()).days as OpenDay[];
    for (const before of original.days) {
      if (current.find((day) => day.date === before.date)?.open !== before.open) {
        await request.put(`/api/appointments/open-days/${before.date}`, { headers, data: { open: before.open } });
      }
    }
  }
});
