import path from "node:path";
import { fileURLToPath } from "node:url";
import type { Page } from "@playwright/test";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
export const FIXTURES_DIR = path.resolve(__dirname, "../../fixtures");

export interface VendorApplicationInput {
  businessName: string;
  ownerName: string;
  email: string;
  password: string;
  /** Defaults to "RETAILER", the form's own default. */
  businessType?: "RETAILER" | "WHOLESALER";
  stateCode?: string;
  /** Text (or substring) of the marketplace-category option to pick - defaults to "Grocery". */
  businessCategory?: string;
  /** Text of the service-city option to pick - defaults to "Pune" (seeded by V13__service_locations.sql). */
  city?: string;
  /** Text of the service-area option to pick under that city - defaults to "Kothrud" (seeded, pincode 411038). */
  area?: string;
}

/**
 * Drives marketplace-web's Step 1 apply form (VendorApplyPage) to completion
 * and returns the registration code shown on the confirmation screen. Shared
 * by every spec that needs a fresh pending application to work with, rather
 * than each one re-typing the same form fill.
 */
export async function applyForVendorAccount(page: Page, input: VendorApplicationInput): Promise<string> {
  await page.goto("/list-your-business/apply");

  await page.getByLabel("Business name").fill(input.businessName);

  if (input.businessType === "WHOLESALER") await page.getByRole("radio", { name: /Wholesaler/ }).check();

  const categorySelect = page.getByLabel("Business category");
  const categoryValue = await categorySelect
    .locator("option", { hasText: input.businessCategory ?? "Grocery" })
    .getAttribute("value");
  await categorySelect.selectOption(categoryValue!);

  await page.getByLabel("GST state").selectOption(input.stateCode ?? "27");

  // City/Area are cascading and gated on Super Admin's curated list (see
  // AdminServiceLocationService) - City only populates/enables once a state
  // is chosen, Area only once a city is chosen. selectOption() already waits
  // for the target to be enabled, so no manual wait is needed here.
  const citySelect = page.getByLabel("City *", { exact: true });
  const cityValue = await citySelect.locator("option", { hasText: input.city ?? "Pune" }).getAttribute("value");
  await citySelect.selectOption(cityValue!);

  const areaSelect = page.getByLabel("Area *", { exact: true });
  const areaValue = await areaSelect.locator("option", { hasText: input.area ?? "Kothrud" }).getAttribute("value");
  await areaSelect.selectOption(areaValue!);

  await page.getByLabel("Landmark").fill("Near the bus stand");

  await page.getByLabel("Your name").fill(input.ownerName);
  await page.getByLabel("Email").fill(input.email);
  await page.getByLabel("Password (min. 8 characters)").fill(input.password);

  await page.getByLabel("Shop front photo").setInputFiles(path.join(FIXTURES_DIR, "shop-front-photo.png"));

  // GST certificate/owner ID uploads are hidden entirely when app.vendor-application.kyc-documents-required
  // is off (the current default - see VendorApplicationService) - Super Admin verifies those by phone
  // instead. Fill them only if present, so this helper works whichever way that flag is set.
  const gstCertificateInput = page.getByLabel("Shop ACT or GST certificate");
  if (await gstCertificateInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await gstCertificateInput.setInputFiles(path.join(FIXTURES_DIR, "gst-certificate.png"));
  }
  const ownerIdInput = page.getByLabel("Owner PAN or Aadhaar");
  if (await ownerIdInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await ownerIdInput.setInputFiles(path.join(FIXTURES_DIR, "owner-id.png"));
  }

  await page.getByLabel("I confirm these details are accurate").click();
  await page.getByRole("button", { name: "Submit Application" }).click();
  await page.getByRole("heading", { name: "Application submitted!" }).waitFor();

  return (await page.locator(".registration-code").innerText()).trim();
}
