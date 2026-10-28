import axios from "axios";
import { apiUrl } from "./apiBase";

export const TOKEN_STORAGE_KEY = "xerxes_msme_token";
export const USER_STORAGE_KEY = "xerxes_msme_user";

export const apiClient = axios.create({
  baseURL: apiUrl("/api"),
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 means the token is missing, expired, or was revoked (logout on
// another tab, or the backend's 30-minute lifecycle - see AuthTokenService)
// - clear local state and send the user back to log in.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);

export interface ApiErrorBody {
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: string[];
}

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.data) {
    const body = error.response.data as ApiErrorBody;
    if (body.fieldErrors?.length) {
      return body.fieldErrors.join(", ");
    }
    if (body.message) {
      return body.message;
    }
  }
  return "Something went wrong. Please try again.";
}
