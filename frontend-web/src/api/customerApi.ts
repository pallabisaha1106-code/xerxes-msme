import { apiClient } from "./client";
import type { BulkImportResult } from "./bulkImport";

export interface CustomerRequest {
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

export interface CustomerResponse {
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

export async function listCustomers(): Promise<CustomerResponse[]> {
  const { data } = await apiClient.get<CustomerResponse[]>("/customers");
  return data;
}

export async function createCustomer(request: CustomerRequest): Promise<CustomerResponse> {
  const { data } = await apiClient.post<CustomerResponse>("/customers", request);
  return data;
}

export async function bulkCreateCustomers(requests: CustomerRequest[]): Promise<BulkImportResult> {
  const { data } = await apiClient.post<BulkImportResult>("/customers/bulk", requests);
  return data;
}
