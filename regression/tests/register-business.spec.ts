import { test, expect } from "@playwright/test";
import { deleteTenantByEmail } from "./db";

const PASSWORD = "password1234";
const OWNER_NAME = "Veera";

test.describe("Register new business - happy path", () => {
  const email = "veera@example.com";

  // Defensive: clean slate even if a previous run crashed mid-test and
  // never reached its own teardown.
  test.beforeEach(() => deleteTenantByEmail(email));
  test.afterEach(() => deleteTenantByEmail(email));

  test("owner can register a business and lands on the dashboard", async ({ page }) => {
    await page.goto("/register");

    await page.getByLabel("Business name").fill("Playwright Test Traders");
    await page.getByLabel("GST state code").fill("27");
    await page.getByLabel("Your name").fill(OWNER_NAME);
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password", { exact: true }).fill(PASSWORD);

    await page.getByRole("button", { name: "Create Account" }).click();

    // A successful register logs the owner straight in and lands on the
    // protected app shell - the dashboard route ("/") with the business name
    // visible in the topbar is the clearest signal that registration,
    // login, and the JWT round-trip all worked.
    await expect(page).toHaveURL("/");
    await expect(page.getByText("Playwright Test Traders")).toBeVisible();
    await expect(page.getByText("OWNER")).toBeVisible();
  });
});

test.describe("Register new business - duplicate email", () => {
  const email = "veera.duplicate@example.com";

  test.beforeEach(() => deleteTenantByEmail(email));
  test.afterEach(() => deleteTenantByEmail(email));

  test("registering with an already-used business email is rejected", async ({ page }) => {
    async function registerOnce() {
      await page.goto("/register");
      await page.getByLabel("Business name").fill("Duplicate Traders");
      await page.getByLabel("GST state code").fill("27");
      await page.getByLabel("Your name").fill(OWNER_NAME);
      await page.getByLabel("Email").fill(email);
      await page.getByLabel("Password", { exact: true }).fill(PASSWORD);
      await page.getByRole("button", { name: "Create Account" }).click();
    }

    await registerOnce();
    await expect(page).toHaveURL("/");

    // Sign out so the second registration attempt starts from a clean session.
    await page.getByTestId("user-menu-trigger").click();
    await page.getByText("Sign Out").click();
    await expect(page).toHaveURL("/login");

    await registerOnce();
    await expect(page).toHaveURL("/register");
    await expect(page.getByText(/already registered/i)).toBeVisible();
  });
});
