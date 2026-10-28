import { apiClient } from "./client";

export interface PurchaseLineItemRequest {
  variantId: string;
  quantity: number;
  unitPrice: number;
}

export interface PurchaseLineItemResponse {
  variantId: string;
  quantity: number;
  unitPrice: number;
  gstRate: number;
  lineTotal: number;
}

export interface PurchaseRequest {
  vendorId: string;
  referenceNumber?: string | null;
  purchaseDate: string;
  lineItems: PurchaseLineItemRequest[];
}

export interface PurchaseResponse {
  id: string;
  vendorId: string;
  referenceNumber: string | null;
  purchaseDate: string;
  subtotal: number;
  totalTax: number;
  totalAmount: number;
  lineItems: PurchaseLineItemResponse[];
}

export async function listPurchases(): Promise<PurchaseResponse[]> {
  const { data } = await apiClient.get<PurchaseResponse[]>("/purchases");
  return data;
}

export async function createPurchase(request: PurchaseRequest): Promise<PurchaseResponse> {
  const { data } = await apiClient.post<PurchaseResponse>("/purchases", request);
  return data;
}
