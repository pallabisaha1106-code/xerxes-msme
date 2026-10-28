import { apiClient } from "./client";

export type InvoiceStatus = "DRAFT" | "FINAL" | "CANCELLED";

export interface TopProductResponse {
  variantId: string;
  productName: string;
  sku: string;
  quantitySold: number;
  revenue: number;
}

export interface RecentInvoiceResponse {
  id: string;
  invoiceDisplayNumber: string;
  customerName: string;
  invoiceDate: string;
  totalAmount: number;
  status: InvoiceStatus;
}

export interface DashboardResponse {
  inventoryValue: number;
  salesToday: number;
  salesThisWeek: number;
  salesThisMonth: number;
  totalProducts: number;
  totalCustomers: number;
  totalVendors: number;
  lowStockCount: number;
  topProducts: TopProductResponse[];
  recentInvoices: RecentInvoiceResponse[];
}

export async function getDashboard(): Promise<DashboardResponse> {
  const { data } = await apiClient.get<DashboardResponse>("/dashboard");
  return data;
}
