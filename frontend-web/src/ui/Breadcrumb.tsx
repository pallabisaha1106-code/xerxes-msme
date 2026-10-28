import { Link } from "react-router-dom";
import { Icon } from "./Icon";

export interface BreadcrumbItem {
  label: string;
  to?: string;
}

export function Breadcrumb({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav className="breadcrumb" aria-label="Breadcrumb">
      <Link to="/" className="breadcrumb-home">
        <Icon name="home" size={14} />
      </Link>
      {items.map((item, index) => (
        <span key={index} className="breadcrumb-item">
          <Icon name="chevronRight" size={12} className="breadcrumb-separator" />
          {item.to ? <Link to={item.to}>{item.label}</Link> : <span>{item.label}</span>}
        </span>
      ))}
    </nav>
  );
}
