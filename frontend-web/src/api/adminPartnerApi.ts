import { apiClient } from "./client";
import type { PartnerArea } from "./partnerApi";

export type AdminPartnerStatus = "APPLIED" | "ACTIVE" | "INACTIVE" | "REJECTED";

export interface AdminPartner {
  id: string;
  name: string;
  email: string;
  whatsapp: string;
  status: AdminPartnerStatus;
  areas: PartnerArea[];
  assignedShopCount: number;
  createdAt: string;
  decidedAt: string | null;
  decisionReason: string | null;
}

export async function listPartners(status?: AdminPartnerStatus): Promise<AdminPartner[]> {
  const { data } = await apiClient.get<AdminPartner[]>("/admin/partners", { params: status ? { status } : {} });
  return data;
}

export async function approvePartner(id: string): Promise<AdminPartner> {
  const { data } = await apiClient.post<AdminPartner>(`/admin/partners/${id}/approve`);
  return data;
}

export async function rejectPartner(id: string, reason: string): Promise<AdminPartner> {
  const { data } = await apiClient.post<AdminPartner>(`/admin/partners/${id}/reject`, { reason });
  return data;
}

export async function deactivatePartner(id: string): Promise<AdminPartner> {
  const { data } = await apiClient.post<AdminPartner>(`/admin/partners/${id}/deactivate`);
  return data;
}

export async function reactivatePartner(id: string): Promise<AdminPartner> {
  const { data } = await apiClient.post<AdminPartner>(`/admin/partners/${id}/reactivate`);
  return data;
}

export async function replacePartnerAreas(id: string, areaIds: string[]): Promise<AdminPartner> {
  const { data } = await apiClient.put<AdminPartner>(`/admin/partners/${id}/areas`, { areaIds });
  return data;
}

export interface WaitingArea {
  areaId: string;
  areaName: string;
  cityName: string | null;
  waitingShops: number;
  activePartners: number;
}

/** Areas with shops that need a partner: they have none, or theirs has been switched off. */
export async function listWaitingAreas(): Promise<WaitingArea[]> {
  const { data } = await apiClient.get<WaitingArea[]>("/admin/partner-waiting-areas");
  return data;
}

export async function assignWaitingArea(areaId: string): Promise<{ assigned: number; stillWaiting: number }> {
  const { data } = await apiClient.post<{ assigned: number; stillWaiting: number }>(`/admin/partner-waiting-areas/${areaId}/assign`);
  return data;
}
