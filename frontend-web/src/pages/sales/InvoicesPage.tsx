import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Dialog, Icon, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as invoiceApi from "../../api/invoiceApi";
import * as customerApi from "../../api/customerApi";
import * as productApi from "../../api/productApi";
import type { InvoiceLineItemResponse, InvoiceResponse, InvoiceStatus } from "../../api/invoiceApi";
import type { CustomerResponse } from "../../api/customerApi";

function statusTag(status: InvoiceStatus) {
  if (status === "FINAL") return <Tag severity="success">FINAL</Tag>;
  if (status === "CANCELLED") return <Tag severity="danger">CANCELLED</Tag>;
  return <Tag severity="secondary">DRAFT</Tag>;
}

function LineItemsTable({
  lineItems,
  variantLabel,
  variantBarcode,
}: {
  lineItems: InvoiceLineItemResponse[];
  variantLabel: (variantId: string) => string;
  variantBarcode: (variantId: string) => string;
}) {
  const columns: Column<InvoiceLineItemResponse>[] = [
    { field: "variantId", header: "Product / SKU", body: (li) => variantLabel(li.variantId) },
    { field: "barcode", header: "Barcode", body: (li) => variantBarcode(li.variantId) },
    { field: "quantity", header: "Quantity" },
    { field: "unitPrice", header: "Unit Price", body: (li) => `₹${li.unitPrice.toFixed(2)}` },
    { field: "gstRate", header: "GST Rate", body: (li) => `${li.gstRate}%` },
    { field: "cgstAmount", header: "CGST", body: (li) => `₹${li.cgstAmount.toFixed(2)}` },
    { field: "sgstAmount", header: "SGST", body: (li) => `₹${li.sgstAmount.toFixed(2)}` },
    { field: "igstAmount", header: "IGST", body: (li) => `₹${li.igstAmount.toFixed(2)}` },
    { field: "lineTotal", header: "Line Total", body: (li) => `₹${li.lineTotal.toFixed(2)}` },
  ];

  return <Table data={lineItems} columns={columns} rowKey={(li) => li.variantId} emptyMessage="No line items." />;
}

export function InvoicesPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [variantLabels, setVariantLabels] = useState<Record<string, string>>({});
  const [variantBarcodes, setVariantBarcodes] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceResponse | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [invoiceList, customerList, productList] = await Promise.all([
          invoiceApi.listInvoices(),
          customerApi.listCustomers(),
          productApi.listProducts(),
        ]);
        setInvoices(invoiceList);
        setCustomers(customerList);

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
        toast.show({ severity: "error", summary: "Failed to load invoices", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function customerName(id: string): string {
    return customers.find((c) => c.id === id)?.name ?? id;
  }

  function variantLabel(id: string): string {
    return variantLabels[id] ?? id;
  }

  function variantBarcode(id: string): string {
    return variantBarcodes[id] ?? "-";
  }

  async function handleDownloadPdf(invoice: InvoiceResponse) {
    setDownloadingId(invoice.id);
    try {
      await invoiceApi.downloadInvoicePdf(invoice.id, invoice.invoiceDisplayNumber);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not download PDF", detail: extractErrorMessage(err) });
    } finally {
      setDownloadingId(null);
    }
  }

  const columns: Column<InvoiceResponse>[] = [
    {
      field: "invoiceDisplayNumber",
      header: "Invoice #",
      sortable: true,
      body: (i) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => setSelectedInvoice(i)}>
          {i.invoiceDisplayNumber}
        </Button>
      ),
    },
    { field: "customerId", header: "Customer", body: (i) => customerName(i.customerId) },
    { field: "invoiceDate", header: "Date", sortable: true },
    { field: "subtotal", header: "Subtotal", body: (i) => `₹${i.subtotal.toFixed(2)}` },
    {
      field: "tax",
      header: "Tax",
      body: (i) => `₹${(i.cgstAmount + i.sgstAmount + i.igstAmount).toFixed(2)}`,
    },
    { field: "totalAmount", header: "Total", body: (i) => `₹${i.totalAmount.toFixed(2)}` },
    { field: "status", header: "Status", body: (i) => statusTag(i.status) },
    {
      field: "pdf",
      header: "",
      width: "3.5rem",
      body: (i) => (
        <button
          type="button"
          className="table-icon-btn"
          onClick={() => handleDownloadPdf(i)}
          disabled={downloadingId === i.id}
          aria-label={`Download PDF for ${i.invoiceDisplayNumber}`}
        >
          <Icon name="download" size={15} />
        </button>
      ),
    },
  ];

  return (
    <Card
      title="Invoices"
      actions={
        <Button icon="plus" severity="success" onClick={() => navigate("/invoices/new")}>
          New Invoice
        </Button>
      }
    >
      <Table
        data={invoices}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No invoices yet. Create your first one."
      />
      <Dialog
        visible={selectedInvoice != null}
        onHide={() => setSelectedInvoice(null)}
        title={selectedInvoice ? `Line Items — ${selectedInvoice.invoiceDisplayNumber}` : "Line Items"}
        width={900}
      >
        {selectedInvoice && (
          <LineItemsTable
            lineItems={selectedInvoice.lineItems}
            variantLabel={variantLabel}
            variantBarcode={variantBarcode}
          />
        )}
      </Dialog>
    </Card>
  );
}
