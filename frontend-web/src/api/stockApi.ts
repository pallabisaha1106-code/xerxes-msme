import { apiClient } from "./client";

export type MovementType = "PURCHASE" | "SALE" | "ADJUSTMENT" | "PURCHASE_RETURN" | "SALE_RETURN";

export interface StockLedgerResponse {
  id: string;
  variantId: string;
  movementType: MovementType;
  quantityChange: number;
  balanceAfter: number;
  referenceType: string | null;
  referenceId: string | null;
  remarks: string | null;
  movementDate: string;
}

export interface AdjustmentRequest {
  signedDelta: number;
  remarks?: string | null;
}

export interface LowStockResponse {
  variantId: string;
  sku: string;
  currentStock: number;
  reorderThreshold: number;
}

export async function getLedger(variantId: string): Promise<StockLedgerResponse[]> {
  const { data } = await apiClient.get<StockLedgerResponse[]>(`/stock/variants/${variantId}/ledger`);
  return data;
}

export async function recordAdjustment(
  variantId: string,
  request: AdjustmentRequest,
): Promise<StockLedgerResponse> {
  const { data } = await apiClient.post<StockLedgerResponse>(
    `/stock/variants/${variantId}/adjustments`,
    request,
  );
  return data;
}

export async function getLowStock(): Promise<LowStockResponse[]> {
  const { data } = await apiClient.get<LowStockResponse[]>("/stock/low-stock");
  return data;
}
