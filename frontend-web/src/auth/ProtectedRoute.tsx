import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

/**
 * mode="admin" gates the platform shell to SUPER_ADMIN and PARTNER (a partner only sees the screens the menu and the
 * server give them), redirecting anyone else back to the tenant dashboard. mode="superAdmin" is for the screens only
 * Super Admin may open: a partner is sent back to the screens they share. mode="tenant" (default) is the mirror image:
 * a SUPER_ADMIN or PARTNER visiting a tenant route is redirected to the admin shell rather than seeing an
 * empty/nonsensical dashboard for the sentinel platform tenant they technically belong to.
 */
export function ProtectedRoute({
  children,
  mode = "tenant",
}: {
  children: ReactNode;
  mode?: "admin" | "superAdmin" | "tenant";
}) {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const isPartner = user?.role === "PARTNER";
  if (mode === "admin" && !isSuperAdmin && !isPartner) {
    return <Navigate to="/" replace />;
  }
  if (mode === "superAdmin" && !isSuperAdmin) {
    return <Navigate to={isPartner ? "/admin" : "/"} replace />;
  }
  if (mode === "tenant" && (isSuperAdmin || isPartner)) {
    return <Navigate to="/admin" replace />;
  }
  return <>{children}</>;
}
