import { apiClient } from "./client";
import type { BulkImportResult } from "./bulkImport";

export interface VariantRequest {
  sku: string;
  barcode?: string | null;
  attributes?: Record<string, string>;
  purchasePrice: number;
  sellingPrice: number;
  hsnCode?: string | null;
  gstRate?: number | null;
  uom?: string | null;
  reorderThreshold?: number;
}

export interface VariantResponse {
  id: string;
  sku: string;
  barcode: string | null;
  attributes: Record<string, string>;
  purchasePrice: number;
  sellingPrice: number;
  hsnCode: string | null;
  gstRate: number | null;
  uom: string | null;
  effectiveHsnCode: string | null;
  effectiveGstRate: number;
  effectiveUom: string;
  currentStock: number;
  reorderThreshold: number;
  active: boolean;
}

export interface ProductRequest {
  name: string;
  brand?: string | null;
  description?: string | null;
  categoryId?: string | null;
  defaultHsnCode?: string | null;
  defaultGstRate: number;
  defaultUom: string;
  fabric?: string | null;
  careInstructions?: string | null;
  includedItems?: string | null;
  variants: VariantRequest[];
}

export interface ProductUpdateRequest {
  name: string;
  brand?: string | null;
  description?: string | null;
  categoryId?: string | null;
  defaultHsnCode?: string | null;
  defaultGstRate: number;
  defaultUom: string;
  fabric?: string | null;
  careInstructions?: string | null;
  includedItems?: string | null;
}

export interface ProductResponse {
  id: string;
  name: string;
  brand: string | null;
  description: string | null;
  categoryId: string | null;
  defaultHsnCode: string | null;
  defaultGstRate: number;
  defaultUom: string;
  fabric: string | null;
  careInstructions: string | null;
  includedItems: string | null;
  variants: VariantResponse[];
}

export interface ProductVariantImageResponse {
  id: string;
  imageUrl: string;
  thumbnailUrl: string;
  width: number;
  height: number;
  displayOrder: number;
}

export interface VariantScanResponse {
  productId: string;
  productName: string;
  variantId: string;
  sku: string;
  barcode: string;
  attributes: Record<string, string>;
  sellingPrice: number;
  effectiveGstRate: number;
  effectiveHsnCode: string | null;
  currentStock: number;
}

export async function listProducts(): Promise<ProductResponse[]> {
  const { data } = await apiClient.get<ProductResponse[]>("/products");
  return data;
}

export async function getProduct(id: string): Promise<ProductResponse> {
  const { data } = await apiClient.get<ProductResponse>(`/products/${id}`);
  return data;
}

export async function createProduct(request: ProductRequest): Promise<ProductResponse> {
  const { data } = await apiClient.post<ProductResponse>("/products", request);
  return data;
}

export async function updateProduct(id: string, request: ProductUpdateRequest): Promise<ProductResponse> {
  const { data } = await apiClient.put<ProductResponse>(`/products/${id}`, request);
  return data;
}

export async function bulkCreateProducts(requests: ProductRequest[]): Promise<BulkImportResult> {
  const { data } = await apiClient.post<BulkImportResult>("/products/bulk", requests);
  return data;
}

export async function addVariant(productId: string, request: VariantRequest): Promise<ProductResponse> {
  const { data } = await apiClient.post<ProductResponse>(`/products/${productId}/variants`, request);
  return data;
}

export async function updateVariant(
  productId: string,
  variantId: string,
  request: VariantRequest,
): Promise<ProductResponse> {
  const { data } = await apiClient.put<ProductResponse>(`/products/${productId}/variants/${variantId}`, request);
  return data;
}

export async function deleteVariant(productId: string, variantId: string): Promise<ProductResponse> {
  const { data } = await apiClient.delete<ProductResponse>(`/products/${productId}/variants/${variantId}`);
  return data;
}

export async function scanByBarcode(barcode: string): Promise<VariantScanResponse> {
  const { data } = await apiClient.get<VariantScanResponse>(`/products/variants/by-barcode/${encodeURIComponent(barcode)}`);
  return data;
}

export async function listVariantImages(
  productId: string,
  variantId: string,
): Promise<ProductVariantImageResponse[]> {
  const { data } = await apiClient.get<ProductVariantImageResponse[]>(
    `/products/${productId}/variants/${variantId}/images`,
  );
  return data;
}

export async function uploadVariantImage(
  productId: string,
  variantId: string,
  file: File,
): Promise<ProductVariantImageResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<ProductVariantImageResponse>(
    `/products/${productId}/variants/${variantId}/images`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data;
}

export async function deleteVariantImage(productId: string, variantId: string, imageId: string): Promise<void> {
  await apiClient.delete(`/products/${productId}/variants/${variantId}/images/${imageId}`);
}

export interface ProductImageRules {
  profile: "COMPACT" | "DETAIL";
  minWidth: number;
  minHeight: number;
  minAspectRatio: number;
  maxAspectRatio: number;
}

/** The photo limits for the signed-in vendor's shop (they differ by the shop category's image profile). */
export async function getProductImageRules(): Promise<ProductImageRules> {
  const { data } = await apiClient.get<ProductImageRules>("/product-image-rules");
  return data;
}
