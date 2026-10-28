import { apiClient } from "./client";
import { downloadBlob } from "../lib/download";

export type InvoiceStatus = "DRAFT" | "FINAL" | "CANCELLED";

export interface InvoiceLineItemRequest {
  variantId: string;
  quantity: number;
  unitPrice: number;
}

export interface InvoiceLineItemResponse {
  variantId: string;
  quantity: number;
  unitPrice: number;
  gstRate: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  lineTotal: number;
}

export interface InvoiceRequest {
  customerId: string;
  invoiceDate: string;
  placeOfSupplyStateCode?: string | null;
  lineItems: InvoiceLineItemRequest[];
}

export interface InvoiceResponse {
  id: string;
  customerId: string;
  invoiceDisplayNumber: string;
  invoiceDate: string;
  placeOfSupplyStateCode: string;
  subtotal: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
  status: InvoiceStatus;
  lineItems: InvoiceLineItemResponse[];
}

export async function listInvoices(): Promise<InvoiceResponse[]> {
  const { data } = await apiClient.get<InvoiceResponse[]>("/invoices");
  return data;
}

export async function createInvoice(request: InvoiceRequest): Promise<InvoiceResponse> {
  const { data } = await apiClient.post<InvoiceResponse>("/invoices", request);
  return data;
}

export async function downloadInvoicePdf(id: string, invoiceDisplayNumber: string): Promise<void> {
  const response = await apiClient.get(`/invoices/${id}/pdf`, { responseType: "blob" });
  const disposition = response.headers["content-disposition"] as string | undefined;
  const filename = disposition?.match(/filename="?([^"]+)"?/)?.[1] ?? `${invoiceDisplayNumber.replace(/\//g, "-")}.pdf`;
  downloadBlob(filename, response.data as Blob);
}
