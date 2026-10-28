import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Dialog, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as purchaseApi from "../../api/purchaseApi";
import * as vendorApi from "../../api/vendorApi";
import * as productApi from "../../api/productApi";
import type { PurchaseLineItemResponse, PurchaseResponse } from "../../api/purchaseApi";
import type { VendorResponse } from "../../api/vendorApi";

function LineItemsTable({
  lineItems,
  variantLabel,
  variantBarcode,
}: {
  lineItems: PurchaseLineItemResponse[];
  variantLabel: (variantId: string) => string;
  variantBarcode: (variantId: string) => string;
}) {
  const columns: Column<PurchaseLineItemResponse>[] = [
    { field: "variantId", header: "Product / SKU", body: (li) => variantLabel(li.variantId) },
    { field: "barcode", header: "Barcode", body: (li) => variantBarcode(li.variantId) },
    { field: "quantity", header: "Quantity" },
    { field: "unitPrice", header: "Unit Price", body: (li) => `₹${li.unitPrice.toFixed(2)}` },
    { field: "gstRate", header: "GST Rate", body: (li) => `${li.gstRate}%` },
    { field: "lineTotal", header: "Line Total", body: (li) => `₹${li.lineTotal.toFixed(2)}` },
  ];

  return <Table data={lineItems} columns={columns} rowKey={(li) => li.variantId} emptyMessage="No line items." />;
}

export function PurchasesPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [purchases, setPurchases] = useState<PurchaseResponse[]>([]);
  const [vendors, setVendors] = useState<VendorResponse[]>([]);
  const [variantLabels, setVariantLabels] = useState<Record<string, string>>({});
  const [variantBarcodes, setVariantBarcodes] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [selectedPurchase, setSelectedPurchase] = useState<PurchaseResponse | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [purchaseList, vendorList, productList] = await Promise.all([
          purchaseApi.listPurchases(),
          vendorApi.listVendors(),
          productApi.listProducts(),
        ]);
        setPurchases(purchaseList);
        setVendors(vendorList);

        const labels: Record<string, string> = {};
        const barcodes: Record<string, string> = {};
        for (const product of productList) {
          for (const variant of product.variants) {
            labels[variant.id] = `${product.name} - ${variant.sku}`;
            barcodes[variant.id] = variant.barcode ?? "-";
          }
        }
        setVariantLabels(labels);
        setVariantBarcodes(barcodes);
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load purchases", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function vendorName(id: string): string {
    return vendors.find((v) => v.id === id)?.name ?? id;
  }

  function variantLabel(id: string): string {
    return variantLabels[id] ?? id;
  }

  function variantBarcode(id: string): string {
    return variantBarcodes[id] ?? "-";
  }

  const columns: Column<PurchaseResponse>[] = [
    { field: "vendorId", header: "Vendor", body: (p) => vendorName(p.vendorId), sortable: false },
    { field: "purchaseDate", header: "Date", sortable: true },
    {
      field: "referenceNumber",
      header: "Reference",
      body: (p) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => setSelectedPurchase(p)}>
          {p.referenceNumber ?? "View items"}
        </Button>
      ),
    },
    { field: "subtotal", header: "Subtotal", body: (p) => `₹${p.subtotal.toFixed(2)}` },
    { field: "totalTax", header: "Tax", body: (p) => `₹${p.totalTax.toFixed(2)}` },
    { field: "totalAmount", header: "Total", body: (p) => `₹${p.totalAmount.toFixed(2)}` },
  ];

  return (
    <Card
      title="Purchase Entries"
      actions={
        <Button icon="plus" severity="success" onClick={() => navigate("/purchases/new")}>
          New Purchase Entry
        </Button>
      }
    >
      <Table
        data={purchases}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No purchase entries yet. Record your first one."
      />
      <Dialog
        visible={selectedPurchase != null}
        onHide={() => setSelectedPurchase(null)}
        title={
          selectedPurchase
            ? `Line Items — ${vendorName(selectedPurchase.vendorId)} (${selectedPurchase.purchaseDate})`
            : "Line Items"
        }
        width={800}
      >
        {selectedPurchase && (
          <LineItemsTable
            lineItems={selectedPurchase.lineItems}
            variantLabel={variantLabel}
            variantBarcode={variantBarcode}
          />
        )}
      </Dialog>
    </Card>
  );
}
