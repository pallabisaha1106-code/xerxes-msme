import { apiUrl } from "./apiBase";
import { toApiError } from "./apiError";

export interface StorefrontVariantDto {
  productId: string;
  productName: string;
  productDescription: string | null;
  variantId: string;
  sku: string;
  attributes: Record<string, string>;
  sellingPrice: number;
  imageUrls: string[];
  inStock: boolean;
  /** Smaller copies parallel to imageUrls; empty when none exist (e.g. a starter placeholder). */
  thumbnailUrls?: string[];
  /** True when imageUrls is only the starter-catalogue placeholder, not the vendor's own photo. */
  sampleImage?: boolean;
  /** Larger copies parallel to imageUrls; null where a photo has none. */
  zoomUrls?: (string | null)[];
  /** The vendor's own product category (e.g. "Milk & Dairy Products") - null when the product has none. */
  categoryId?: string | null;
  categoryName?: string | null;
}

export interface StorefrontOrderLineItemRequest {
  variantId: string;
  quantity: number;
}

export interface StorefrontOrderRequest {
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  pincode?: string;
  stateCode?: string;
  notes?: string;
  lineItems: StorefrontOrderLineItemRequest[];
}

export interface StorefrontOrderResponse {
  id: string;
  status: string;
  subtotal: number;
  createdAt: string;
}

export async function listStorefrontProducts(tenantId: string): Promise<StorefrontVariantDto[]> {
  const response = await fetch(apiUrl(`/api/public/storefront/${encodeURIComponent(tenantId)}/products`));
  if (!response.ok) throw await toApiError(response, "Could not load products");
  return response.json();
}

export async function submitStorefrontOrder(
  tenantId: string,
  request: StorefrontOrderRequest,
): Promise<StorefrontOrderResponse> {
  const response = await fetch(apiUrl(`/api/public/storefront/${encodeURIComponent(tenantId)}/orders`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw await toApiError(response, "Order submission failed");
  return response.json();
}
