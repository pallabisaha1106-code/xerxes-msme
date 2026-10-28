import { Outlet } from "react-router-dom";
import { AppTopbar } from "./AppTopbar";
import { AppSidebar } from "./AppSidebar";
import { AppBreadcrumb } from "./AppBreadcrumb";
import { SubscriptionBanner } from "./SubscriptionBanner";
import "./layout.css";

export function AppLayout() {
  return (
    <div className="app-shell">
      <AppTopbar />
      <SubscriptionBanner />
      <div className="app-body">
        <AppSidebar />
        <main className="app-content">
          <div className="app-breadcrumb-bar">
            <AppBreadcrumb />
          </div>
          <div className="app-content-inner">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
