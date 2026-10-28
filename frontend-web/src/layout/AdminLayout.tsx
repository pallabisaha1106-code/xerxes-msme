import { Outlet, useNavigate } from "react-router-dom";
import { Avatar, Icon, Popover } from "../ui";
import { useAuth } from "../auth/AuthContext";
import { AdminSidebar } from "./AdminSidebar";
import "./layout.css";

export function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const initials = (user?.email ?? "?").charAt(0).toUpperCase();

  function handleSignOut() {
    logout();
    navigate("/login");
  }

  return (
    <div className="app-shell">
      <header className="app-topbar">
        <div className="app-topbar-brand">
          <Icon name="grid" size={20} />
          <span className="app-topbar-title">{user?.role === "PARTNER" ? "Xerxes Local Partner" : "Xerxes Platform Admin"}</span>
        </div>
        <div className="app-topbar-user">
          {user && <span className="app-topbar-business">{user.email}</span>}
          <Popover
            align="right"
            trigger={({ onClick, ref }) => <Avatar ref={ref} label={initials} onClick={onClick} data-testid="user-menu-trigger" />}
          >
            <button type="button" className="popover-item" onClick={handleSignOut}>
              <Icon name="logOut" size={16} />
              Sign Out
            </button>
          </Popover>
        </div>
      </header>
      <div className="app-body">
        <AdminSidebar />
        <main className="app-content">
          <div className="app-content-inner">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
