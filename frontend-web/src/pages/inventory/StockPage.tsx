import { useEffect, useState, type FormEvent } from "react";
import { Button, Card, Dialog, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import * as stockApi from "../../api/stockApi";
import type { LowStockResponse, MovementType, StockLedgerResponse } from "../../api/stockApi";
import "./stock.css";

function movementTag(type: MovementType) {
  if (type === "PURCHASE" || type === "SALE_RETURN") return <Tag severity="success">{type}</Tag>;
  if (type === "SALE" || type === "PURCHASE_RETURN") return <Tag severity="danger">{type}</Tag>;
  return <Tag severity="secondary">{type}</Tag>;
}

function signedQuantity(value: number): string {
  return value > 0 ? `+${value}` : `${value}`;
}

export function StockPage() {
  const toast = useToast();
  const [lowStock, setLowStock] = useState<LowStockResponse[]>([]);
  const [variantOptions, setVariantOptions] = useState<{ label: string; value: string }[]>([]);
  const [loading, setLoading] = useState(true);

  const [selectedVariantId, setSelectedVariantId] = useState("");
  const [selectedSku, setSelectedSku] = useState("");
  const [ledger, setLedger] = useState<StockLedgerResponse[]>([]);
  const [ledgerLoading, setLedgerLoading] = useState(false);

  const [adjustDialogVisible, setAdjustDialogVisible] = useState(false);
  const [signedDelta, setSignedDelta] = useState("");
  const [remarks, setRemarks] = useState("");
  const [saving, setSaving] = useState(false);

  async function loadOverview() {
    setLoading(true);
    try {
      const [lowStockList, products] = await Promise.all([stockApi.getLowStock(), productApi.listProducts()]);
      setLowStock(lowStockList);
      const options = products.flatMap((product) =>
        product.variants.map((variant) => ({
          label: `${product.name} - ${variant.sku}`,
          value: variant.id,
        })),
      );
      setVariantOptions(options);
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load stock overview", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function viewLedger(variantId: string, sku: string) {
    setSelectedVariantId(variantId);
    setSelectedSku(sku);
    setLedgerLoading(true);
    try {
      setLedger(await stockApi.getLedger(variantId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load ledger", detail: extractErrorMessage(err) });
    } finally {
      setLedgerLoading(false);
    }
  }

  function openAdjustDialog() {
    setSignedDelta("");
    setRemarks("");
    setAdjustDialogVisible(true);
  }

  async function handleAdjust(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await stockApi.recordAdjustment(selectedVariantId, {
        signedDelta: Number(signedDelta || 0),
        remarks: remarks || undefined,
      });
      toast.show({ severity: "success", summary: "Stock adjustment recorded" });
      setAdjustDialogVisible(false);
      await Promise.all([viewLedger(selectedVariantId, selectedSku), loadOverview()]);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not adjust stock", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  const lowStockColumns: Column<LowStockResponse>[] = [
    { field: "sku", header: "SKU", sortable: true },
    { field: "currentStock", header: "Current Stock" },
    { field: "reorderThreshold", header: "Reorder Threshold" },
    {
      field: "actions",
      header: "",
      body: (row) => (
        <Button size="sm" variant="text" onClick={() => viewLedger(row.variantId, row.sku)}>
          View Ledger
        </Button>
      ),
    },
  ];

  const ledgerColumns: Column<StockLedgerResponse>[] = [
    { field: "movementDate", header: "Date", sortable: true },
    { field: "movementType", header: "Type", body: (l) => movementTag(l.movementType) },
    {
      field: "quantityChange",
      header: "Quantity",
      body: (l) => signedQuantity(l.quantityChange),
    },
    { field: "balanceAfter", header: "Balance After" },
    { field: "referenceType", header: "Reference", body: (l) => l.referenceType ?? "-" },
    { field: "remarks", header: "Remarks", body: (l) => l.remarks ?? "-" },
  ];

  return (
    <>
      <Card title="Low Stock Alerts">
        <Table
          data={lowStock}
          columns={lowStockColumns}
          rowKey={(row) => row.variantId}
          loading={loading}
          emptyMessage="Nothing is at or below its reorder threshold right now."
        />
      </Card>

      <Card
        title="Stock Ledger"
        className="stock-ledger-card"
        actions={
          selectedVariantId && (
            <Button size="sm" severity="secondary" variant="outlined" onClick={openAdjustDialog}>
              Adjust Stock
            </Button>
          )
        }
      >
        <Select
          id="variant-picker"
          label="Product variant"
          placeholder="Select a variant to view its stock history"
          value={selectedVariantId}
          onChange={(e) => {
            const option = variantOptions.find((o) => o.value === e.target.value);
            viewLedger(e.target.value, option?.label ?? "");
          }}
          options={variantOptions}
        />

        {selectedVariantId ? (
          <Table
            data={ledger}
            columns={ledgerColumns}
            rowKey={(row) => row.id}
            loading={ledgerLoading}
            emptyMessage="No stock movements recorded for this variant yet."
          />
        ) : (
          <p style={{ color: "var(--text-color-secondary)", margin: 0 }}>
            Select a variant above to see its stock movement history.
          </p>
        )}
      </Card>

      <Dialog
        visible={adjustDialogVisible}
        onHide={() => setAdjustDialogVisible(false)}
        title={`Adjust Stock - ${selectedSku}`}
        footer={
          <>
            <Button variant="text" severity="secondary" onClick={() => setAdjustDialogVisible(false)}>
              Cancel
            </Button>
            <Button onClick={handleAdjust} loading={saving}>
              Save
            </Button>
          </>
        }
      >
        <form onSubmit={handleAdjust}>
          <Input
            id="signedDelta"
            label="Quantity change"
            hint="Positive to add stock (e.g. found extra), negative to remove it (e.g. damaged goods)."
            type="number"
            step="0.001"
            value={signedDelta}
            onChange={(e) => setSignedDelta(e.target.value)}
            required
          />
          <Input
            id="adjustment-remarks"
            label="Remarks (optional)"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        </form>
      </Dialog>
    </>
  );
}
