import { apiClient } from "./client";

export interface AdminDefaultProductCategory {
  id: string;
  marketplaceCategoryId: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminDefaultProduct {
  id: string;
  defaultProductCategoryId: string;
  name: string;
  displayOrder: number;
  active: boolean;
  imageUrl: string | null;
}

export interface ImportDefaultCatalogProduct {
  name: string;
  imageFileName?: string;
}

export interface ImportDefaultCatalogCategory {
  name: string;
  products: ImportDefaultCatalogProduct[];
}

export interface ImportDefaultCatalogManifest {
  marketplaceCategoryId: string;
  categories: ImportDefaultCatalogCategory[];
}

export interface ImportDefaultCatalogResult {
  categoriesCreated: number;
  categoriesUpdated: number;
  productsCreated: number;
  productsUpdated: number;
  imagesAttached: number;
  /** Photos left out of the import (e.g. too small), each with the reason. */
  skippedImages: string[];
}

export async function listDefaultCategories(marketplaceCategoryId: string): Promise<AdminDefaultProductCategory[]> {
  const { data } = await apiClient.get<AdminDefaultProductCategory[]>("/admin/default-product-categories", {
    params: { marketplaceCategoryId },
  });
  return data;
}

export async function createDefaultCategory(
  marketplaceCategoryId: string,
  name: string,
  displayOrder: number,
): Promise<AdminDefaultProductCategory> {
  const { data } = await apiClient.post<AdminDefaultProductCategory>("/admin/default-product-categories", {
    marketplaceCategoryId,
    name,
    displayOrder,
  });
  return data;
}

export async function updateDefaultCategory(
  id: string,
  name: string,
  displayOrder: number,
  active: boolean,
): Promise<AdminDefaultProductCategory> {
  const { data } = await apiClient.put<AdminDefaultProductCategory>(`/admin/default-product-categories/${id}`, {
    name,
    displayOrder,
    active,
  });
  return data;
}

export async function listDefaultProducts(defaultProductCategoryId: string): Promise<AdminDefaultProduct[]> {
  const { data } = await apiClient.get<AdminDefaultProduct[]>("/admin/default-products", {
    params: { defaultProductCategoryId },
  });
  return data;
}

export async function createDefaultProduct(
  defaultProductCategoryId: string,
  name: string,
  displayOrder: number,
): Promise<AdminDefaultProduct> {
  const { data } = await apiClient.post<AdminDefaultProduct>("/admin/default-products", {
    defaultProductCategoryId,
    name,
    displayOrder,
  });
  return data;
}

export async function updateDefaultProduct(
  id: string,
  name: string,
  displayOrder: number,
  active: boolean,
): Promise<AdminDefaultProduct> {
  const { data } = await apiClient.put<AdminDefaultProduct>(`/admin/default-products/${id}`, {
    name,
    displayOrder,
    active,
  });
  return data;
}

export async function uploadDefaultProductPhoto(id: string, file: File): Promise<AdminDefaultProduct> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<AdminDefaultProduct>(`/admin/default-products/${id}/photo`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

/**
 * Bulk-seeds/updates the starter catalog under one marketplace category in
 * one call, upserting by name - the app-facing replacement for hand-writing
 * a SQL migration every time the catalog needs to grow. `images` are matched
 * to products by their `imageFileName`.
 */
export async function importDefaultCatalog(
  manifest: ImportDefaultCatalogManifest,
  images: File[],
): Promise<ImportDefaultCatalogResult> {
  const formData = new FormData();
  formData.append("manifest", new Blob([JSON.stringify(manifest)], { type: "application/json" }));
  for (const file of images) {
    formData.append("images", file, file.name);
  }
  const { data } = await apiClient.post<ImportDefaultCatalogResult>("/admin/default-catalog/import", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}
