import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { adminHeaders, createTestArea } from "./helpers/partnerHelpers";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

/**
 * A local partner's whole life: apply on marketplace-web, cannot sign in until Super Admin approves, then signs in to
 * the dashboard, which shows the review screens and their profile but none of the Super Admin-only ones, and cannot sign in
 * again once Super Admin deactivates them. The partner covers a brand-new area, so no other partner is affected.
 * Leaves its data in place (the name carries a timestamp); the partner is left deactivated.
 */
test("a partner applies, is approved, signs in to the shared review screens and is switched off again", async ({ page, browser, request }) => {
  const suffix = timestampSuffix();
  const areaName = `Partner Spec Area ${suffix}`;
  const partnerName = `Spec Partner ${suffix}`;
  const email = `spec-partner-${suffix}@test.com`;
  const password = "password1234";
  await createTestArea(request, await adminHeaders(request), areaName);

  await page.goto("/partners/apply");
  await page.getByLabel("Your name").fill(partnerName);
  await page.getByLabel("WhatsApp number").fill(`9${suffix.slice(-8)}1`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password (min. 8 characters)").fill(password);
  await page.getByRole("combobox", { name: "State *" }).selectOption("27");
  const citySelect = page.getByRole("combobox", { name: "City *" });
  await citySelect.selectOption(await citySelect.locator("option", { hasText: "Pune" }).getAttribute("value") as string);
  await page.getByRole("checkbox", { name: new RegExp(areaName) }).check();
  await page.getByRole("button", { name: "Send application" }).click();
  await expect(page.getByRole("heading", { name: "Application received" })).toBeVisible();

  // Not approved yet: the dashboard refuses the sign-in.
  const partnerPage = await (await browser.newContext()).newPage();
  async function signInAsPartner() {
    await partnerPage.goto(`${FRONTEND_WEB_URL}/login`);
    await partnerPage.getByLabel("Email").fill(email);
    await partnerPage.getByLabel("Password", { exact: true }).fill(password);
    await partnerPage.getByRole("button", { name: "Sign In" }).click();
  }
  await signInAsPartner();
  await expect(partnerPage.locator(".auth-error")).toBeVisible();
  await expect(partnerPage).toHaveURL(`${FRONTEND_WEB_URL}/login`);

  // Super Admin approves it.
  await loginAsSuperAdmin(page);
  await page.goto(`${FRONTEND_WEB_URL}/admin/partner-applications`);
  const applicationRow = page.getByRole("row", { name: new RegExp(partnerName) });
  await expect(applicationRow).toContainText(areaName);
  await applicationRow.getByRole("button", { name: "Approve" }).click();
  await expect(applicationRow).toBeHidden();

  // Signed in, the partner lands on the Vendors list, with the three review screens and their profile in the menu.
  await signInAsPartner();
  await partnerPage.waitForURL(`${FRONTEND_WEB_URL}/admin`);
  await expect(partnerPage.locator(".sidebar-item")).toHaveText(["Vendors", "Marketplace Listings", "Feed Posts", "My profile"]);
  await partnerPage.getByRole("button", { name: "My profile" }).click();
  await expect(partnerPage.getByText(partnerName)).toBeVisible();
  await expect(partnerPage.getByText(areaName)).toBeVisible();
  await expect(partnerPage.getByTestId("partner-shop-count")).toContainText("0");
  // A Super Admin-only screen sends the partner back to the Vendors list.
  await partnerPage.goto(`${FRONTEND_WEB_URL}/admin/partners`);
  await expect(partnerPage).toHaveURL(`${FRONTEND_WEB_URL}/admin`);

  // Super Admin switches the partner off and the partner is locked out.
  await page.goto(`${FRONTEND_WEB_URL}/admin/partners`);
  const partnerRow = page.getByRole("row", { name: new RegExp(partnerName) });
  await expect(partnerRow).toContainText("ACTIVE");
  await partnerRow.getByRole("button", { name: "Deactivate" }).click();
  await expect(partnerRow).toContainText("INACTIVE");
  await partnerPage.context().clearCookies();
  await partnerPage.evaluate(() => localStorage.clear());
  await signInAsPartner();
  await expect(partnerPage.locator(".auth-error")).toBeVisible();
});
