import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL, SUPER_ADMIN } from "../config";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the slowMo/headless rationale. The shop is set
// up through the API (so the screens are the only UI under test); the vendor then works entirely in frontend-web.
test.use({ headless: false, video: "on", launchOptions: { slowMo: 200 } });

/** A date (YYYY-MM-DD) in India, the time zone appointment booking runs on, a number of days from today. */
function indiaDate(plusDays = 0): string {
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + plusDays);
  return date.toISOString().slice(0, 10);
}

/**
 * The vendor's appointment screens. A Salon & Spa vendor (set up through the API) signs in and uses Appointments >
 * Availability to set the appointment length, weekly hours (with the form's own overlap warning) and published
 * dates ("Publish next 7 days", then one date off and on). Two shoppers then request the same time; in Appointments >
 * Requests the vendor sees both with "1 other asked for this time", the sidebar badge says 2 waiting, and WhatsApp
 * and Call links carry the customer's number and a message for the request's status. Approving one flags the other
 * "TIME ALREADY BOOKED" (its Approve button is off), rejecting it empties the waiting list, and cancelling the approved
 * appointment goes through a confirm dialog and clears the badge.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a salon vendor sets up availability and works the request inbox in the vendor app", async ({ page, request }) => {
  test.setTimeout(240_000);

  const suffix = timestampSuffix();
  const email = `salon-screens-${suffix}@test.com`;
  const password = "password1234";
  const api = (path: string) => `${FRONTEND_WEB_URL}/api${path}`;

  // ---- A live Salon & Spa shop with one service, set up through the API ----
  const categories = (await (await request.get(api("/public/marketplace/categories"))).json()) as { id: string; name: string }[];
  const salon = categories.find((category) => category.name === "Salon & Spa");
  expect(salon, "the Salon & Spa category (V22) exists").toBeTruthy();

  const registered = await request.post(api("/auth/register"), {
    data: { businessName: `Glow Screens ${suffix}`, stateCode: "27", ownerName: "Test Owner", email, password },
  });
  expect(registered.status()).toBe(201);
  const { token, tenantId } = await registered.json();
  const vendor = { Authorization: `Bearer ${token}` };
  expect((await request.put(api("/marketplace/listing"), { headers: vendor, data: { categoryId: salon!.id, area: "Kothrud", enquiryWhatsapp: "9876543210", orderMode: "WHATSAPP" } })).status()).toBe(200);
  const photo = await request.post(api("/marketplace/listing/photo"), {
    headers: vendor,
    multipart: { file: { name: "shopfront.png", mimeType: "image/png", buffer: solidPng(1600, 900) } },
  });
  expect(photo.status(), "shop photo upload").toBe(200);
  expect((await request.post(api("/marketplace/listing/submit"), { headers: vendor })).status()).toBe(200);
  const admin = { Authorization: `Bearer ${(await (await request.post(api("/auth/login"), { data: { email: SUPER_ADMIN.email, password: SUPER_ADMIN.password } })).json()).token}` };
  expect((await request.post(api(`/admin/marketplace/listings/${tenantId}/approve`), { headers: admin })).status()).toBe(200);
  const product = await request.post(api("/products"), {
    headers: vendor,
    data: { name: "Haircut", defaultGstRate: 0, defaultUom: "PCS", variants: [{ sku: `HAIRCUT-${suffix}`, attributes: { Type: "Men" }, purchasePrice: 0, sellingPrice: 300 }] },
  });
  const variantId = (await product.json()).variants[0].id as string;

  // ---- The vendor signs in and finds the new menu section ----
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  await page.getByRole("button", { name: "Appointments" }).click();
  await expect(page.getByRole("button", { name: "Requests" })).toBeVisible();
  await page.getByRole("button", { name: "Availability" }).click();
  await expect(page.getByRole("heading", { name: "Appointment length" })).toBeVisible();

  // ---- Appointment length ----
  await page.getByLabel("Each appointment lasts").selectOption("45");
  await page.getByRole("button", { name: "Save length" }).click();
  await expect(page.getByText("Appointment length saved")).toBeVisible();
  await page.getByLabel("Each appointment lasts").selectOption("30");
  await page.getByRole("button", { name: "Save length" }).click();
  await expect(page.getByLabel("Each appointment lasts")).toHaveValue("30");

  // ---- Weekly hours: a form warning for an overlap, then 10:00-13:00 every day ----
  const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  for (const day of weekdays) await page.getByRole("button", { name: `Add hours for ${day}` }).click();

  await page.getByRole("button", { name: "Add hours for Monday" }).click();
  await page.getByLabel("Monday opens").nth(1).fill("12:00"); // overlaps 10:00-13:00
  await expect(page.getByText("Monday: opening ranges must not overlap.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Save weekly hours" })).toBeDisabled();
  await page.getByRole("button", { name: "Remove Monday hours 2" }).click();
  await expect(page.getByText("Monday: opening ranges must not overlap.")).toHaveCount(0);

  await page.getByRole("button", { name: "Save weekly hours" }).click();
  await expect(page.getByText("Weekly hours saved")).toBeVisible();
  await page.reload();
  await expect(page.getByLabel("Monday opens")).toHaveValue("10:00");
  await expect(page.getByLabel("Sunday closes")).toHaveValue("13:00");

  // ---- Published dates ----
  await page.getByRole("button", { name: "Publish next 7 days" }).click();
  await expect(page.getByText("Opened 7 dates")).toBeVisible();
  await expect(page.getByRole("switch", { checked: true })).toHaveCount(7);
  await page.getByRole("switch").nth(2).uncheck(); // a day off
  await page.reload();
  await expect(page.getByRole("switch", { checked: true })).toHaveCount(6);
  await page.getByRole("switch").nth(2).check();
  await expect(page.getByRole("switch", { checked: true })).toHaveCount(7);

  // ---- Two shoppers ask for the same time (through the public API, as the shop page will) ----
  const day = indiaDate(3);
  const ask = (name: string, phone: string) =>
    request.post(api(`/public/appointments/${tenantId}/requests`), { data: { variantId, date: day, time: "10:30", name, phone, note: "first visit" } });
  expect((await ask("Asha Rao", "98765 43210")).status()).toBe(201);
  expect((await ask("Bina Shah", "9123456780")).status()).toBe(201);

  // ---- The inbox: both waiting, the badge says 2, contact links are ready ----
  await page.getByRole("button", { name: "Requests" }).click();
  await expect(page.getByRole("heading", { name: "Appointment Requests" })).toBeVisible();
  const asha = page.getByRole("row", { name: /Asha Rao/ });
  const bina = page.getByRole("row", { name: /Bina Shah/ });
  await expect(asha).toBeVisible();
  await expect(bina).toBeVisible();
  await expect(asha.getByText("1 other asked for this time")).toBeVisible();
  await expect(bina.getByText("1 other asked for this time")).toBeVisible();
  await expect(asha.getByText("9876543210")).toBeVisible();
  await expect(asha.getByText("Haircut (Men)")).toBeVisible();
  await expect(page.getByLabel("2 waiting").first()).toBeVisible();

  const whatsapp = (await asha.getByRole("link", { name: "WhatsApp Asha Rao" }).getAttribute("href"))!;
  expect(whatsapp).toContain("https://wa.me/919876543210?text=");
  expect(decodeURIComponent(whatsapp)).toContain("Hi Asha Rao, this is Glow Screens");
  await expect(asha.getByRole("link", { name: "Call Asha Rao" })).toHaveAttribute("href", "tel:+919876543210");

  // ---- Approve one: the other is flagged and cannot be approved; the badge drops to 1 ----
  await asha.getByRole("button", { name: "Approve" }).click();
  await expect(page.getByText("Request approved")).toBeVisible();
  await expect(page.getByRole("row", { name: /Asha Rao/ })).toHaveCount(0); // no longer waiting
  await expect(bina.getByText("TIME ALREADY BOOKED")).toBeVisible();
  await expect(bina.getByRole("button", { name: "Approve" })).toBeDisabled();
  await expect(page.getByLabel("1 waiting").first()).toBeVisible();

  // ---- Under Approved, the message is now a confirmation, and there is a Cancel ----
  await page.getByLabel("Show requests").selectOption({ label: "Approved" });
  const approved = page.getByRole("row", { name: /Asha Rao/ });
  await expect(approved.getByText("APPROVED")).toBeVisible();
  expect(decodeURIComponent((await approved.getByRole("link", { name: "WhatsApp Asha Rao" }).getAttribute("href"))!)).toContain("is confirmed");

  // ---- Reject the other: nothing is waiting, the badge is gone ----
  await page.getByLabel("Show requests").selectOption({ label: "Waiting for you" });
  await bina.getByRole("button", { name: "Reject" }).click();
  await expect(page.getByText("Request rejected")).toBeVisible();
  await expect(page.getByText("No requests here.")).toBeVisible();
  await expect(page.getByLabel(/\d+ waiting/)).toHaveCount(0);

  // ---- Cancelling the approved appointment asks first ----
  await page.getByLabel("Show requests").selectOption({ label: "Approved" });
  await page.getByRole("row", { name: /Asha Rao/ }).getByRole("button", { name: "Cancel" }).click();
  const confirm = page.getByRole("dialog", { name: "Cancel this appointment?" });
  await expect(confirm.getByText(/The time becomes available again/)).toBeVisible();
  await confirm.getByRole("button", { name: "Keep it" }).click();
  await expect(page.getByRole("row", { name: /Asha Rao/ }).getByText("APPROVED")).toBeVisible();

  await page.getByRole("row", { name: /Asha Rao/ }).getByRole("button", { name: "Cancel" }).click();
  await page.getByRole("dialog", { name: "Cancel this appointment?" }).getByRole("button", { name: "Cancel appointment" }).click();
  await expect(page.getByText("Appointment cancelled")).toBeVisible();
  await expect(page.getByText("No requests here.")).toBeVisible();

  // The cancelled appointment is still on record under Cancelled, with a cancellation message ready.
  await page.getByLabel("Show requests").selectOption({ label: "Cancelled" });
  const cancelled = page.getByRole("row", { name: /Asha Rao/ });
  await expect(cancelled.getByText("CANCELLED")).toBeVisible();
  expect(decodeURIComponent((await cancelled.getByRole("link", { name: "WhatsApp Asha Rao" }).getAttribute("href"))!)).toContain("we have to cancel your appointment");
});
