import type { Page } from "@playwright/test";
import { TEST_USER } from "../../config";

/** Logs in as the standing test account (config.ts) and waits for the dashboard to load. */
export async function loginAsTestUser(page: Page): Promise<void> {
  await page.goto("/login");
  await page.getByLabel("Email").fill(TEST_USER.email);
  await page.getByLabel("Password", { exact: true }).fill(TEST_USER.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL("/");
}
