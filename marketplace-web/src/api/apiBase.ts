/**
 * Base URL every backend API call (and /media/** photo URL) is resolved
 * against. Empty by default, so requests stay relative - Vite's dev proxy
 * (vite.config.ts) forwards those to localhost:8080 during local dev.
 * VITE_API_BASE_URL (set in .env.production, or any .env.* file) points a
 * built app at a real backend - e.g. the deployed Cloud Run URL - with no
 * proxy involved.
 */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}
