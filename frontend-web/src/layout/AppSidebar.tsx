import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Icon } from "../ui";
import { navSections } from "./navConfig";
import { usePendingAppointments } from "./usePendingAppointments";

/** The number of appointment requests waiting for an answer. */
function WaitingBadge({ count }: { count: number }) {
  return (
    <span className="sidebar-badge" aria-label={`${count} waiting`}>
      {count}
    </span>
  );
}

export function AppSidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const pendingAppointments = usePendingAppointments(location.pathname);
  const activeSection = navSections.find((section) =>
    section.items.some((item) => item.path === location.pathname),
  );
  const [openSection, setOpenSection] = useState<string | null>(activeSection?.label ?? null);

  return (
    <aside className="app-sidebar">
      <button
        type="button"
        className={`sidebar-item sidebar-top-item ${location.pathname === "/" ? "sidebar-item-active" : ""}`}
        onClick={() => navigate("/")}
      >
        <Icon name="home" size={16} />
        <span>Dashboard</span>
      </button>

      {navSections.map((section) => {
        const isOpen = openSection === section.label;
        return (
          <div key={section.label} className="sidebar-section">
            <button
              type="button"
              className="sidebar-item sidebar-section-header"
              onClick={() => setOpenSection(isOpen ? null : section.label)}
            >
              <Icon name={section.icon} size={16} />
              <span>{section.label}</span>
              {section.label === "Appointments" && pendingAppointments > 0 && <WaitingBadge count={pendingAppointments} />}
              <Icon
                name="chevronDown"
                size={14}
                className="sidebar-chevron"
                style={{ transform: isOpen ? undefined : "rotate(-90deg)" }}
              />
            </button>
            {isOpen && (
              <div className="sidebar-subitems">
                {section.items.map((item) => (
                  <button
                    key={item.path}
                    type="button"
                    className={`sidebar-item sidebar-subitem ${
                      location.pathname === item.path ? "sidebar-item-active" : ""
                    }`}
                    onClick={() => navigate(item.path)}
                  >
                    <Icon name={item.icon} size={15} />
                    <span>{item.label}</span>
                    {item.path === "/appointments" && pendingAppointments > 0 && <WaitingBadge count={pendingAppointments} />}
                  </button>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </aside>
  );
}
