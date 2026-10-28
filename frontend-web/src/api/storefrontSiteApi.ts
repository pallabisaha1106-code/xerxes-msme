import { apiClient } from "./client";
import { downloadBlob } from "../lib/download";

export interface TemplateField {
  key: string;
  label: string;
  type: "text" | "textarea";
  defaultValue: string;
}

export interface TemplateSection {
  key: string;
  label: string;
  fields: TemplateField[];
}

export interface TemplateManifest {
  id: string;
  name: string;
  description: string;
  sections: TemplateSection[];
}

export interface StorefrontSettings {
  templateId: string;
  content: Record<string, string>;
  publishedAt: string | null;
  previewUrl: string | null;
}

export interface PublishResult {
  publishedAt: string;
  previewUrl: string;
}

export async function listTemplates(): Promise<TemplateManifest[]> {
  const { data } = await apiClient.get<TemplateManifest[]>("/storefront/templates");
  return data;
}

export async function getSettings(): Promise<StorefrontSettings> {
  const { data } = await apiClient.get<StorefrontSettings>("/storefront/settings");
  return data;
}

export async function updateSettings(templateId: string, content: Record<string, string>): Promise<StorefrontSettings> {
  const { data } = await apiClient.put<StorefrontSettings>("/storefront/settings", { templateId, content });
  return data;
}

export async function publish(): Promise<PublishResult> {
  const { data } = await apiClient.post<PublishResult>("/storefront/publish");
  return data;
}

export async function downloadZip(): Promise<void> {
  const response = await apiClient.get("/storefront/download", { responseType: "blob" });
  const disposition = response.headers["content-disposition"] as string | undefined;
  const filename = disposition?.match(/filename="?([^"]+)"?/)?.[1] ?? "storefront.zip";
  downloadBlob(filename, response.data as Blob);
}
