import { apiUrl } from "./apiBase";
import { toApiError } from "./apiError";
import type { MarketplaceOrderMode } from "../types";
import type { PartnerContact } from "./partnerApi";

export type BusinessType = "RETAILER" | "WHOLESALER";
export type MembershipType = "FREE" | "PLUS" | "PREMIUM";

export interface VendorApplicationConfigDto {
  /** When false, Super Admin verifies the GSTIN/Aadhaar over a phone call instead - see backend application.yml. */
  kycDocumentsRequired: boolean;
}

export async function getVendorApplicationConfig(): Promise<VendorApplicationConfigDto> {
  const response = await fetch(apiUrl("/api/public/vendor-applications/config"));
  if (!response.ok) throw await toApiError(response, "Could not load application settings");
  return response.json();
}

export interface VendorApplicationRequest {
  businessName: string;
  businessType: BusinessType;
  marketplaceCategoryId: string;
  gstin?: string;
  stateCode: string;
  addressLine1?: string;
  cityId: string;
  areaId: string;
  landmark?: string;
  phone?: string;
  ownerName: string;
  email: string;
  password: string;
}

export interface VendorApplicationResponse {
  tenantId: string;
  registrationCode: string;
  businessName: string;
  /** The local partner looking after this shop, or null when nobody covers the area. */
  partner?: PartnerContact | null;
}

export interface MarketplaceListingResponseDto {
  id: string | null;
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
  status: "DRAFT" | "PENDING" | "APPROVED" | "REJECTED";
  rejectionReason: string | null;
  submittedAt: string | null;
}

export interface VendorApplicationDetails {
  tenantId: string;
  businessName: string;
  businessType: BusinessType;
  stateCode: string;
  addressLine1: string | null;
  city: string | null;
  pincode: string | null;
  phone: string | null;
  subscriptionPlan: MembershipType;
  productCount: number;
  productLimit: number;
  listing: MarketplaceListingResponseDto | null;
  partner?: PartnerContact | null;
}

export interface ListingDetails {
  categoryId?: string;
  area?: string;
  shopPhotoAlt?: string;
  instagramUrl?: string;
  facebookUrl?: string;
  youtubeUrl?: string;
  googleMapsUrl?: string;
  whatsappUrl?: string;
  websiteUrl?: string;
  enquiryWhatsapp?: string;
  orderMode: MarketplaceOrderMode;
}

/**
 * Step 1 of registration: creates an inactive tenant + owner login and hands
 * back a registration code (not a JWT - see backend VendorApplicationController).
 * The applicant types that code back in later to reach Step 2.
 */
export async function applyForVendorAccount(
  request: VendorApplicationRequest,
  shopFrontPhoto: File,
  gstCertificate: File | null,
  ownerIdDocument: File | null,
): Promise<VendorApplicationResponse> {
  const formData = new FormData();
  formData.append("application", new Blob([JSON.stringify(request)], { type: "application/json" }));
  formData.append("shopFrontPhoto", shopFrontPhoto);
  // Optional - see VendorApplicationConfigDto.kycDocumentsRequired. Omitted entirely rather
  // than appended as empty, so the backend's @RequestPart(required=false) sees them as absent.
  if (gstCertificate) formData.append("gstCertificate", gstCertificate);
  if (ownerIdDocument) formData.append("ownerIdDocument", ownerIdDocument);
  const response = await fetch(apiUrl("/api/public/vendor-applications"), {
    method: "POST",
    body: formData,
  });
  if (!response.ok) throw await toApiError(response, "Could not submit your application");
  return response.json();
}

export async function getVendorApplication(code: string): Promise<VendorApplicationDetails> {
  const response = await fetch(apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}`));
  if (!response.ok) throw await toApiError(response, "Could not find that registration code");
  return response.json();
}

export async function updateVendorListing(
  code: string,
  details: ListingDetails,
): Promise<MarketplaceListingResponseDto> {
  const response = await fetch(apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}/listing`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(details),
  });
  if (!response.ok) throw await toApiError(response, "Could not save your listing details");
  return response.json();
}

export async function uploadVendorListingPhoto(code: string, file: File): Promise<MarketplaceListingResponseDto> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await fetch(apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}/photo`), {
    method: "POST",
    body: formData,
  });
  if (!response.ok) throw await toApiError(response, "Could not upload your shop photo");
  return response.json();
}

export async function selectMembershipPlan(code: string, plan: MembershipType): Promise<VendorApplicationDetails> {
  const response = await fetch(apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}/plan`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ plan }),
  });
  if (!response.ok) throw await toApiError(response, "Could not save your membership plan");
  return response.json();
}

/** Seeds the tenant's own Product catalog from the Super Admin's starter list, capped by the chosen plan's limit. */
export async function createVendorStarterProducts(code: string, defaultProductIds: string[]): Promise<void> {
  const response = await fetch(
    apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}/starter-products`),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ defaultProductIds }),
    },
  );
  if (!response.ok) throw await toApiError(response, "Could not add your starter products");
}

export async function submitVendorListing(code: string): Promise<MarketplaceListingResponseDto> {
  const response = await fetch(apiUrl(`/api/public/vendor-applications/${encodeURIComponent(code)}/submit`), {
    method: "POST",
  });
  if (!response.ok) throw await toApiError(response, "Could not submit your listing for review");
  return response.json();
}
