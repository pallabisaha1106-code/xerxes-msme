import { test, expect, type Page } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { registerLiveBusiness, type LiveBusiness } from "./helpers/liveBusiness";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

/** Logs the vendor in and submits one Feed post, confirming it lands as pending review. */
async function postOffer(page: Page, vendor: LiveBusiness, content: string) {
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(vendor.email);
  await page.getByLabel("Password", { exact: true }).fill(vendor.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);
  await page.goto(`${FRONTEND_WEB_URL}/marketplace/feed`);
  await page.locator("#feed-post-content").fill(content);
  await page.getByRole("button", { name: "Post Update" }).click();
  await expect(page.getByText("PENDING REVIEW")).toBeVisible();
}

/**
 * Trending is the home page: "/" shows the latest offers, each labelled Retail or Wholesale, with a filter for
 * either type, and the old "/feed" address leads there. Explore Shops moved to "/shops". The first-visit welcome
 * dialog on the home page has no "Browse all businesses" way out, because the feed needs a city.
 *
 * A retail and a wholesale business each post one offer and Super Admin approves both. Leaves its data in place,
 * like the other registration specs, and uses the apply form, so the local public rate limit must stay off.
 */
test("the home page shows offers labelled Retail or Wholesale, and the filter narrows them", async ({ page }) => {
  test.setTimeout(420_000);

  const suffix = timestampSuffix();
  const retailer = await registerLiveBusiness(page, `Trend Retail Steel Thread ${suffix}`, "RETAILER");
  const wholesaler = await registerLiveBusiness(page, `Trend Wholesale Steel Thread ${suffix}`, "WHOLESALER");
  const retailOffer = `Trend retail offer ${suffix}`;
  const wholesaleOffer = `Trend wholesale offer ${suffix}`;
  await postOffer(page, retailer, retailOffer);
  await postOffer(page, wholesaler, wholesaleOffer);

  await loginAsSuperAdmin(page);
  await page.goto(`${FRONTEND_WEB_URL}/admin/feed-posts`);
  await page.locator("#status-filter").selectOption("PENDING");
  for (const vendor of [retailer, wholesaler]) {
    const row = page.getByRole("row", { name: new RegExp(vendor.businessName) });
    await expect(row).toBeVisible();
    await row.getByRole("button", { name: "Approve" }).click();
    await expect(row.getByText("APPROVED", { exact: true })).toBeVisible();
  }

  // ---- The old address leads to the home page ----
  await page.goto("/feed");
  await expect(page).toHaveURL("http://localhost:5174/");

  // ---- First visit: pick a city; there is no "Browse all businesses" here ----
  const picker = page.locator("#discovery-welcome");
  await expect(picker.getByRole("button", { name: "Pune" })).toBeVisible();
  await expect(picker.getByRole("button", { name: "Browse all businesses" })).toHaveCount(0);
  await picker.getByRole("button", { name: "Pune" }).click();
  await picker.getByLabel("Area").selectOption({ label: "Kothrud" });
  await picker.getByRole("button", { name: /Show matching businesses/ }).click();

  // ---- Both offers are there, each with its own label ----
  const retailCard = page.getByRole("article").filter({ hasText: retailOffer });
  const wholesaleCard = page.getByRole("article").filter({ hasText: wholesaleOffer });
  await expect(retailCard).toBeVisible();
  await expect(wholesaleCard).toBeVisible();
  await expect(retailCard.getByText("Retail", { exact: true })).toBeVisible();
  await expect(wholesaleCard.getByText("Wholesale", { exact: true })).toBeVisible();

  // ---- The filter narrows to one type and back ----
  const filter = page.getByRole("group", { name: "Show" });
  await filter.getByRole("button", { name: "Wholesale", exact: true }).click();
  await expect(wholesaleCard).toBeVisible();
  await expect(retailCard).toHaveCount(0);
  await expect(page.getByRole("status").filter({ hasText: "· Wholesale" })).toBeVisible();

  await filter.getByRole("button", { name: "Retail", exact: true }).click();
  await expect(retailCard).toBeVisible();
  await expect(wholesaleCard).toHaveCount(0);

  await filter.getByRole("button", { name: "All", exact: true }).click();
  await expect(retailCard).toBeVisible();
  await expect(wholesaleCard).toBeVisible();

  // ---- The menu: Trending is home, Explore Shops moved to /shops ----
  await expect(page.getByRole("navigation", { name: "Main navigation" }).getByRole("link", { name: "Explore Shops" }))
    .toHaveAttribute("href", "/shops");
});
