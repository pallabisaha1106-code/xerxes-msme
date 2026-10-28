import { apiClient } from "./client";
import type { BulkImportResult } from "./bulkImport";

export interface VendorRequest {
  name: string;
  gstin?: string | null;
  stateCode?: string | null;
  contactPerson?: string | null;
  phone?: string | null;
  email?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  pincode?: string | null;
}

export interface VendorResponse {
  id: string;
  name: string;
  gstin: string | null;
  stateCode: string | null;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  pincode: string | null;
}

export async function listVendors(): Promise<VendorResponse[]> {
  const { data } = await apiClient.get<VendorResponse[]>("/vendors");
  return data;
}

export async function createVendor(request: VendorRequest): Promise<VendorResponse> {
  const { data } = await apiClient.post<VendorResponse>("/vendors", request);
  return data;
}

export async function bulkCreateVendors(requests: VendorRequest[]): Promise<BulkImportResult> {
  const { data } = await apiClient.post<BulkImportResult>("/vendors/bulk", requests);
  return data;
}
