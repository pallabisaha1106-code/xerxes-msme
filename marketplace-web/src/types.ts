export interface ContactLinks {
  instagram?: string;
  facebook?: string;
  youtube?: string;
  googleMaps?: string;
  whatsapp?: string;
  website?: string;
}

/** Grocery products carry packSize + mrp; other shops just show name/image/category. */
export interface Product {
  id?: string;
  name: string;
  image?: string;
  packSize?: string;
  mrp?: number;
  category?: string;
}

export interface ProductPageConfig {
  enabled: boolean;
  /** A local page filename, e.g. "veera-collection.html" - also the route path in this app. */
  url: string;
  products: Product[];
}

export type MarketplaceOrderMode = "WHATSAPP" | "CHECKOUT";

export interface Business {
  id: string;
  businessName: string;
  /** Must match a Category name exactly - it's the filter facet. */
  businessType: string;
  shopPhoto?: string;
  shopPhotoAlt?: string;
  address: string;
  area: string;
  pincode: string;
  city?: string;
  products: Product[];
  links?: ContactLinks;
  /** Direct WhatsApp number (digits only) for product enquiries - independent of links.whatsapp. */
  enquiryWhatsapp?: string;
  productPage?: ProductPageConfig;
  /** Present only for a real, API-backed listing (see src/api/marketplaceApi.ts) - routes to /shop/:tenantId instead of productPage's static .html route. */
  tenantId?: string;
  /** How a real listing takes orders: a WhatsApp enquiry per product, or a real cart + checkout against its live inventory. Undefined for the hardcoded demo/wholesaler data. */
  orderMode?: MarketplaceOrderMode;
  /** True for a shop in a bookable category (a salon, a clinic) - it takes appointment requests. */
  bookable?: boolean;
}

export interface Category {
  name: string;
  icon: string;
  /** Present only for a real, API-backed category (see categoryToCategory) - lets the listings fetch filter server-side by id. */
  id?: string;
}

export interface MarketplaceConfig {
  name: string;
  adminWhatsapp: string;
  listingSingular?: string;
  listingPlural?: string;
  submissionMessage?: string;
}

export interface MarketplaceData {
  config: MarketplaceConfig;
  categories: Category[];
  businesses: Business[];
}


export interface SocialLinks {
  instagram?: string;
  facebook?: string;
  youtube?: string;
  whatsapp?: string;
}
