/**
 * frontend-web's own origin - where an already-listed vendor logs in to manage
 * their shop. Same pattern as api/apiBase.ts's API_BASE_URL: falls back to
 * localhost:5173 (frontend-web's local dev port) so nothing breaks locally,
 * and VITE_DASHBOARD_URL (set in .env.production, or any .env.* file) points
 * a built app at the real deployed frontend-web origin instead.
 */
const DASHBOARD_URL = (import.meta.env.VITE_DASHBOARD_URL || "http://localhost:5173").replace(/\/$/, "");

export const DASHBOARD_LOGIN_URL = `${DASHBOARD_URL}/login`;
