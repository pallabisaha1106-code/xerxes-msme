import { apiClient } from "./client";

export interface ProductCapacity {
  limit: number;
  used: number;
  remaining: number;
}

/**
 * How many more products the vendor's plan allows. The "add products from photos" screen uses it to show "N of M slots
 * left" and to stop a batch that can't fit. Returns null when it can't be read, in which case the screen simply
 * leaves that line out and the server's own limit still applies when each product is created.
 */
export async function getProductCapacity(): Promise<ProductCapacity | null> {
  try {
    const { data } = await apiClient.get<ProductCapacity>("/products/capacity");
    return data;
  } catch {
    return null;
  }
}

/** Gives a variant its own copy of every photo on another variant of the same product (one photo shared across sizes). */
export async function copyVariantImages(productId: string, variantId: string, sourceVariantId: string): Promise<void> {
  await apiClient.post(`/products/${productId}/variants/${variantId}/images/copy-from/${sourceVariantId}`);
}
