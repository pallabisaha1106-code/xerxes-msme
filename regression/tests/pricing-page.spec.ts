import { test, expect } from "@playwright/test";

test.use({ baseURL: "http://localhost:5174" });

interface Plan {
  plan: "FREE" | "PLUS" | "PREMIUM";
  monthlyAmountPaise: number;
  productLimit: number;
}

const LABEL = { FREE: "Free", PLUS: "Plus", PREMIUM: "Premium" } as const;

/**
 * The public pricing page lists the three plans with the prices and product limits the backend itself reports
 * (/api/public/membership-plans), says "plus GST" on the paid ones only, explains the billing, and leads to the
 * application form, and is a menu item of its own (the menu marks only the page you are on). Runs on the desktop project and the phone-sized one (plans side by side vs stacked, and never any
 * sideways scroll). Creates no data.
 */
test("the pricing page shows the three plans exactly as the backend reports them", async ({ page, request, isMobile }) => {
  const plans = (await (await request.get("/api/public/membership-plans")).json()) as Plan[];
  expect(plans.map((p) => p.plan)).toEqual(["FREE", "PLUS", "PREMIUM"]);

  await page.goto("/pricing");
  await expect(page.getByRole("heading", { name: "Simple pricing for local businesses" })).toBeVisible();

  const cardBoxes = [];
  for (const plan of plans) {
    const card = page.getByRole("region", { name: LABEL[plan.plan], exact: true });
    await expect(card).toBeVisible();
    await expect(card.getByText(`₹${(plan.monthlyAmountPaise / 100).toLocaleString("en-IN")}`)).toBeVisible();
    await expect(card.getByText(`Up to ${plan.productLimit} products`)).toBeVisible();
    // Free has nothing to add GST to; the paid plans are quoted without it.
    await expect(card.getByText("plus GST")).toHaveCount(plan.monthlyAmountPaise > 0 ? 1 : 0);
    cardBoxes.push((await card.boundingBox())!);
  }
  if (isMobile) {
    // Stacked: each card starts below the previous one, at the same left edge.
    expect(cardBoxes[1].y).toBeGreaterThan(cardBoxes[0].y + cardBoxes[0].height - 1);
    expect(Math.abs(cardBoxes[1].x - cardBoxes[0].x)).toBeLessThan(2);
  } else {
    // Side by side: the same top edge, each to the right of the one before.
    expect(Math.abs(cardBoxes[1].y - cardBoxes[0].y)).toBeLessThan(2);
    expect(cardBoxes[1].x).toBeGreaterThan(cardBoxes[0].x + cardBoxes[0].width - 1);
    expect(cardBoxes[2].x).toBeGreaterThan(cardBoxes[1].x + cardBoxes[1].width - 1);
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);

  await expect(page.getByRole("heading", { name: "Included in every plan" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "How billing works" })).toBeVisible();
  await expect(page.getByText("Your 30 days start when you apply")).toBeVisible();
  await expect(page.getByText("Prices are shown without GST.")).toBeVisible();

  // ---- The main menu: Pricing sits in the same row as the other links and is the one marked as current ----
  const menu = page.getByRole("navigation", { name: "Main navigation" });
  if (isMobile) await page.getByRole("button", { name: "Toggle navigation" }).click();
  await expect(menu.getByRole("link", { name: "Pricing" })).toHaveAttribute("aria-current", "page");
  await expect(menu.getByRole("link", { name: "Explore Shops" })).not.toHaveAttribute("aria-current", "page");
  if (!isMobile) {
    const shopsBox = (await menu.getByRole("link", { name: "Explore Shops" }).boundingBox())!;
    const pricingBox = (await menu.getByRole("link", { name: "Pricing" }).boundingBox())!;
    expect(Math.abs(pricingBox.y - shopsBox.y)).toBeLessThan(2);

    // The words sit on one baseline: the flame emoji and the arrow come from other fonts and used to push
    // "Trending" and "For business owners" off it, which boxes alone do not show.
    const baselines = await menu.locator("a").evaluateAll((links) =>
      links.map((link) => {
        const probe = document.createElement("span");
        probe.style.cssText = "display:inline-block;width:0;height:0;vertical-align:baseline";
        link.appendChild(probe);
        const baseline = probe.getBoundingClientRect().bottom;
        link.removeChild(probe);
        return baseline;
      }),
    );
    expect(baselines.length).toBe(5);
    expect(Math.max(...baselines) - Math.min(...baselines)).toBeLessThan(1);
  }

  // ---- A plan's button leads to the application form ----
  await page.getByRole("region", { name: "Plus", exact: true }).getByRole("link", { name: "Register your business" }).click();
  await expect(page).toHaveURL("http://localhost:5174/list-your-business/apply");

  // ---- The page is reachable from the List your business page and the footer (which belongs to no menu item) ----
  await page.goto("/list-your-business");
  await expect(menu.locator('[aria-current="page"]')).toHaveCount(0);
  await page.getByRole("link", { name: "See pricing" }).click();
  await expect(page).toHaveURL("http://localhost:5174/pricing");
  await page.getByRole("contentinfo").getByRole("link", { name: "Pricing" }).click();
  await expect(page).toHaveURL("http://localhost:5174/pricing");
});
