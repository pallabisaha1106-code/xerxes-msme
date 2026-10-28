import { apiClient } from "./client";

export type AdvertisementSlot = "TOP_CAROUSEL" | "LEFT_RAIL" | "RIGHT_RAIL";

export interface AdminAdvertisement {
  id: string;
  slot: AdvertisementSlot;
  imageUrl: string | null;
  linkUrl: string | null;
  altText: string;
  headline: string | null;
  description: string | null;
  published: boolean;
  startsAt: string;
  endsAt: string;
  currentlyLive: boolean;
}

export interface CreateAdvertisementRequest {
  slot: AdvertisementSlot;
  linkUrl?: string;
  altText: string;
  headline?: string;
  description?: string;
  startsAt: string;
  endsAt: string;
}

export interface UpdateAdvertisementRequest {
  linkUrl?: string;
  altText: string;
  headline?: string;
  description?: string;
  startsAt: string;
  endsAt: string;
}

export async function listAdvertisements(slot: AdvertisementSlot): Promise<AdminAdvertisement[]> {
  const { data } = await apiClient.get<AdminAdvertisement[]>("/admin/advertisements", { params: { slot } });
  return data;
}

export async function createAdvertisement(request: CreateAdvertisementRequest): Promise<AdminAdvertisement> {
  const { data } = await apiClient.post<AdminAdvertisement>("/admin/advertisements", request);
  return data;
}

export async function updateAdvertisement(id: string, request: UpdateAdvertisementRequest): Promise<AdminAdvertisement> {
  const { data } = await apiClient.put<AdminAdvertisement>(`/admin/advertisements/${id}`, request);
  return data;
}

export async function uploadAdvertisementPhoto(id: string, file: File): Promise<AdminAdvertisement> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<AdminAdvertisement>(`/admin/advertisements/${id}/photo`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function publishAdvertisement(id: string): Promise<AdminAdvertisement> {
  const { data } = await apiClient.put<AdminAdvertisement>(`/admin/advertisements/${id}/publish`);
  return data;
}

export async function unpublishAdvertisement(id: string): Promise<AdminAdvertisement> {
  const { data } = await apiClient.put<AdminAdvertisement>(`/admin/advertisements/${id}/unpublish`);
  return data;
}

export async function deleteAdvertisement(id: string): Promise<void> {
  await apiClient.delete(`/admin/advertisements/${id}`);
}
