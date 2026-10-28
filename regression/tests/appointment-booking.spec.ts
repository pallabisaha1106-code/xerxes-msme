import { test, expect, type APIRequestContext } from "@playwright/test";
import { SUPER_ADMIN } from "../config";
import { solidPng } from "./helpers/solidPng";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the slowMo/headless rationale. Shops are set
// up through the API so the shopper's journey on the shop page is the only UI under test.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** A date (YYYY-MM-DD) in India, the time zone appointment booking runs on, a number of days from today. */
function indiaDate(plusDays = 0): string {
  const today = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + plusDays);
  return date.toISOString().slice(0, 10);
}

/** "Wed 21 Oct" - how the dialog writes a date. */
function dateChip(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return `${WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()]} ${day} ${MONTHS[month - 1]}`;
}

interface Salon {
  tenantId: string;
  businessName: string;
  vendor: { Authorization: string };
  variantId: string;
}

/** A live Salon & Spa shop with one service; with {publish}, 10:00-12:00 every day for the next 14 days (30-minute slots). */
async function createSalon(request: APIRequestContext, suffix: string, label: string, publish: boolean): Promise<Salon> {
  const categories = (await (await request.get("/api/public/marketplace/categories")).json()) as { id: string; name: string }[];
  const salonCategory = categories.find((category) => category.name === "Salon & Spa");
  expect(salonCategory, "the Salon & Spa category (V22) exists").toBeTruthy();

  const businessName = `Glow Booking ${label} ${suffix}`;
  const registered = await request.post("/api/auth/register", {
    data: { businessName, stateCode: "27", ownerName: "Test Owner", email: `salon-book-${label.toLowerCase()}-${suffix}@test.com`, password: "password1234" },
  });
  expect(registered.status()).toBe(201);
  const { token, tenantId } = await registered.json();
  const vendor = { Authorization: `Bearer ${token}` };

  expect((await request.put("/api/marketplace/listing", { headers: vendor, data: { categoryId: salonCategory!.id, area: "Kothrud", enquiryWhatsapp: "9876543210", orderMode: "WHATSAPP" } })).status()).toBe(200);
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
    data: { name: "Haircut", defaultGstRate: 0, defaultUom: "PCS", variants: [{ sku: `HAIRCUT-${label.toUpperCase()}-${suffix}`, attributes: { Type: "Men" }, purchasePrice: 0, sellingPrice: 300 }] },
  });
  expect(product.status()).toBe(201);
  const variantId = (await product.json()).variants[0].id as string;

  if (publish) {
    const put = async (url: string, data: unknown) => expect((await request.put(url, { headers: vendor, data })).status(), url).toBe(200);
    await put("/api/appointments/settings", { slotMinutes: 30 });
    await put("/api/appointments/hours", { ranges: [1, 2, 3, 4, 5, 6, 7].map((dayOfWeek) => ({ dayOfWeek, opensAt: "10:00", closesAt: "12:00" })) });
    expect((await request.post("/api/appointments/open-days/publish-next", { headers: vendor, data: { days: 14 } })).status()).toBe(200);
  }
  return { tenantId, businessName, vendor, variantId };
}

/**
 * The shopper's side of appointment booking. On a bookable shop's page each service has "Request a slot" and nothing
 * for carts, enquiries or stock. In the dialog the shopper picks a date and a time, gets a clear message for a bad
 * phone number, sends the request and is told the business will confirm by WhatsApp or a call. A second shopper can
 * still ask for the same time (nothing is held); once the vendor approves one, that time is gone from the dialog. If a
 * time is taken while a shopper is filling in their details, sending shows the message and the times that are left.
 * A bookable shop with nothing published says so and offers WhatsApp instead.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a shopper books an appointment on a salon's shop page, and times taken meanwhile are handled", async ({ page, request }) => {
  test.setTimeout(240_000);

  const suffix = timestampSuffix();
  const open = await createSalon(request, suffix, "Open", true);
  const closed = await createSalon(request, suffix, "Closed", false);

  const day = indiaDate(3); // far enough ahead that the two-hour notice never matters
  const dayLabel = dateChip(day);
  const dialog = page.getByRole("dialog", { name: "Request an appointment" });
  const requestSlot = page.getByRole("button", { name: "Request a slot for Haircut (Men)" });

  const inbox = async (status: string) =>
    (await (await request.get(`/api/appointments/requests?status=${status}`, { headers: open.vendor })).json()) as { id: string; customerName: string; customerPhone: string; date: string; time: string }[];

  // ---- A bookable shop's page: services with "Request a slot", and none of the shopping extras ----
  await page.goto(`/shop/${open.tenantId}`);
  await expect(page.getByRole("heading", { name: open.businessName })).toBeVisible();
  await expect(page.getByText("Pick a service and request a time that suits you.")).toBeVisible();
  await expect(requestSlot).toBeVisible();
  await expect(page.getByRole("button", { name: /Add .* to cart/ })).toHaveCount(0);
  await expect(page.getByRole("link", { name: /Enquire with/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /^Cart/ })).toHaveCount(0);
  await expect(page.getByText("Out of stock")).toHaveCount(0);

  // ---- The first shopper: date, time, a bad phone number, then the request ----
  await requestSlot.click();
  await expect(dialog.getByText("Haircut (Men)", { exact: true })).toBeVisible();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await expect(dialog.getByRole("button", { name: "10:30 am" })).toBeVisible();
  await dialog.getByRole("button", { name: "10:30 am" }).click();
  await expect(dialog.getByText(`${dayLabel} at 10:30 am`)).toBeVisible();

  await dialog.getByLabel("Your name").fill("Asha Rao");
  await dialog.getByLabel("Mobile number").fill("12345");
  await dialog.getByRole("button", { name: "Send request" }).click();
  await expect(dialog.getByText("Enter a valid 10-digit mobile number.")).toBeVisible();

  await dialog.getByLabel("Mobile number").fill("98765 43210");
  await dialog.getByLabel("Note (optional)").fill("first visit");
  await dialog.getByRole("button", { name: "Send request" }).click();
  await expect(dialog.getByText("Request sent!")).toBeVisible();
  await expect(dialog.getByText(new RegExp(`${open.businessName} will confirm your appointment for Haircut \\(Men\\) on ${dayLabel} at 10:30 am by WhatsApp or a call on 98765 43210`))).toBeVisible();
  await expect(dialog.getByText("Nothing is booked until they confirm.")).toBeVisible();
  await dialog.getByRole("button", { name: "Done" }).click();
  await expect(dialog).toBeHidden();

  // The vendor has it, waiting.
  const waiting = await inbox("PENDING");
  expect(waiting).toHaveLength(1);
  expect(waiting[0]).toMatchObject({ customerName: "Asha Rao", customerPhone: "9876543210", date: day, time: "10:30" });

  // ---- A second shopper can still ask for the same time: a waiting request holds nothing ----
  await requestSlot.click();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await expect(dialog.getByRole("button", { name: "10:30 am" })).toBeVisible();
  await dialog.getByRole("button", { name: "10:30 am" }).click();
  await dialog.getByLabel("Your name").fill("Bina Shah");
  await dialog.getByLabel("Mobile number").fill("9123456780");
  await dialog.getByRole("button", { name: "Send request" }).click();
  await expect(dialog.getByText("Request sent!")).toBeVisible();
  await dialog.getByRole("button", { name: "Done" }).click();
  expect(await inbox("PENDING")).toHaveLength(2);

  // ---- The vendor approves Asha; that time is gone for the next shopper ----
  const asha = (await inbox("PENDING")).find((row) => row.customerName === "Asha Rao")!;
  expect((await request.post(`/api/appointments/requests/${asha.id}/approve`, { headers: open.vendor })).status()).toBe(200);
  await requestSlot.click();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await expect(dialog.getByRole("button", { name: "10:00 am" })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "11:00 am" })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "10:30 am" })).toHaveCount(0);

  // ---- A time is taken while a shopper is filling in their details ----
  await dialog.getByRole("button", { name: "11:00 am" }).click();
  await dialog.getByLabel("Your name").fill("Chitra Iyer");
  await dialog.getByLabel("Mobile number").fill("9988776655");
  const other = await request.post(`/api/public/appointments/${open.tenantId}/requests`, {
    data: { variantId: open.variantId, date: day, time: "11:00", name: "Someone Else", phone: "9000000001" },
  });
  expect(other.status()).toBe(201);
  const taken = (await inbox("PENDING")).find((row) => row.customerName === "Someone Else")!;
  expect((await request.post(`/api/appointments/requests/${taken.id}/approve`, { headers: open.vendor })).status()).toBe(200);

  await dialog.getByRole("button", { name: "Send request" }).click();
  await expect(dialog.getByText(/no longer available/)).toBeVisible();
  await expect(dialog.getByRole("button", { name: "11:00 am" })).toHaveCount(0);
  await expect(dialog.getByRole("button", { name: "11:30 am" })).toBeVisible();
  await dialog.getByRole("button", { name: "11:30 am" }).click();
  await dialog.getByRole("button", { name: "Send request" }).click(); // the details were kept
  await expect(dialog.getByText("Request sent!")).toBeVisible();
  await dialog.getByRole("button", { name: "Done" }).click();

  // ---- A bookable shop with nothing published says so, and offers WhatsApp ----
  await page.goto(`/shop/${closed.tenantId}`);
  await expect(page.getByRole("heading", { name: closed.businessName })).toBeVisible();
  await requestSlot.click();
  await expect(dialog.getByText("No appointment times are open right now.")).toBeVisible();
  await expect(dialog.getByRole("link", { name: /WhatsApp us/ })).toBeVisible();
});
