import { test, expect } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { registerLiveBusiness } from "./helpers/liveBusiness";
import { timestampSuffix } from "./helpers/testId";

test.use({ baseURL: "http://localhost:5174" });

/**
 * "Add from starter catalogue": the picker a shop owner uses at registration, for a logged-in vendor. A Grocery shop
 * (Free plan, 10 products) registers with one starter product, Amul Milk, logs in to the dashboard and reaches the
 * page from the Products page. The page says how many more products the plan allows, search narrows the category
 * tiles, the category dialog marks Amul Milk "Already in your catalogue" and won't tick it, picks collect in "Your
 * catalogue" (and can be removed), and confirming adds them to the Products page. Reopening shows them as already
 * added, and once the plan's limit is reached the remaining boxes can't be ticked. Also checks that a category with
 * no starter catalogue has nothing to offer. Leaves its data in place, like the other registration specs; the local
 * public rate limit must stay off (its local default).
 */
test("a Grocery vendor adds products from the starter catalogue after logging in", async ({ page, request }) => {
  test.setTimeout(300_000);

  const suffix = timestampSuffix();
  const vendor = await registerLiveBusiness(page, `Starter Catalogue Steel Thread ${suffix}`, "RETAILER", {
    category: "Grocery",
    group: "Milk & Dairy Products",
    product: "Amul Milk",
  });
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(vendor.email);
  await page.getByLabel("Password", { exact: true }).fill(vendor.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  // ---- The Products page offers the new way in ----
  await page.goto(`${FRONTEND_WEB_URL}/products`);
  await page.getByRole("button", { name: "Add from starter catalogue" }).click();
  await expect(page.getByRole("heading", { name: "Add from starter catalogue" })).toBeVisible();
  await expect(page.getByText("PRODUCT LIBRARY")).toBeVisible();
  // Free plan: 10 products, one already there.
  await expect(page.getByText("Your plan allows up to 9 products.")).toBeVisible();

  // ---- Search narrows the category tiles, by group name or by a product inside ----
  await expect(page.getByRole("button", { name: "Rice", exact: true })).toBeVisible();
  await page.getByLabel("Search products").fill("milk");
  await expect(page.getByRole("button", { name: "Milk & Dairy Products", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Rice", exact: true })).toHaveCount(0);
  await page.getByLabel("Search products").fill("");

  // ---- The category dialog: Amul Milk is already there; pick three others ----
  await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Milk & Dairy Products" });
  await expect(dialog.getByText("Select any products you stock.")).toBeVisible();
  await expect(dialog.getByText("Already in your catalogue")).toHaveCount(1);
  await expect(dialog.getByRole("checkbox", { name: /Amul Milk/ })).toBeDisabled();
  await dialog.getByRole("checkbox", { name: "Mother Dairy Milk" }).check();
  await dialog.getByRole("checkbox", { name: "Amul Butter" }).check();
  await dialog.getByRole("checkbox", { name: "Amul Curd" }).check();
  await dialog.getByRole("button", { name: "Done" }).click();

  // ---- "Your catalogue" collects the picks; one can be removed ----
  await expect(page.getByText("Milk & Dairy Products").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "Milk & Dairy Products" })).toContainText("3 selected");
  await expect(page.getByRole("button", { name: "Add 3 products" })).toBeVisible();
  await page.getByRole("button", { name: "Remove Amul Curd" }).click();
  await expect(page.getByRole("button", { name: "Add 2 products" })).toBeVisible();

  // ---- Confirming adds them, and the vendor lands on the Products page ----
  await page.getByRole("button", { name: "Add 2 products" }).click();
  await expect(page).toHaveURL(`${FRONTEND_WEB_URL}/products`);
  await expect(page.getByText("2 products added")).toBeVisible();
  await expect(page.getByRole("button", { name: "Mother Dairy Milk" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Amul Butter" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Amul Curd" })).toHaveCount(0);

  // ---- Reopening: those are now "Already in your catalogue", and the plan limit stops further picks ----
  await page.getByRole("button", { name: "Add from starter catalogue" }).click();
  await expect(page.getByText("Your plan allows up to 7 products.")).toBeVisible();
  await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
  const again = page.getByRole("dialog", { name: "Milk & Dairy Products" });
  await expect(again.getByText("Already in your catalogue")).toHaveCount(3);
  const stillPickable = again.locator('input[type="checkbox"]:not(:checked):not(:disabled)');
  for (let i = 0; i < 7; i++) await stillPickable.first().check();
  // Seven picked is every slot the plan has left: nothing else can be ticked.
  await expect(stillPickable).toHaveCount(0);
  await again.getByRole("button", { name: "Done" }).click();
  await page.getByRole("button", { name: "Cancel" }).click();
  await expect(page).toHaveURL(`${FRONTEND_WEB_URL}/products`);

  // ---- A category with no starter catalogue has nothing to offer, so its shops get no button ----
  const categories = (await (await request.get("/api/public/marketplace/categories")).json()) as { id: string; name: string }[];
  const idOf = (name: string) => categories.find((c) => c.name === name)!.id;
  const catalogSize = async (name: string) =>
    ((await (await request.get(`/api/public/marketplace/default-catalog?marketplaceCategoryId=${idOf(name)}`)).json()) as unknown[]).length;
  expect(await catalogSize("Grocery")).toBeGreaterThan(0);
  expect(await catalogSize("Salon & Spa")).toBe(0);
});
