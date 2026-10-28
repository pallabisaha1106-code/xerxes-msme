import { test, expect, type Page } from "@playwright/test";
import { registerLiveBusiness } from "./helpers/liveBusiness";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

/** Opens a listing page ("/shops", "/wholesalers"), browses past the first-visit welcome dialog, and searches for a name. */
async function searchFor(page: Page, path: string, searchLabel: string, buttonLabel: string, name: string) {
  await page.goto(path);
  // The dialog fills in its city list a moment after the page loads, so wait for the button rather than just glance.
  await page.getByRole("button", { name: "Browse all businesses" }).click({ timeout: 10_000 });
  await page.getByLabel(searchLabel).fill(name);
  await page.getByRole("button", { name: buttonLabel }).click();
}

/**
 * Explore Shops lists retailers and Explore Wholesaler lists wholesalers, never a mix. Registers one of each, then
 * checks the public API (with and without the filter) and both pages. Uses the apply form, so like the other
 * registration specs it needs the local backend's public rate limit off (its default for local runs).
 */
test("a retailer shows only under Explore Shops and a wholesaler only under Explore Wholesaler", async ({
  page,
  request,
}) => {
  test.setTimeout(300_000);

  const suffix = timestampSuffix();
  const retailer = `Retail Split Steel Thread ${suffix}`;
  const wholesaler = `Wholesale Split Steel Thread ${suffix}`;
  await registerLiveBusiness(page, retailer, "RETAILER");
  await registerLiveBusiness(page, wholesaler, "WHOLESALER");

  // ---- The public API ----
  const namesFor = async (query: string) =>
    ((await (await request.get(`/api/public/marketplace/listings?size=50${query}`)).json()).items as { businessName: string }[]).map(
      (item) => item.businessName,
    );
  const retailNames = await namesFor("&businessType=RETAILER");
  expect(retailNames).toContain(retailer);
  expect(retailNames).not.toContain(wholesaler);
  const wholesaleNames = await namesFor("&businessType=WHOLESALER");
  expect(wholesaleNames).toContain(wholesaler);
  expect(wholesaleNames).not.toContain(retailer);
  const allNames = await namesFor("");
  expect(allNames).toContain(retailer);
  expect(allNames).toContain(wholesaler);

  // ---- Explore Wholesaler: the wholesaler is there, the retailer is not ----
  await searchFor(page, "/wholesalers", "Search wholesalers, products, areas or pincodes", "Find wholesalers", wholesaler);
  await expect(page.getByRole("heading", { name: "Find wholesalers" })).toBeVisible();
  await expect(page.locator("article.business-card", { hasText: wholesaler })).toBeVisible();
  await page.getByLabel("Search wholesalers, products, areas or pincodes").fill(retailer);
  await expect(page.locator("article.business-card", { hasText: retailer })).toHaveCount(0);

  // ---- Explore Shops: the retailer is there, the wholesaler is not ----
  await searchFor(page, "/shops", "Search shops, products, areas or pincodes", "Find shops", retailer);
  await expect(page.locator("article.business-card", { hasText: retailer })).toBeVisible();
  await page.getByLabel("Search shops, products, areas or pincodes").fill(wholesaler);
  await expect(page.locator("article.business-card", { hasText: wholesaler })).toHaveCount(0);
});
