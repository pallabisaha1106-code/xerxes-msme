import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Card, Icon, Table, Tag, useToast, type Column, type IconName } from "../ui";
import { extractErrorMessage } from "../api/client";
import * as dashboardApi from "../api/dashboardApi";
import type {
  DashboardResponse,
  InvoiceStatus,
  RecentInvoiceResponse,
  TopProductResponse,
} from "../api/dashboardApi";
import { PartnerContactCard } from "./PartnerContactCard";
import "./dashboard.css";

function statusTag(status: InvoiceStatus) {
  if (status === "FINAL") return <Tag severity="success">FINAL</Tag>;
  if (status === "CANCELLED") return <Tag severity="danger">CANCELLED</Tag>;
  return <Tag severity="secondary">DRAFT</Tag>;
}

function formatCurrency(value: number): string {
  return `₹${value.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function StatTile({
  label,
  value,
  icon,
  linkTo,
  warning,
}: {
  label: string;
  value: string;
  icon: IconName;
  linkTo?: string;
  warning?: boolean;
}) {
  const content = (
    <>
      <div className={`stat-tile-icon${warning ? " severity-warning" : ""}`}>
        <Icon name={icon} size={22} />
      </div>
      <div>
        <div className="stat-tile-label">{label}</div>
        <div className="stat-tile-value">{value}</div>
      </div>
    </>
  );

  if (linkTo) {
    return (
      <Link to={linkTo} className="stat-tile">
        {content}
      </Link>
    );
  }
  return <div className="stat-tile">{content}</div>;
}

export function DashboardPage() {
  const toast = useToast();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi
      .getDashboard()
      .then(setData)
      .catch((err) => {
        toast.show({ severity: "error", summary: "Failed to load dashboard", detail: extractErrorMessage(err) });
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const topProductColumns: Column<TopProductResponse>[] = [
    { field: "productName", header: "Product", body: (p) => `${p.productName} - ${p.sku}` },
    { field: "quantitySold", header: "Qty Sold" },
    { field: "revenue", header: "Revenue", body: (p) => formatCurrency(p.revenue) },
  ];

  const recentInvoiceColumns: Column<RecentInvoiceResponse>[] = [
    { field: "invoiceDisplayNumber", header: "Invoice #" },
    { field: "customerName", header: "Customer" },
    { field: "invoiceDate", header: "Date" },
    { field: "totalAmount", header: "Total", body: (i) => formatCurrency(i.totalAmount) },
    { field: "status", header: "Status", body: (i) => statusTag(i.status) },
  ];

  if (loading || !data) {
    return (
      <Card title="Dashboard">
        <p style={{ color: "var(--text-color-secondary)", margin: 0 }}>Loading...</p>
      </Card>
    );
  }

  return (
    <div>
      <PartnerContactCard />
      <div className="dashboard-tiles">
        <StatTile label="Inventory Value" value={formatCurrency(data.inventoryValue)} icon="package" />
        <StatTile
          label="Low Stock Items"
          value={String(data.lowStockCount)}
          icon="alertCircle"
          linkTo="/stock"
          warning={data.lowStockCount > 0}
        />
        <StatTile label="Today's Sales" value={formatCurrency(data.salesToday)} icon="receipt" />
        <StatTile label="This Week's Sales" value={formatCurrency(data.salesThisWeek)} icon="receipt" />
        <StatTile label="This Month's Sales" value={formatCurrency(data.salesThisMonth)} icon="receipt" />
        <StatTile label="Products" value={String(data.totalProducts)} icon="box" linkTo="/products" />
        <StatTile label="Customers" value={String(data.totalCustomers)} icon="users" linkTo="/customers" />
        <StatTile label="Vendors" value={String(data.totalVendors)} icon="truck" linkTo="/vendors" />
      </div>

      <div className="dashboard-section">
        <Card title="Top Selling Products (Last 30 Days)">
          <Table
            data={data.topProducts}
            columns={topProductColumns}
            rowKey={(row) => row.variantId}
            emptyMessage="No sales recorded in the last 30 days."
          />
        </Card>
      </div>

      <div className="dashboard-section">
        <Card title="Recent Invoices" actions={<Link to="/invoices">View All</Link>}>
          <Table
            data={data.recentInvoices}
            columns={recentInvoiceColumns}
            rowKey={(row) => row.id}
            emptyMessage="No invoices yet."
          />
        </Card>
      </div>
    </div>
  );
}
