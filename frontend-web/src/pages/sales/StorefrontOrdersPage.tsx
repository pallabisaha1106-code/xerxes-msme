import { useEffect, useState } from "react";
import { Button, Card, Dialog, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as storefrontApi from "../../api/storefrontApi";
import type { StorefrontOrderLineItemResponse, StorefrontOrderResponse, StorefrontOrderStatus } from "../../api/storefrontApi";

function statusTag(status: StorefrontOrderStatus) {
  if (status === "CONFIRMED") return <Tag severity="success">CONFIRMED</Tag>;
  if (status === "REJECTED") return <Tag severity="danger">REJECTED</Tag>;
  return <Tag severity="warning">NEW</Tag>;
}

const STATUS_FILTER_OPTIONS = [
  { label: "All orders", value: "" },
  { label: "New", value: "NEW" },
  { label: "Confirmed", value: "CONFIRMED" },
  { label: "Rejected", value: "REJECTED" },
];

function LineItemsTable({ lineItems }: { lineItems: StorefrontOrderLineItemResponse[] }) {
  const columns: Column<StorefrontOrderLineItemResponse>[] = [
    { field: "productName", header: "Product / SKU", body: (li) => `${li.productName} - ${li.sku}` },
    { field: "quantity", header: "Quantity" },
    { field: "unitPrice", header: "Unit Price", body: (li) => `₹${li.unitPrice.toFixed(2)}` },
    { field: "lineTotal", header: "Line Total", body: (li) => `₹${li.lineTotal.toFixed(2)}` },
  ];

  return <Table data={lineItems} columns={columns} rowKey={(li) => li.variantId} emptyMessage="No line items." />;
}

export function StorefrontOrdersPage() {
  const toast = useToast();
  const [orders, setOrders] = useState<StorefrontOrderResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<StorefrontOrderStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState<StorefrontOrderResponse | null>(null);
  const [rejectTarget, setRejectTarget] = useState<StorefrontOrderResponse | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [actioningId, setActioningId] = useState<string | null>(null);

  async function loadOrders() {
    setLoading(true);
    try {
      const list = await storefrontApi.listOrders(statusFilter || undefined);
      setOrders(list);
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load orders", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  function applyUpdatedOrder(order: StorefrontOrderResponse) {
    setOrders((current) => current.map((o) => (o.id === order.id ? order : o)));
    setSelectedOrder((current) => (current?.id === order.id ? order : current));
  }

  async function handleConfirm(order: StorefrontOrderResponse) {
    setActioningId(order.id);
    try {
      const updated = await storefrontApi.confirmOrder(order.id);
      applyUpdatedOrder(updated);
      toast.show({
        severity: "success",
        summary: "Order confirmed",
        detail: `Invoice created for ${order.customerName}.`,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not confirm order", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  async function handleConfirmReject() {
    if (!rejectTarget) return;
    setActioningId(rejectTarget.id);
    try {
      const updated = await storefrontApi.rejectOrder(rejectTarget.id, rejectReason);
      applyUpdatedOrder(updated);
      toast.show({ severity: "success", summary: "Order rejected", detail: rejectTarget.customerName });
      setRejectTarget(null);
      setRejectReason("");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not reject order", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  const columns: Column<StorefrontOrderResponse>[] = [
    {
      field: "customerName",
      header: "Customer",
      body: (o) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => setSelectedOrder(o)}>
          {o.customerName}
        </Button>
      ),
    },
    { field: "customerPhone", header: "Phone" },
    { field: "createdAt", header: "Placed", body: (o) => new Date(o.createdAt).toLocaleString() },
    { field: "subtotal", header: "Subtotal", body: (o) => `₹${o.subtotal.toFixed(2)}` },
    { field: "status", header: "Status", body: (o) => statusTag(o.status) },
    {
      field: "actions",
      header: "",
      body: (o) =>
        o.status === "NEW" ? (
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              size="sm"
              severity="success"
              loading={actioningId === o.id}
              onClick={() => handleConfirm(o)}
            >
              Confirm
            </Button>
            <Button
              size="sm"
              variant="outlined"
              severity="danger"
              disabled={actioningId === o.id}
              onClick={() => setRejectTarget(o)}
            >
              Reject
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <Card
      title="Storefront Orders"
      actions={
        <Select
          id="status-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StorefrontOrderStatus | "")}
          options={STATUS_FILTER_OPTIONS}
        />
      }
    >
      <Table
        data={orders}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No storefront orders yet. Guest orders placed on a published storefront will show up here."
      />

      <Dialog
        visible={selectedOrder != null}
        onHide={() => setSelectedOrder(null)}
        title={selectedOrder ? `Order — ${selectedOrder.customerName}` : "Order"}
        width={900}
      >
        {selectedOrder && (
          <>
            <div className="form-row">
              <p>
                <strong>Phone:</strong> {selectedOrder.customerPhone}
              </p>
              <p>
                <strong>Email:</strong> {selectedOrder.customerEmail ?? "-"}
              </p>
            </div>
            <p>
              <strong>Address:</strong>{" "}
              {[selectedOrder.addressLine1, selectedOrder.addressLine2, selectedOrder.city, selectedOrder.pincode]
                .filter(Boolean)
                .join(", ") || "-"}
            </p>
            {selectedOrder.notes && (
              <p>
                <strong>Notes:</strong> {selectedOrder.notes}
              </p>
            )}
            {selectedOrder.status === "REJECTED" && selectedOrder.rejectionReason && (
              <p>
                <strong>Rejection reason:</strong> {selectedOrder.rejectionReason}
              </p>
            )}
            <LineItemsTable lineItems={selectedOrder.lineItems} />
          </>
        )}
      </Dialog>

      <Dialog
        visible={rejectTarget != null}
        onHide={() => setRejectTarget(null)}
        title="Reject Order"
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setRejectTarget(null)}>
              Cancel
            </Button>
            <Button severity="danger" loading={actioningId === rejectTarget?.id} onClick={handleConfirmReject}>
              Reject
            </Button>
          </>
        }
      >
        <p>
          Reject the order from <strong>{rejectTarget?.customerName}</strong>?
        </p>
        <Input
          id="reject-reason"
          label="Reason (optional)"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Dialog>
    </Card>
  );
}
