import { apiClient } from "./client";

export type StorefrontOrderStatus = "NEW" | "CONFIRMED" | "REJECTED";

export interface StorefrontOrderLineItemResponse {
  variantId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface StorefrontOrderResponse {
  id: string;
  status: StorefrontOrderStatus;
  customerName: string;
  customerPhone: string;
  customerEmail?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  pincode?: string | null;
  stateCode?: string | null;
  notes?: string | null;
  subtotal: number;
  rejectionReason?: string | null;
  confirmedInvoiceId?: string | null;
  createdAt: string;
  lineItems: StorefrontOrderLineItemResponse[];
}

export async function listOrders(status?: StorefrontOrderStatus): Promise<StorefrontOrderResponse[]> {
  const { data } = await apiClient.get<StorefrontOrderResponse[]>("/storefront/orders", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function confirmOrder(id: string): Promise<StorefrontOrderResponse> {
  const { data } = await apiClient.post<StorefrontOrderResponse>(`/storefront/orders/${id}/confirm`);
  return data;
}

export async function rejectOrder(id: string, reason?: string): Promise<StorefrontOrderResponse> {
  const { data } = await apiClient.post<StorefrontOrderResponse>(`/storefront/orders/${id}/reject`, {
    reason: reason || undefined,
  });
  return data;
}
