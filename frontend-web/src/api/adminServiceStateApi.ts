import { apiClient } from "./client";

export interface AdminServiceState {
  code: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export async function listServiceStates(): Promise<AdminServiceState[]> {
  const { data } = await apiClient.get<AdminServiceState[]>("/admin/service-states");
  return data;
}

export async function updateServiceStateStatus(code: string, active: boolean): Promise<AdminServiceState> {
  const { data } = await apiClient.put<AdminServiceState>(`/admin/service-states/${code}/status`, { active });
  return data;
}
