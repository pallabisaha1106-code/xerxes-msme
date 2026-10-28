import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the slowMo/headless rationale. This one
// drives everything through the screens, in both apps: marketplace-web (baseURL) and frontend-web (FRONTEND_WEB_URL).
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

/** "Wed 21 Oct" - how the booking dialog writes a date. */
function dateChip(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return `${WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()]} ${day} ${MONTHS[month - 1]}`;
}

/**
 * Appointment booking from the front door to the finish, entirely through the screens. A salon owner applies on
 * marketplace-web under "Salon & Spa" (there is no starter catalogue to pick), Super Admin approves the account and
 * the listing, and the owner signs in to the vendor app, creates a "Haircut (Men)" service as an ordinary product,
 * and under Appointments > Availability saves weekly hours and publishes the next 7 days. A shopper then finds the shop
 * in the marketplace (its card says "Appointments"), requests 10:30 am three days ahead and is told the shop will
 * confirm. Back in the vendor app the request is waiting, the badge says 1, and approving it removes that time from
 * what the next shopper is offered; cancelling the appointment brings the time back.
 *
 * Deliberately leaves its data in place afterward - see grocery-vendor-free-whatsapp.spec.ts for why the timestamp
 * suffix makes that safe.
 */
test("a salon goes from applying to taking an approved appointment, through the screens", async ({ page }) => {
  test.setTimeout(420_000);

  const suffix = timestampSuffix();
  const email = `salon-e2e-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Glow End To End ${suffix}`;
  const day = indiaDate(3); // far enough ahead that the two-hour notice never matters
  const dayLabel = dateChip(day);

  // ---- The owner applies under Salon & Spa: no starter catalogue to pick, just a WhatsApp number ----
  await applyForVendorAccount(page, { businessName, ownerName: "Test Owner", email, password, businessCategory: "Salon & Spa" });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();
  await expect(page.getByRole("checkbox")).toHaveCount(0);
  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({ timeout: 15_000 });

  // ---- Super Admin approves the account and the listing ----
  await loginAsSuperAdmin(page);
  await page.getByRole("button", { name: businessName }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
  await page.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();
  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
  await page.locator("#status-filter").selectOption("PENDING");
  const listingRow = page.getByRole("row", { name: new RegExp(businessName) });
  await expect(listingRow).toBeVisible();
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  // ---- The owner signs in to the vendor app and creates the service as an ordinary product ----
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  await page.goto(`${FRONTEND_WEB_URL}/products/new`);
  await page.getByLabel("Product name").fill("Haircut");
  await page.getByLabel("Default GST rate (%)").fill("0");
  await page.getByLabel("SKU").fill(`HAIRCUT-MEN-${suffix}`);
  await page.getByPlaceholder("Attribute (e.g. Size)").first().fill("Type");
  await page.getByPlaceholder("Value (e.g. M)").first().fill("Men");
  await page.getByLabel("Purchase price").fill("0");
  await page.getByLabel("Selling price").fill("300");
  await page.getByRole("button", { name: "Create Product" }).click();
  await expect(page.getByText("Product created")).toBeVisible();

  // ---- Availability: weekly hours for every day, then the next 7 days published ----
  await page.getByRole("button", { name: "Appointments" }).click();
  await page.getByRole("button", { name: "Availability" }).click();
  await expect(page.getByRole("heading", { name: "Appointment length" })).toBeVisible();
  for (const weekday of ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]) {
    await page.getByRole("button", { name: `Add hours for ${weekday}` }).click();
  }
  await page.getByRole("button", { name: "Save weekly hours" }).click();
  await expect(page.getByText("Weekly hours saved")).toBeVisible();
  await page.getByRole("button", { name: "Publish next 7 days" }).click();
  await expect(page.getByText("Opened 7 dates")).toBeVisible();

  // ---- A shopper finds the shop (its card says Appointments) and requests 10:30 am ----
  await page.goto("/shops");
  // The "Welcome to Neighbourly" dialog only shows on a first visit per browser, so dismiss it only if present.
  const browseAllBusinesses = page.getByRole("button", { name: "Browse all businesses" });
  if (await browseAllBusinesses.isVisible({ timeout: 5_000 }).catch(() => false)) {
    await browseAllBusinesses.click();
  }
  await page.getByLabel("Search shops, products, areas or pincodes").fill(businessName);
  await page.getByRole("button", { name: "Find shops" }).click();
  await expect(page.getByRole("article").filter({ hasText: businessName }).getByText("Appointments", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: new RegExp(`View all products from ${businessName}`) }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
  const shopUrl = page.url();

  const dialog = page.getByRole("dialog", { name: "Request an appointment" });
  const requestSlot = page.getByRole("button", { name: "Request a slot for Haircut (Men)" });
  await requestSlot.click();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await dialog.getByRole("button", { name: "10:30 am" }).click();
  await dialog.getByLabel("Your name").fill("Asha Rao");
  await dialog.getByLabel("Mobile number").fill("98765 43210");
  await dialog.getByRole("button", { name: "Send request" }).click();
  await expect(dialog.getByText(new RegExp(`${businessName} will confirm your appointment for Haircut \\(Men\\) on ${dayLabel} at 10:30 am`))).toBeVisible();
  await dialog.getByRole("button", { name: "Done" }).click();

  // ---- The owner finds it waiting, with the badge, and approves it ----
  await page.goto(`${FRONTEND_WEB_URL}/appointments`);
  const waitingRow = page.getByRole("row", { name: /Asha Rao/ });
  await expect(waitingRow).toBeVisible();
  await expect(waitingRow.getByText("Haircut (Men)")).toBeVisible();
  await expect(waitingRow.getByText("9876543210")).toBeVisible();
  await expect(page.getByLabel("1 waiting").first()).toBeVisible();
  await waitingRow.getByRole("button", { name: "Approve" }).click();
  await expect(page.getByText("Request approved")).toBeVisible();
  await expect(page.getByLabel(/\d+ waiting/)).toHaveCount(0);

  // ---- The approved time is gone for the next shopper ----
  await page.goto(shopUrl);
  await requestSlot.click();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await expect(dialog.getByRole("button", { name: "10:00 am" })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "10:30 am" })).toHaveCount(0);
  await dialog.getByRole("button", { name: "Close" }).click();

  // ---- Cancelling the appointment brings it back ----
  await page.goto(`${FRONTEND_WEB_URL}/appointments`);
  await page.getByLabel("Show requests").selectOption({ label: "Approved" });
  await page.getByRole("row", { name: /Asha Rao/ }).getByRole("button", { name: "Cancel" }).click();
  await page.getByRole("dialog", { name: "Cancel this appointment?" }).getByRole("button", { name: "Cancel appointment" }).click();
  await expect(page.getByText("Appointment cancelled")).toBeVisible();

  await page.goto(shopUrl);
  await requestSlot.click();
  await dialog.getByRole("button", { name: dayLabel, exact: true }).click();
  await expect(dialog.getByRole("button", { name: "10:30 am" })).toBeVisible();
});
