import { apiClient } from "./client";
import type { BulkImportResult } from "./bulkImport";

export interface CategoryResponse {
  id: string;
  name: string;
  parentCategoryId: string | null;
}

export interface CategoryRequest {
  name: string;
  parentCategoryId?: string | null;
}

export async function listCategories(): Promise<CategoryResponse[]> {
  const { data } = await apiClient.get<CategoryResponse[]>("/categories");
  return data;
}

export async function createCategory(request: CategoryRequest): Promise<CategoryResponse> {
  const { data } = await apiClient.post<CategoryResponse>("/categories", request);
  return data;
}

export async function bulkCreateCategories(requests: CategoryRequest[]): Promise<BulkImportResult> {
  const { data } = await apiClient.post<BulkImportResult>("/categories/bulk", requests);
  return data;
}
