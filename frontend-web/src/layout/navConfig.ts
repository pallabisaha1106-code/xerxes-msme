import type { IconName } from "../ui";

export interface NavLeaf {
  label: string;
  icon: IconName;
  path: string;
}

export interface NavSection {
  label: string;
  icon: IconName;
  items: NavLeaf[];
}

export const navSections: NavSection[] = [
  {
    label: "Catalog",
    icon: "tag",
    items: [
      { label: "Categories", icon: "sitemap", path: "/categories" },
      { label: "Products", icon: "box", path: "/products" },
    ],
  },
  {
    label: "Inventory",
    icon: "package",
    items: [{ label: "Stock Ledger", icon: "activity", path: "/stock" }],
  },
  {
    label: "Purchases",
    icon: "truck",
    items: [
      { label: "Vendors", icon: "truck", path: "/vendors" },
      { label: "Purchase Entries", icon: "fileImport", path: "/purchases" },
    ],
  },
  {
    label: "Sales",
    icon: "receipt",
    items: [
      { label: "Quick Billing", icon: "barcode", path: "/billing/quick" },
      { label: "Customers", icon: "users", path: "/customers" },
      { label: "Invoices", icon: "receipt", path: "/invoices" },
    ],
  },
  {
    label: "Storefront",
    icon: "grid",
    items: [
      { label: "Site Builder", icon: "edit", path: "/storefront/builder" },
      { label: "Orders", icon: "inbox", path: "/storefront/orders" },
    ],
  },
  {
    label: "Appointments",
    icon: "calendar",
    items: [
      { label: "Requests", icon: "inbox", path: "/appointments" },
      { label: "Availability", icon: "calendar", path: "/appointments/availability" },
    ],
  },
  {
    label: "Marketplace",
    icon: "search",
    items: [
      { label: "My Listing", icon: "edit", path: "/marketplace/listing" },
      { label: "Feed Posts", icon: "activity", path: "/marketplace/feed" },
    ],
  },
  {
    label: "Settings",
    icon: "creditCard",
    items: [{ label: "Billing", icon: "creditCard", path: "/settings/billing" }],
  },
];

/** path -> [section label, page label], used to render the breadcrumb. */
export const routeBreadcrumbs: Record<string, [string, string]> = {
  "/": ["Home", "Dashboard"],
  "/categories": ["Catalog", "Categories"],
  "/products": ["Catalog", "Products"],
  "/products/new": ["Catalog", "New Product"],
  "/products/from-photos": ["Catalog", "Add from Photos"],
  "/products/from-catalogue": ["Catalog", "Add from Starter Catalogue"],
  "/stock": ["Inventory", "Stock Ledger"],
  "/vendors": ["Purchases", "Vendors"],
  "/purchases": ["Purchases", "Purchase Entries"],
  "/purchases/new": ["Purchases", "New Purchase Entry"],
  "/customers": ["Sales", "Customers"],
  "/invoices": ["Sales", "Invoices"],
  "/invoices/new": ["Sales", "New Invoice"],
  "/billing/quick": ["Sales", "Quick Billing"],
  "/storefront/builder": ["Storefront", "Site Builder"],
  "/storefront/orders": ["Storefront", "Orders"],
  "/appointments": ["Appointments", "Requests"],
  "/appointments/availability": ["Appointments", "Availability"],
  "/marketplace/listing": ["Marketplace", "My Listing"],
  "/marketplace/feed": ["Marketplace", "Feed Posts"],
  "/settings/billing": ["Settings", "Billing"],
};
