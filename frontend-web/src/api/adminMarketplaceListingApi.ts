import { apiClient } from "./client";
import type { MarketplaceListingStatus, MarketplaceOrderMode } from "./marketplaceListingApi";

export interface AdminMarketplaceListing {
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
  status: MarketplaceListingStatus;
  rejectionReason: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
}

export async function listMarketplaceListings(status?: MarketplaceListingStatus): Promise<AdminMarketplaceListing[]> {
  const { data } = await apiClient.get<AdminMarketplaceListing[]>("/admin/marketplace/listings", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function approveMarketplaceListing(tenantId: string): Promise<AdminMarketplaceListing> {
  const { data } = await apiClient.post<AdminMarketplaceListing>(`/admin/marketplace/listings/${tenantId}/approve`);
  return data;
}

export async function rejectMarketplaceListing(tenantId: string, reason?: string): Promise<AdminMarketplaceListing> {
  const { data } = await apiClient.post<AdminMarketplaceListing>(`/admin/marketplace/listings/${tenantId}/reject`, {
    reason,
  });
  return data;
}

export async function unpublishMarketplaceListing(tenantId: string, reason?: string): Promise<AdminMarketplaceListing> {
  const { data } = await apiClient.post<AdminMarketplaceListing>(`/admin/marketplace/listings/${tenantId}/unpublish`, {
    reason,
  });
  return data;
}
