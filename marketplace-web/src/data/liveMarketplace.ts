import type { MarketplaceCategoryDto, MarketplaceListingDto } from "../api/marketplaceApi";
import type { Business, Category } from "../types";

export function listingToBusiness(listing: MarketplaceListingDto): Business {
  return {
    id: listing.tenantId,
    tenantId: listing.tenantId,
    businessName: listing.businessName ?? "Unnamed business",
    businessType: listing.categoryName ?? "Other",
    shopPhoto: listing.shopPhotoUrl ?? undefined,
    shopPhotoAlt: listing.shopPhotoAlt ?? undefined,
    address: listing.area ?? "",
    area: listing.area ?? "",
    pincode: "",
    // The public listing carries no product data of its own - a shop's real
    // catalogue is only fetched on its own detail page (see LiveShopPage),
    // not for every card in the grid. BusinessCard already shows a graceful
    // "Product previews coming soon" fallback when products is empty.
    products: [],
    links: {
      instagram: listing.instagramUrl ?? undefined,
      facebook: listing.facebookUrl ?? undefined,
      youtube: listing.youtubeUrl ?? undefined,
      googleMaps: listing.googleMapsUrl ?? undefined,
      whatsapp: listing.whatsappUrl ?? undefined,
      website: listing.websiteUrl ?? undefined,
    },
    enquiryWhatsapp: listing.enquiryWhatsapp ?? undefined,
    orderMode: listing.orderMode,
    bookable: listing.bookable,
  };
}

export function categoryToCategory(category: MarketplaceCategoryDto): Category {
  return { id: category.id, name: category.name, icon: category.icon };
}
