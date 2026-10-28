import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";

test.use({ baseURL: "http://localhost:5174" });

/**
 * A new account's email needs a real domain ("name@example.com"). The browser's own type="email" check accepts
 * "test@test", so both registration forms add their own check. Creates no data: neither form is submitted
 * successfully, so there is nothing to clean up and no public-API budget is used.
 */
test("the shop application form refuses an email without a real domain", async ({ page }) => {
  await page.goto("/list-your-business/apply");

  // Skip the browser's own required-field checks so the form's own message appears even with the rest left empty.
  await page.locator("form.join-form").evaluate((form: HTMLFormElement) => {
    form.noValidate = true;
  });

  const problem = page.getByText(/Before submitting, please add:/);

  await page.getByLabel("Email").fill("test@test");
  await page.getByRole("button", { name: "Submit Application" }).click();
  await expect(problem).toContainText("a valid email address");

  await page.getByLabel("Email").fill("owner@example.com");
  await page.getByRole("button", { name: "Submit Application" }).click();
  await expect(problem).not.toContainText("a valid email address");
});

test("the direct sign-up page refuses an email without a real domain", async ({ page }) => {
  await page.goto(`${FRONTEND_WEB_URL}/register`);

  await page.getByLabel("Business name").fill("Email Check Traders");
  await page.getByLabel("GST state code").fill("27");
  await page.getByLabel("Your name").fill("Test Owner");
  await page.getByLabel("Email").fill("test@test");
  await page.getByLabel("Password", { exact: true }).fill("password1234");
  await page.getByRole("button", { name: "Create Account" }).click();

  await expect(page.getByText("Enter a valid email address, like name@example.com.")).toBeVisible();
  await expect(page).toHaveURL(`${FRONTEND_WEB_URL}/register`);
});
