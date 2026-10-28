import { test, expect, type Page } from "@playwright/test";
import { FRONTEND_WEB_URL } from "../config";
import { applyForVendorAccount } from "./helpers/vendorApplication";
import { loginAsSuperAdmin } from "./helpers/adminLogin";
import { timestampSuffix } from "./helpers/testId";

// Same shape as grocery-vendor-free-whatsapp.spec.ts - see that file for the
// slowMo/headless rationale.
test.use({ baseURL: "http://localhost:5174", headless: false, video: "on", launchOptions: { slowMo: 200 } });

interface LiveVendor {
  email: string;
  password: string;
  businessName: string;
}

/**
 * Registers a Grocery vendor on the Free plan with a single starter product,
 * then has Super Admin approve both the vendor account and the marketplace
 * listing, so the shop is live and able to post to the Feed. A rejected
 * post must come from a real, live shop - not one blocked by some other
 * gate - to actually prove rejection itself is what keeps it off the public
 * feed, so both vendors this spec needs go through the full approval.
 */
async function registerLiveVendor(page: Page, label: "Approved" | "Rejected" | "Plain", suffix: string): Promise<LiveVendor> {
  const email = `feed-${label.toLowerCase()}-${suffix}@test.com`;
  const password = "password1234";
  const businessName = `Feed ${label} Steel Thread ${suffix}`;

  await applyForVendorAccount(page, { businessName, ownerName: "Test Owner", email, password });
  await page.getByRole("link", { name: "Build My Catalogue Now" }).click();

  await expect(page.getByRole("heading", { name: /Build your catalogue/ })).toBeVisible();
  await page.getByRole("radio", { name: /^Free/ }).click();
  await expect(page.getByRole("radio", { name: /^Free/ })).toBeChecked();

  await page.getByRole("button", { name: "Milk & Dairy Products", exact: true }).click();
  await page.getByRole("checkbox", { name: "Amul Milk" }).check();
  await page.getByRole("button", { name: "Done" }).click();
  await expect(page.locator(".selection-card")).toHaveCount(1);

  await page.getByLabel("WhatsApp number for enquiries").fill("9876543210");
  await page.getByRole("button", { name: "Submit Listing" }).click();
  await expect(page.getByRole("heading", { name: new RegExp(`You.re all set, ${businessName}`) })).toBeVisible({
    timeout: 15_000,
  });

  await loginAsSuperAdmin(page);
  await page.getByRole("button", { name: businessName }).click();
  await expect(page.getByRole("heading", { name: businessName })).toBeVisible();
  await page.getByRole("button", { name: "Reactivate Vendor" }).click();
  await expect(page.getByRole("button", { name: "Suspend Vendor" })).toBeVisible();

  await page.goto(`${FRONTEND_WEB_URL}/admin/marketplace/listings`);
  await page.locator("#status-filter").selectOption("PENDING");
  const listingRow = page.getByRole("row", { name: new RegExp(businessName) });
  await expect(listingRow).toBeVisible();
  await listingRow.getByRole("button", { name: "Approve" }).click();
  await expect(listingRow.getByText("LIVE")).toBeVisible();

  return { email, password, businessName };
}

interface OfferFields {
  discountLabel: string;
  /** YYYY-MM-DD, the value an <input type="date"> takes. */
  validUntil: string;
}

/** Logs the vendor in and submits one Feed post (optionally with a discount label and end date), confirming it lands as pending review. */
async function postToFeed(page: Page, vendor: LiveVendor, content: string, offer?: OfferFields): Promise<void> {
  await page.goto(`${FRONTEND_WEB_URL}/login`);
  await page.getByLabel("Email").fill(vendor.email);
  await page.getByLabel("Password", { exact: true }).fill(vendor.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(`${FRONTEND_WEB_URL}/`);

  await page.goto(`${FRONTEND_WEB_URL}/marketplace/feed`);
  await page.locator("#feed-post-content").fill(content);
  if (offer) {
    await page.locator("#feed-post-discount").fill(offer.discountLabel);
    await page.locator("#feed-post-valid-until").fill(offer.validUntil);
  }
  await page.getByRole("button", { name: "Post Update" }).click();
  await expect(page.getByText("Post submitted for review")).toBeVisible();
  await expect(page.getByText("PENDING REVIEW")).toBeVisible();
}

/**
 * Full Feed loop: three Grocery vendors register free and go live and each
 * posts one Feed update - one with a discount label and an end date of today
 * (to be approved), one the same way (to be rejected), and a plain one with
 * neither, posted last so it is the newest (to be approved). Super Admin sees
 * the label and end date on the review page, approves two and rejects one. A
 * marketplace visitor choosing Pune/Kothrud in the city picker then sees the
 * approved offers as cards - the dated one with its "20% off" badge, "Ends
 * today" tag and category label, the rejected one and its label never - and
 * the sort control flips the order: Newest leads with the plain offer, "Ending
 * soon" leads with the dated one. The dated card's "View shop" link leads to
 * the live shop.
 *
 * Deliberately leaves its data in place afterward - see
 * grocery-vendor-free-whatsapp.spec.ts for why the timestamp suffix makes
 * that safe.
 */
test("vendor Feed posts are reviewed by Super Admin and only the approved one reaches the public feed", async ({
  page,
}) => {
  test.setTimeout(180_000);

  const suffix = timestampSuffix();
  const approvedContent = `Feed approved steel thread offer ${suffix}`;
  const rejectedContent = `Feed rejected steel thread offer ${suffix}`;
  const plainContent = `Feed plain steel thread offer ${suffix}`;
  // Today is the earliest end date an offer can have and still be visible, so under "Ending soon" the approved
  // post always ranks above the undated "plain" one: dated offers come first, and among offers ending today the
  // newer post wins a tie. (en-CA is YYYY-MM-DD.)
  const today = new Date().toLocaleDateString("en-CA");
  const approvedOffer: OfferFields = { discountLabel: "20% off", validUntil: today };
  const rejectedOffer: OfferFields = { discountLabel: `Rejected deal ${suffix}`, validUntil: today };

  // ---- Three live vendors, one post each. The "plain" post (no label, no end date) goes last, so it is the newest:
  // that is what lets the sort check below tell "Newest" from "Ending soon" - the approved offer is older. ----
  const approvedVendor = await registerLiveVendor(page, "Approved", suffix);
  await postToFeed(page, approvedVendor, approvedContent, approvedOffer);

  const rejectedVendor = await registerLiveVendor(page, "Rejected", suffix);
  await postToFeed(page, rejectedVendor, rejectedContent, rejectedOffer);

  const plainVendor = await registerLiveVendor(page, "Plain", suffix);
  await postToFeed(page, plainVendor, plainContent);

  // ---- Super Admin reviews the queue: approves two, rejects the other ----
  await loginAsSuperAdmin(page);
  await page.goto(`${FRONTEND_WEB_URL}/admin/feed-posts`);
  await page.locator("#status-filter").selectOption("PENDING");

  const approvedRow = page.getByRole("row", { name: new RegExp(approvedVendor.businessName) });
  await expect(approvedRow).toBeVisible();
  // The review page shows the discount label and an end date (cell 3 is "Ends on"; "-" would mean none was saved).
  await expect(approvedRow.getByText("20% off")).toBeVisible();
  await expect(approvedRow.getByRole("cell").nth(3)).not.toHaveText("-");
  await approvedRow.getByRole("button", { name: "Approve" }).click();
  // exact: true - the business name itself ("Feed Approved Steel Thread ...")
  // otherwise substring-matches "APPROVED" case-insensitively too.
  await expect(approvedRow.getByText("APPROVED", { exact: true })).toBeVisible();

  const plainRow = page.getByRole("row", { name: new RegExp(plainVendor.businessName) });
  await expect(plainRow).toBeVisible();
  await plainRow.getByRole("button", { name: "Approve" }).click();
  await expect(plainRow.getByText("APPROVED", { exact: true })).toBeVisible();

  const rejectedRow = page.getByRole("row", { name: new RegExp(rejectedVendor.businessName) });
  await expect(rejectedRow).toBeVisible();
  await rejectedRow.getByRole("button", { name: "Reject" }).click();
  await page.getByLabel("Reason (optional)").fill("Not a genuine offer");
  await page.getByRole("dialog", { name: "Reject Post" }).getByRole("button", { name: "Reject" }).click();
  await expect(rejectedRow.getByText("REJECTED", { exact: true })).toBeVisible();

  // ---- A marketplace visitor choosing the vendors' own area sees the approved posts, never the rejected one ----
  // A city saved earlier in this browser would skip the picker - clear it on every load from here on.
  await page.addInitScript(() => localStorage.removeItem("neighbourly.discovery.v2.shops"));
  await page.goto("http://localhost:5174/");

  const picker = page.locator("#discovery-welcome");
  await picker.getByRole("button", { name: "Pune" }).click();
  await picker.getByLabel("Area").selectOption({ label: "Kothrud" });
  await picker.getByRole("button", { name: /Show matching businesses/ }).click();
  await expect(page.getByRole("button", { name: "Change location, currently Pune · Kothrud" })).toBeVisible();

  const approvedCard = page.getByRole("article").filter({ hasText: approvedContent });
  await expect(approvedCard).toBeVisible();
  await expect(approvedCard.getByText("Grocery", { exact: true })).toBeVisible();
  // The vendor's discount label shows as a badge, and an end date of today as an "Ends today" tag.
  await expect(approvedCard.getByText("20% off")).toBeVisible();
  await expect(approvedCard.getByText("Ends today")).toBeVisible();
  // Asserted after the approved card is on screen, so the feed has actually loaded.
  await expect(page.getByText(rejectedContent)).toHaveCount(0);
  await expect(page.getByText(rejectedOffer.discountLabel)).toHaveCount(0);

  // Newest first (the default): the plain offer was posted last, so it leads.
  await expect(page.getByRole("article").first()).toContainText(plainContent);

  // "Ending soon" puts offers with an end date first - the approved offer ends today, so it overtakes the newer
  // plain one, which has no end date. (Only the top spot is asserted: earlier runs today leave other offers ending
  // today, which also rank above the undated plain one.)
  await page.getByLabel("Sort").selectOption("ENDING_SOON");
  await expect(page.getByRole("article").first()).toContainText(approvedContent);
  await expect(page.getByRole("article").first()).not.toContainText(plainContent);

  // The card's "View shop" link leads to the live shop.
  await approvedCard.getByRole("link", { name: `View ${approvedVendor.businessName}` }).click();
  await expect(page.getByRole("heading", { name: approvedVendor.businessName })).toBeVisible();
});
