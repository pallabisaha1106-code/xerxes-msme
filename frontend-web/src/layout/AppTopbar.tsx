import { useNavigate } from "react-router-dom";
import { Avatar, Icon, Popover } from "../ui";
import { useAuth } from "../auth/AuthContext";

export function AppTopbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const initials = (user?.email ?? "?").charAt(0).toUpperCase();

  function handleSignOut() {
    logout();
    navigate("/login");
  }

  return (
    <header className="app-topbar">
      <div className="app-topbar-brand">
        <Icon name="grid" size={20} />
        <span className="app-topbar-title">Xerxes MSME</span>
      </div>

      <div className="app-topbar-user">
        {user && (
          <span className="app-topbar-business">
            {user.businessName} <span className="app-topbar-role">({user.role})</span>
          </span>
        )}
        <Popover
          align="right"
          trigger={({ onClick, ref }) => (
            <Avatar
              ref={ref}
              label={initials}
              onClick={onClick}
              data-testid="user-menu-trigger"
            />
          )}
        >
          <button type="button" className="popover-item" onClick={handleSignOut}>
            <Icon name="logOut" size={16} />
            Sign Out
          </button>
        </Popover>
      </div>
    </header>
  );
}
