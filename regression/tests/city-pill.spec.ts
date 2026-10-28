import { test, expect } from "@playwright/test";

test.use({ baseURL: "http://localhost:5174" });

/**
 * The shop list page has a city pill in its filter row, so a shopper can change city without hunting for the small
 * label in the header (which is hidden on phones). Runs on the desktop project and the phone-sized one: on a phone
 * the pill must sit on its own full-width row, and on either the page must not scroll sideways. Creates no data.
 */
test("a shopper changes city from the pill in the filter row", async ({ page, isMobile }) => {
  await page.goto("/shops");

  // First visit: the welcome dialog asks for a city, then for an area and category.
  const welcome = page.getByRole("dialog");
  await welcome.getByRole("button", { name: "Pune" }).click();
  await welcome.getByRole("button", { name: /Show matching businesses/ }).click();

  const pill = page.getByRole("button", { name: "Change city, currently Pune" });
  await expect(pill).toBeVisible();

  const pillBox = (await pill.boundingBox())!;
  const searchBox = (await page.locator("#search-form").boundingBox())!;
  const areaBox = (await page.getByLabel("Filter by area").boundingBox())!;
  expect(pillBox.height).toBeGreaterThanOrEqual(40);
  if (isMobile) {
    // Phone: its own row, under the search box and above Area, as wide as the search box.
    expect(pillBox.y).toBeGreaterThan(searchBox.y + searchBox.height - 1);
    expect(areaBox.y).toBeGreaterThan(pillBox.y + pillBox.height - 1);
    expect(pillBox.width).toBeGreaterThan(searchBox.width - 2);
  } else {
    // Desktop: in the same row as Area, to its left.
    expect(Math.abs(pillBox.y + pillBox.height / 2 - (areaBox.y + areaBox.height / 2))).toBeLessThan(12);
    expect(pillBox.x + pillBox.width).toBeLessThanOrEqual(areaBox.x);
  }
  const sidewaysScroll = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(sidewaysScroll).toBeLessThanOrEqual(1);

  // Tapping the pill brings the dialog back on the same page (no reload), and choosing again updates the pill.
  await page.evaluate(() => ((window as unknown as { samePage: boolean }).samePage = true));
  await pill.click();
  await expect(welcome).toBeVisible();
  await welcome.getByRole("button", { name: "Pune" }).click();
  await welcome.getByRole("button", { name: /Show matching businesses/ }).click();
  await expect(welcome).toBeHidden();
  await expect(pill).toBeVisible();
  expect(await page.evaluate(() => (window as unknown as { samePage?: boolean }).samePage)).toBe(true);
});
