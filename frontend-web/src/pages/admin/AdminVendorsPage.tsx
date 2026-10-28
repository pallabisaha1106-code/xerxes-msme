import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import * as adminApi from "../../api/adminApi";
import type { AdminTenantResponse } from "../../api/adminApi";

/** What a shop sells and earns is Super Admin's alone: a partner's list leaves these columns out. */
const SALES_FIELDS = ["customerCount", "invoiceCount", "storefrontOrderCount"];

export function AdminVendorsPage() {
  const { user } = useAuth();
  const isPartner = user?.role === "PARTNER";
  const toast = useToast();
  const navigate = useNavigate();
  const [vendors, setVendors] = useState<AdminTenantResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        setVendors(await adminApi.listVendors());
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load vendors", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const columns: Column<AdminTenantResponse>[] = [
    {
      field: "businessName",
      header: "Business",
      sortable: true,
      body: (v) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => navigate(`/admin/vendors/${v.id}`)}>
          {v.businessName}
        </Button>
      ),
    },
    { field: "email", header: "Email" },
    { field: "phone", header: "Phone", body: (v) => v.phone ?? "-" },
    { field: "partnerName", header: "Partner", body: (v) => (v.partnerName ? `${v.partnerName}${v.partnerActive === false ? " (inactive)" : ""}` : "-") },
    { field: "subscriptionPlan", header: "Plan" },
    { field: "productCount", header: "Products" },
    { field: "customerCount", header: "Customers" },
    { field: "invoiceCount", header: "Invoices" },
    { field: "storefrontOrderCount", header: "Storefront Orders" },
    {
      field: "active",
      header: "Status",
      body: (v) => (v.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="danger">SUSPENDED</Tag>),
    },
    {
      field: "createdAt",
      header: "Signed Up",
      sortable: true,
      body: (v) => new Date(v.createdAt).toLocaleDateString(),
    },
  ];

  return (
    <Card title="Vendors">
      <Table
        data={vendors}
        columns={isPartner ? columns.filter((c) => !SALES_FIELDS.includes(String(c.field))) : columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage={isPartner ? "No shops are assigned to you yet." : "No vendors have registered yet."}
      />
    </Card>
  );
}
