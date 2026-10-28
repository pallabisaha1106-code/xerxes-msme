import { apiClient } from "./client";

export interface StarterProductOption {
  id: string;
  name: string;
  imageUrl: string | null;
}

/** One group of the Super Admin's starter catalogue (e.g. "Rice") with the products in it. */
export interface StarterCatalogGroup {
  id: string;
  name: string;
  products: StarterProductOption[];
}

// The catalogue is a public, rate-limited endpoint and about 200 products long, so it is fetched once per category
// per session: the Products page's button and the picker page share this one request.
const catalogByCategory = new Map<string, Promise<StarterCatalogGroup[]>>();

/** The starter catalogue for a marketplace category (an empty list when that category has none). */
export function listStarterCatalog(marketplaceCategoryId: string): Promise<StarterCatalogGroup[]> {
  let pending = catalogByCategory.get(marketplaceCategoryId);
  if (!pending) {
    pending = apiClient
      .get<StarterCatalogGroup[]>("/public/marketplace/default-catalog", { params: { marketplaceCategoryId } })
      .then((response) => response.data);
    catalogByCategory.set(marketplaceCategoryId, pending);
    // A failure is not remembered, so the next visit tries again.
    pending.catch(() => catalogByCategory.delete(marketplaceCategoryId));
  }
  return pending;
}

/** Adds the picked starter products to the logged-in vendor's catalogue, with no price or stock (all or nothing). */
export async function createStarterProducts(defaultProductIds: string[]): Promise<void> {
  await apiClient.post("/marketplace/starter-products", { defaultProductIds });
}
