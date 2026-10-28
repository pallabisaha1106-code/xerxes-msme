import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from "../api/client";
import * as authApi from "../api/authApi";
import type { AuthResponse, LoginRequest, RegisterRequest } from "../api/authApi";

export interface CurrentUser {
  tenantId: string;
  userId: string;
  email: string;
  role: "OWNER" | "STAFF" | "SUPER_ADMIN" | "PARTNER";
  businessName: string;
}

interface AuthContextValue {
  user: CurrentUser | null;
  isAuthenticated: boolean;
  login: (request: LoginRequest) => Promise<CurrentUser>;
  register: (request: RegisterRequest) => Promise<CurrentUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toCurrentUser(response: AuthResponse): CurrentUser {
  return {
    tenantId: response.tenantId,
    userId: response.userId,
    email: response.email,
    role: response.role,
    businessName: response.businessName,
  };
}

function loadStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(loadStoredUser);

  const persist = useCallback((response: AuthResponse): CurrentUser => {
    localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
    const currentUser = toCurrentUser(response);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    setUser(currentUser);
    return currentUser;
  }, []);

  const login = useCallback(
    async (request: LoginRequest) => {
      const response = await authApi.login(request);
      return persist(response);
    },
    [persist],
  );

  const register = useCallback(
    async (request: RegisterRequest) => {
      const response = await authApi.register(request);
      return persist(response);
    },
    [persist],
  );

  const logout = useCallback(() => {
    // Read the token before clearing it - axios runs request interceptors as
    // a microtask, so if we cleared localStorage first, the logout call
    // would go out with no Authorization header and never actually revoke
    // the session server-side (see authApi.logout).
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      // Best-effort - local state is cleared either way, so don't block the
      // UI on the network call or let a failure stop the user signing out.
      authApi.logout(token).catch(() => {});
    }
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, register, logout }),
    [user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
