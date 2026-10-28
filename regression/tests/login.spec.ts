import { test, expect } from "@playwright/test";
import { loginAsTestUser } from "./helpers/login";

test("standing test user can log in and reach the dashboard", async ({ page }) => {
  await loginAsTestUser(page);

  await expect(page).toHaveURL("/");
  await expect(page.getByText("veera", { exact: false })).toBeVisible();
  await expect(page.getByText("OWNER")).toBeVisible();
});
