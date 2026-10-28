import type { Page } from "@playwright/test";
import { FRONTEND_WEB_URL, SUPER_ADMIN } from "../../config";

/**
 * Logs in as the standing Super Admin account (config.ts) and waits for the
 * Admin → Vendors page to load. Always navigates by absolute URL to
 * frontend-web's own origin, since callers may be running under a different
 * app's baseURL (e.g. marketplace-web, for a cross-app approval journey).
 */
export async function loginAsSuperAdmin(page: Page): Promise<void> {
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(SUPER_ADMIN.email);
  await page.getByLabel("Password", { exact: true }).fill(SUPER_ADMIN.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/admin`);
}
