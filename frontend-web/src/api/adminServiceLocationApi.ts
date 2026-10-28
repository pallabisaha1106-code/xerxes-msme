import { apiClient } from "./client";

export interface AdminServiceCity {
  id: string;
  stateCode: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminServiceArea {
  id: string;
  cityId: string;
  name: string;
  pincode: string;
  displayOrder: number;
  active: boolean;
}

export async function listServiceCities(stateCode: string): Promise<AdminServiceCity[]> {
  const { data } = await apiClient.get<AdminServiceCity[]>("/admin/service-cities", { params: { stateCode } });
  return data;
}

export async function createServiceCity(stateCode: string, name: string, displayOrder: number): Promise<AdminServiceCity> {
  const { data } = await apiClient.post<AdminServiceCity>("/admin/service-cities", { stateCode, name, displayOrder });
  return data;
}

export async function updateServiceCity(
  id: string,
  name: string,
  displayOrder: number,
  active: boolean,
): Promise<AdminServiceCity> {
  const { data } = await apiClient.put<AdminServiceCity>(`/admin/service-cities/${id}`, { name, displayOrder, active });
  return data;
}

export async function listServiceAreas(cityId: string): Promise<AdminServiceArea[]> {
  const { data } = await apiClient.get<AdminServiceArea[]>("/admin/service-areas", { params: { cityId } });
  return data;
}

export async function createServiceArea(
  cityId: string,
  name: string,
  pincode: string,
  displayOrder: number,
): Promise<AdminServiceArea> {
  const { data } = await apiClient.post<AdminServiceArea>("/admin/service-areas", { cityId, name, pincode, displayOrder });
  return data;
}

export async function updateServiceArea(
  id: string,
  name: string,
  pincode: string,
  displayOrder: number,
  active: boolean,
): Promise<AdminServiceArea> {
  const { data } = await apiClient.put<AdminServiceArea>(`/admin/service-areas/${id}`, {
    name,
    pincode,
    displayOrder,
    active,
  });
  return data;
}
