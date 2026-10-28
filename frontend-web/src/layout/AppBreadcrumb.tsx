import { useLocation } from "react-router-dom";
import { Breadcrumb } from "../ui";
import { routeBreadcrumbs } from "./navConfig";

export function AppBreadcrumb() {
  const location = useLocation();
  const isProductLabelsRoute = /^\/products\/[^/]+\/labels$/.test(location.pathname);
  const [section, page] = isProductLabelsRoute
    ? ["Catalog", "Print Labels"]
    : (routeBreadcrumbs[location.pathname] ?? ["", "Not Found"]);

  const items = section ? [{ label: section }, { label: page }] : [{ label: page }];

  return <Breadcrumb items={items} />;
}
