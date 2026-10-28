import { expect, type Page } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../../config";
import { applyForVendorAccount } from "./vendorApplication";
import { loginAsSuperAdmin } from "./adminLogin";

export interface LiveBusiness {
  email: string;
  password: string;
  businessName: string;
}

/** The marketplace category to register under and the one starter product to pick at registration. */
export interface StarterPick {
  category: string;
  group: string;
  product: string;
}

const LADIES_WEAR_PICK: StarterPick = { category: "Ladies Wear", group: "Kurtis & Kurta Sets", product: "Biba Anarkali Kurti" };

/**
 * Registers a business of the given type through the apply form (Free plan, one starter product - Ladies Wear's
 * unless another pick is given - WhatsApp enquiries), then has Super Admin activate the account and approve the listing, so it is live. Leaves its
 * data in place, like the other registration specs - the business name carries a timestamp, so that is safe.
 */
export async function registerLiveBusiness(
  page: Page,
  businessName: string,
  businessType: "RETAILER" | "WHOLESALER",
  pick: StarterPick = LADIES_WEAR_PICK,
): Promise<LiveBusiness> {
  const suffix = businessName.split(" ").pop();
  const email = `${businessType.toLowerCase()}-${suffix}-${Math.random().toString(36).slice(2, 6)}@test.com`;
  const password = "password1234";
  await applyForVendorAccount(page, {
    businessName,
    ownerName: "Test Owner",
    email,
    password,
    businessType,
    businessCategory: pick.category,
  });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await page.getByRole("button", { name: pick.group, exact: true }).click();
  await page.getByRole("checkbox", { name: pick.product }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({
    timeout: 15_000,
  });

  await loginAsSuperAdmin(page);
  await page.getByRole("button", { name: businessName }).click();
  await page.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();
  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
  await page.locator("#status-filter").selectOption("PENDING");
  const listingRow = page.getByRole("row", { name: new RegExp(businessName) });
  await expect(listingRow).toBeVisible();
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  return { email, password, businessName };
}
