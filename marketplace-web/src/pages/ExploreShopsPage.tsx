import { ExploreListingsPage } from "./ExploreListingsPage";
import type { MarketplacePageCopy } from "./MarketplacePage";
import type { MarketplaceConfig } from "../types";

const config: MarketplaceConfig = { name: "Xerxes Marketplace", adminWhatsapp: "" };

const copy: MarketplacePageCopy = {
  pageTitle: "Discover Local Shops & Businesses | Neighbourly",
  metaDescription:
    "Discover Pune's independent shops, browse their products, and connect directly on WhatsApp. Your neighbourhood, all in one place.",
  skipLinkLabel: "Skip to shops",
  heading: "Find local shops",
  subheading: "Discover shops and products in Pune.",
  searchAriaLabel: "Search shops, products, areas or pincodes",
  searchPlaceholder: 'Try "clothing", "Baner" or "411045"',
  searchButtonLabel: "Find shops",
  categorySectionAriaLabel: "Shop categories",
  emptyHeading: "No shops found just yet",
  onboarding: {
    eyebrow: "FOR THE PEOPLE BEHIND THE COUNTER",
    heading: (
      <>
        Big on local?
        <br />
        So are we.
      </>
    ),
    paragraph: "Let’s help your neighbours find you. Set up your business account and shop listing in a few minutes - our team will review it before it goes live.",
    buttonLabel: "List Your Business ↗",
    listItems: [
      "Shop photo",
      "Business name and category",
      "Your business WhatsApp number",
      "Area / neighbourhood",
      "Optional: Instagram, Facebook, YouTube, Google Maps and website links",
    ],
    footerLinkLabel: "List your business",
    ctaTo: "/list-your-business",
  },
};

export function ExploreShopsPage() {
  return <ExploreListingsPage kind="shops" config={config} copy={copy} loadingText="Loading shops…" />;
}
