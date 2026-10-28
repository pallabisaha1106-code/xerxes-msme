import { apiClient } from "./client";

export type MarketplaceOrderMode = "WHATSAPP" | "CHECKOUT";
export type MarketplaceListingStatus = "DRAFT" | "PENDING" | "LIVE";

export interface MarketplaceCategory {
  id: string;
  name: string;
  icon: string;
}

export interface MarketplaceListing {
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
  status: MarketplaceListingStatus;
  rejectionReason: string | null;
  submittedAt: string | null;
}

export interface UpdateMarketplaceListingRequest {
  categoryId: string | null;
  area: string | null;
  shopPhotoAlt: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  youtubeUrl: string | null;
  googleMapsUrl: string | null;
  whatsappUrl: string | null;
  websiteUrl: string | null;
  enquiryWhatsapp: string | null;
  orderMode: MarketplaceOrderMode;
}

export async function listMarketplaceCategories(): Promise<MarketplaceCategory[]> {
  const { data } = await apiClient.get<MarketplaceCategory[]>("/marketplace/categories");
  return data;
}

export async function getMarketplaceListing(): Promise<MarketplaceListing> {
  const { data } = await apiClient.get<MarketplaceListing>("/marketplace/listing");
  return data;
}

export async function updateMarketplaceListing(request: UpdateMarketplaceListingRequest): Promise<MarketplaceListing> {
  const { data } = await apiClient.put<MarketplaceListing>("/marketplace/listing", request);
  return data;
}

export async function uploadMarketplacePhoto(file: File): Promise<MarketplaceListing> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<MarketplaceListing>("/marketplace/listing/photo", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function deleteMarketplacePhoto(): Promise<MarketplaceListing> {
  const { data } = await apiClient.delete<MarketplaceListing>("/marketplace/listing/photo");
  return data;
}

export async function submitMarketplaceListing(): Promise<MarketplaceListing> {
  const { data } = await apiClient.post<MarketplaceListing>("/marketplace/listing/submit");
  return data;
}
