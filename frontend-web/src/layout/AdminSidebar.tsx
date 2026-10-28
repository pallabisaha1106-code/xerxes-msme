import { useLocation, useNavigate } from "react-router-dom";
import { Icon, type IconName } from "../ui";
import { useAuth } from "../auth/AuthContext";

interface AdminNavItem {
  label: string;
  icon: IconName;
  path: string;
}

const ADMIN_NAV: AdminNavItem[] = [
  { label: "Vendors", icon: "users", path: "/admin" },
  { label: "Marketplace Listings", icon: "search", path: "/admin/marketplace/listings" },
  { label: "Feed Posts", icon: "activity", path: "/admin/feed-posts" },
  { label: "Marketplace Categories", icon: "grid", path: "/admin/marketplace-categories" },
  { label: "Starter Catalog", icon: "tag", path: "/admin/default-catalog" },
  { label: "Service States", icon: "sitemap", path: "/admin/service-states" },
  { label: "Service Locations", icon: "sitemap", path: "/admin/service-locations" },
  { label: "Advertisements", icon: "eye", path: "/admin/advertisements" },
  { label: "Partner Applications", icon: "users", path: "/admin/partner-applications" },
  { label: "Local Partners", icon: "users", path: "/admin/partners" },
  { label: "Shops Needing a Partner", icon: "users", path: "/admin/partner-waiting" },
];

/** What a local partner sees: the three review screens they share with Super Admin, plus their own profile. */
const PARTNER_NAV: AdminNavItem[] = [
  ...ADMIN_NAV.slice(0, 3),
  { label: "My profile", icon: "users", path: "/admin/profile" },
];

/** Flat, admin-scoped counterpart to AppSidebar - reuses the same .app-sidebar/.sidebar-item
 * classes for visual consistency with the tenant app, but no accordion sections: four
 * top-level destinations don't need grouping the way the tenant's larger nav does. */
export function AdminSidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const items = user?.role === "PARTNER" ? PARTNER_NAV : ADMIN_NAV;

  return (
    <aside className="app-sidebar">
      {items.map((item) => (
        <button
          key={item.path}
          type="button"
          className={`sidebar-item sidebar-top-item ${location.pathname === item.path ? "sidebar-item-active" : ""}`}
          onClick={() => navigate(item.path)}
        >
          <Icon name={item.icon} size={16} />
          <span>{item.label}</span>
        </button>
      ))}
    </aside>
  );
}
