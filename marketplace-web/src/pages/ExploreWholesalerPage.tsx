import { ExploreListingsPage } from "./ExploreListingsPage";
import type { MarketplacePageCopy } from "./MarketplacePage";
import type { MarketplaceConfig } from "../types";

const config: MarketplaceConfig = {
  name: "Xerxes Marketplace",
  adminWhatsapp: "",
  listingSingular: "wholesaler",
  listingPlural: "wholesalers",
};

const copy: MarketplacePageCopy = {
  pageTitle: "Explore Wholesaler | Neighbourly",
  metaDescription:
    "Find Pune's wholesale suppliers, browse what they sell in bulk, and connect directly on WhatsApp. Your neighbourhood, all in one place.",
  skipLinkLabel: "Skip to wholesalers",
  heading: "Find wholesalers",
  subheading: "Discover wholesale suppliers in Pune.",
  searchAriaLabel: "Search wholesalers, products, areas or pincodes",
  searchPlaceholder: 'Try "rice", "Baner" or "411045"',
  searchButtonLabel: "Find wholesalers",
  categorySectionAriaLabel: "Wholesaler categories",
  emptyHeading: "No wholesalers found just yet",
  onboarding: {
    eyebrow: "FOR WHOLESALE SUPPLIERS",
    heading: (
      <>
        Sell in bulk?
        <br />
        Get found by shops.
      </>
    ),
    paragraph:
      "Set up your wholesale business account and listing in a few minutes - our team will review it before it goes live.",
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

export function ExploreWholesalerPage() {
  return <ExploreListingsPage kind="wholesalers" config={config} copy={copy} loadingText="Loading wholesalers…" />;
}
