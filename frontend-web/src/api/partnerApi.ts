import { apiClient } from "./client";

export interface PartnerArea {
  areaId: string;
  areaName: string;
  cityName: string;
}

export interface PartnerProfile {
  id: string;
  name: string;
  email: string;
  whatsapp: string;
  status: "APPLIED" | "ACTIVE" | "INACTIVE" | "REJECTED";
  areas: PartnerArea[];
  assignedShopCount: number;
}

/** The signed-in partner's own record (PARTNER role only). */
export async function getMyPartnerProfile(): Promise<PartnerProfile> {
  const { data } = await apiClient.get<PartnerProfile>("/partner/me");
  return data;
}

export interface PartnerContact {
  name: string;
  whatsapp: string;
}

/** The partner looking after the signed-in shop, or null when there is none (the API answers 204). */
export async function getMyPartnerContact(): Promise<PartnerContact | null> {
  const { data, status } = await apiClient.get<PartnerContact | "">("/partners/mine");
  return status === 204 || !data ? null : data;
}
