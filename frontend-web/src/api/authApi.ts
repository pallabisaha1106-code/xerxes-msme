import { apiClient } from "./client";

export interface RegisterRequest {
  businessName: string;
  gstin?: string;
  stateCode: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  pincode?: string;
  phone?: string;
  ownerName: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tenantId: string;
  userId: string;
  email: string;
  role: "OWNER" | "STAFF" | "SUPER_ADMIN" | "PARTNER";
  businessName: string;
}

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/auth/register", request);
  return data;
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/auth/login", request);
  return data;
}

/**
 * Takes the token explicitly rather than relying on the shared request
 * interceptor's localStorage read - axios runs interceptors as a microtask,
 * so if the caller clears localStorage synchronously right after firing this
 * (as AuthContext's logout() does, to feel instant), the interceptor would
 * otherwise pick up no token at all and the real session on the server
 * would never actually get revoked.
 */
export async function logout(token: string): Promise<void> {
  await apiClient.post("/auth/logout", undefined, { headers: { Authorization: `Bearer ${token}` } });
}
