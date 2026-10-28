import { API_BASE_URL } from "../api/apiBase";

/**
 * The backend always returns photo URLs as paths relative to itself (see
 * TenantStorageService), e.g. "/media/<tenantId>/products/...". Resolve
 * those against API_BASE_URL so they still load once this app and the
 * backend are on different origins (no proxy in production - see apiBase.ts).
 */
export function resolveImagePath(path: string): string {
  return path.startsWith("/media/") ? `${API_BASE_URL}${path}` : path;
}
