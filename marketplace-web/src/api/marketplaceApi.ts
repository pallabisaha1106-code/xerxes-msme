import { apiUrl } from "./apiBase";
import { toApiError } from "./apiError";
import type { MarketplaceOrderMode } from "../types";
import type { BusinessType, MembershipType } from "./vendorOnboardingApi";

export interface AdvertisementDto {
  imageUrl: string;
  linkUrl: string | null;
  altText: string;
  headline: string | null;
  description: string | null;
  /** Set only on the built-in fallbacks in data/defaultAds.ts - never sent by the API. */
  ctaLabel?: string;
  isDefault?: boolean;
}

export interface AdvertisementSlotsDto {
  topCarousel: AdvertisementDto | null;
  leftRail: AdvertisementDto | null;
  rightRail: AdvertisementDto | null;
}

/** Super Admin-managed ads for the 3 fixed marketplace ad slots - a null slot means nothing is currently published/live there. */
export function listAdvertisements(): Promise<AdvertisementSlotsDto> {
  return getJson("/api/public/marketplace/advertisements");
}

export interface MarketplaceCategoryDto {
  id: string;
  name: string;
  icon: string;
}

export interface MarketplaceListingDto {
  tenantId: string;
  businessName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  area: string | null;
  shopPhotoUrl: string | null;
  shopPhotoAlt: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  youtubeUrl: string | null;
  googleMapsUrl: string | null;
  whatsappUrl: string | null;
  websiteUrl: string | null;
  enquiryWhatsapp: string | null;
  orderMode: MarketplaceOrderMode;
  /** DETAIL shops (apparel etc.) offer zoom on product photos; COMPACT ones just enlarge. */
  imageProfile?: "COMPACT" | "DETAIL";
  /** True when the shop's category takes appointment requests (a salon, a clinic) instead of orders. */
  bookable?: boolean;
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(apiUrl(path));
  if (!response.ok) throw await toApiError(response, `Request to ${path} failed`);
  return response.json() as Promise<T>;
}

export function listMarketplaceCategories(): Promise<MarketplaceCategoryDto[]> {
  return getJson("/api/public/marketplace/categories");
}

export interface MarketplaceListingPageDto {
  items: MarketplaceListingDto[];
  hasMore: boolean;
}

export interface ListMarketplaceListingsParams {
  categoryId?: string;
  cityId?: string;
  areaId?: string;
  /** Retail shops (accounts with no type count as retail) or wholesalers; omitted means both. */
  businessType?: BusinessType;
  page?: number;
  size?: number;
}

/** Server-paginated browse - cityId/areaId scope to that city's vendors (see the Feed page's own city filter). */
export function listMarketplaceListings(params: ListMarketplaceListingsParams = {}): Promise<MarketplaceListingPageDto> {
  const query = new URLSearchParams();
  if (params.categoryId) query.set("categoryId", params.categoryId);
  if (params.cityId) query.set("cityId", params.cityId);
  if (params.areaId) query.set("areaId", params.areaId);
  if (params.businessType) query.set("businessType", params.businessType);
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  const queryString = query.toString();
  return getJson(`/api/public/marketplace/listings${queryString ? `?${queryString}` : ""}`);
}

export function getMarketplaceListing(tenantId: string): Promise<MarketplaceListingDto> {
  return getJson(`/api/public/marketplace/listings/${encodeURIComponent(tenantId)}`);
}

export interface DefaultProductOptionDto {
  id: string;
  name: string;
  imageUrl: string | null;
}

export interface DefaultProductCatalogEntryDto {
  id: string;
  name: string;
  products: DefaultProductOptionDto[];
}

export function listDefaultCatalog(marketplaceCategoryId: string): Promise<DefaultProductCatalogEntryDto[]> {
  return getJson(`/api/public/marketplace/default-catalog?marketplaceCategoryId=${encodeURIComponent(marketplaceCategoryId)}`);
}

export interface ServiceStateDto {
  code: string;
  name: string;
}

export interface ServiceCityDto {
  id: string;
  name: string;
}

export interface ServiceAreaDto {
  id: string;
  name: string;
  pincode: string;
}

/** Super Admin-toggleable GST states the marketplace supports - see AdminServiceStateService. */
export function listServiceStates(): Promise<ServiceStateDto[]> {
  return getJson("/api/public/marketplace/service-states");
}

/** Super Admin-curated cities the marketplace supports under a GST state - see AdminServiceLocationService. */
export function listServiceCities(stateCode: string): Promise<ServiceCityDto[]> {
  return getJson(`/api/public/marketplace/service-cities?stateCode=${encodeURIComponent(stateCode)}`);
}

/** Areas (each carrying its own pincode) under a city - vendor registration is gated on picking one of these. */
export function listServiceAreas(cityId: string): Promise<ServiceAreaDto[]> {
  return getJson(`/api/public/marketplace/service-areas?cityId=${encodeURIComponent(cityId)}`);
}

/** Every city across every state, state-agnostic - for the "search/pick your city" discovery picker. */
export function listAllServiceCities(): Promise<ServiceCityDto[]> {
  return getJson("/api/public/marketplace/service-cities");
}

export interface PublicFeedPostDto {
  tenantId: string;
  businessName: string;
  shopPhotoUrl: string | null;
  area: string | null;
  categoryName: string | null;
  /** An account with no type is reported as a retailer. */
  businessType: BusinessType;
  enquiryWhatsapp: string | null;
  content: string;
  /** Short badge text the vendor attached ("20% off"), if any. */
  discountLabel: string | null;
  /** Last day (YYYY-MM-DD, inclusive) the offer is shown, if the vendor set one. */
  validUntil: string | null;
  createdAt: string;
}

export interface PublicFeedPageDto {
  items: PublicFeedPostDto[];
  hasMore: boolean;
}

/** NEWEST (default) or ENDING_SOON: offers with an end date first, soonest first, then the rest newest-first. */
export type FeedSortOption = "NEWEST" | "ENDING_SOON";

export interface ListPublicFeedParams {
  cityId: string;
  areaId?: string;
  categoryId?: string;
  /** Only retailers (accounts with no type count as retail) or only wholesalers; omitted means both. */
  businessType?: BusinessType;
  sort?: FeedSortOption;
  page?: number;
  size?: number;
}

/** Newest-first page of approved posts within the 14-day window for a city (optionally narrowed by area and category) - see FeedPage. */
export function listPublicFeed(params: ListPublicFeedParams): Promise<PublicFeedPageDto> {
  const query = new URLSearchParams({ cityId: params.cityId });
  if (params.areaId) query.set("areaId", params.areaId);
  if (params.categoryId) query.set("categoryId", params.categoryId);
  if (params.businessType) query.set("businessType", params.businessType);
  if (params.sort) query.set("sort", params.sort);
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  return getJson(`/api/public/marketplace/feed?${query.toString()}`);
}

export interface MembershipPlanDto {
  plan: MembershipType;
  /** Per month, in paise, without GST. */
  monthlyAmountPaise: number;
  productLimit: number;
}

/** The plans, their prices and product limits - the backend's own figures, in order Free, Plus, Premium. */
export function listMembershipPlans(): Promise<MembershipPlanDto[]> {
  return getJson("/api/public/membership-plans");
}
