import type { AdvertisementDto } from "../api/marketplaceApi";

/** Platform house ads - shown for a slot only when no real, Super Admin-published ad is live there. */
export const DEFAULT_ADS: { topCarousel: AdvertisementDto; leftRail: AdvertisementDto; rightRail: AdvertisementDto } = {
  topCarousel: {
    imageUrl: "images/defaults/default-ad-top.svg",
    linkUrl: "/list-your-business",
    altText: "A neighbourhood shop with a striped awning and a new-order notification",
    headline: "Get found by every neighbour",
    description: "List free in minutes. Orders reach your WhatsApp.",
    ctaLabel: "List your business",
    isDefault: true,
  },
  leftRail: {
    imageUrl: "images/defaults/default-ad-left.svg",
    linkUrl: "/",
    altText: "Offers from shops near you. See the feed.",
    headline: null,
    description: null,
    isDefault: true,
  },
  rightRail: {
    imageUrl: "images/defaults/default-ad-right.svg",
    linkUrl: "#marketplace",
    altText: "Shop local. Chat to order. Explore shops.",
    headline: null,
    description: null,
    isDefault: true,
  },
};
