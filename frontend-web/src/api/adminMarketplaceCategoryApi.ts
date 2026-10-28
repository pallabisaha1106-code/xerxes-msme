import { apiClient } from "./client";

export type ImageProfile = "COMPACT" | "DETAIL";

export interface AdminMarketplaceCategory {
  id: string;
  name: string;
  icon: string;
  displayOrder: number;
  active: boolean;
  imageProfile: ImageProfile;
  bookable: boolean;
}

export async function listMarketplaceCategories(): Promise<AdminMarketplaceCategory[]> {
  const { data } = await apiClient.get<AdminMarketplaceCategory[]>("/admin/marketplace-categories");
  return data;
}

export async function updateMarketplaceCategoryStatus(id: string, active: boolean): Promise<AdminMarketplaceCategory> {
  const { data } = await apiClient.put<AdminMarketplaceCategory>(`/admin/marketplace-categories/${id}/status`, {
    active,
  });
  return data;
}

export async function updateMarketplaceCategoryImageProfile(
  id: string,
  imageProfile: ImageProfile,
): Promise<AdminMarketplaceCategory> {
  const { data } = await apiClient.put<AdminMarketplaceCategory>(`/admin/marketplace-categories/${id}/image-profile`, {
    imageProfile,
  });
  return data;
}

export async function updateMarketplaceCategoryBookable(id: string, bookable: boolean): Promise<AdminMarketplaceCategory> {
  const { data } = await apiClient.put<AdminMarketplaceCategory>(`/admin/marketplace-categories/${id}/bookable`, {
    bookable,
  });
  return data;
}
