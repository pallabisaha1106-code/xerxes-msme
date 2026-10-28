import { useEffect, useRef, useState, type FormEvent } from "react";
import { BrowserMultiFormatReader, type IScannerControls } from "@zxing/browser";
import { Button, Card, Icon, Select, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import * as customerApi from "../../api/customerApi";
import * as invoiceApi from "../../api/invoiceApi";
import type { VariantScanResponse } from "../../api/productApi";
import type { CustomerResponse } from "../../api/customerApi";
import type { InvoiceResponse } from "../../api/invoiceApi";
import "./quick-billing.css";

interface CartLine {
  variantId: string;
  productName: string;
  sku: string;
  attributes: Record<string, string>;
  unitPrice: number;
  quantity: number;
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function describeAttributes(attributes: Record<string, string>): string {
  return Object.entries(attributes)
    .map(([key, value]) => `${key}: ${value}`)
    .join(", ");
}

const SCAN_COOLDOWN_MS = 1500;

export function QuickBillingPage() {
  const toast = useToast();

  const videoRef = useRef<HTMLVideoElement>(null);
  const controlsRef = useRef<IScannerControls | null>(null);
  const lastScanRef = useRef<{ code: string; at: number } | null>(null);

  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manualBarcode, setManualBarcode] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [customerId, setCustomerId] = useState("");
  const [invoiceDate, setInvoiceDate] = useState(todayIso());
  const [submitting, setSubmitting] = useState(false);
  const [lastInvoice, setLastInvoice] = useState<InvoiceResponse | null>(null);

  useEffect(() => {
    customerApi
      .listCustomers()
      .then(setCustomers)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load customers", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const reader = new BrowserMultiFormatReader();
    let cancelled = false;

    (async () => {
      try {
        const devices = await BrowserMultiFormatReader.listVideoInputDevices();
        if (devices.length === 0) {
          setCameraError("No camera found. Use the manual barcode field below.");
          return;
        }
        const backCamera = devices.find((d) => /back|rear|environment/i.test(d.label));
        const deviceId = (backCamera ?? devices[0]).deviceId;
        if (cancelled || !videoRef.current) return;

        const controls = await reader.decodeFromVideoDevice(deviceId, videoRef.current, (result) => {
          if (result) {
            handleScan(result.getText());
          }
        });
        if (cancelled) {
          controls.stop();
          return;
        }
        controlsRef.current = controls;
      } catch {
        setCameraError("Could not access the camera. Use the manual barcode field below.");
      }
    })();

    return () => {
      cancelled = true;
      controlsRef.current?.stop();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function addToCart(variant: VariantScanResponse) {
    setCart((current) => {
      const existing = current.find((line) => line.variantId === variant.variantId);
      if (existing) {
        return current.map((line) =>
          line.variantId === variant.variantId ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [
        ...current,
        {
          variantId: variant.variantId,
          productName: variant.productName,
          sku: variant.sku,
          attributes: variant.attributes,
          unitPrice: variant.sellingPrice,
          quantity: 1,
        },
      ];
    });
  }

  async function handleScan(rawCode: string) {
    const code = rawCode.trim();
    if (!code) return;

    const now = Date.now();
    if (lastScanRef.current && lastScanRef.current.code === code && now - lastScanRef.current.at < SCAN_COOLDOWN_MS) {
      return;
    }
    lastScanRef.current = { code, at: now };

    try {
      const variant = await productApi.scanByBarcode(code);
      addToCart(variant);
      toast.show({ severity: "success", summary: "Added to cart", detail: `${variant.productName} - ${variant.sku}` });
    } catch (err) {
      toast.show({ severity: "error", summary: `Unrecognized barcode: ${code}`, detail: extractErrorMessage(err) });
    }
  }

  async function handleManualSubmit(e: FormEvent) {
    e.preventDefault();
    const code = manualBarcode.trim();
    setManualBarcode("");
    if (code) await handleScan(code);
  }

  function updateQuantity(variantId: string, quantity: number) {
    setCart((current) => current.map((line) => (line.variantId === variantId ? { ...line, quantity } : line)));
  }

  function removeLine(variantId: string) {
    setCart((current) => current.filter((line) => line.variantId !== variantId));
  }

  function startNewSale() {
    setCart([]);
    setCustomerId("");
    setLastInvoice(null);
  }

  const subtotal = cart.reduce((sum, line) => sum + line.quantity * line.unitPrice, 0);

  async function handleFinalize() {
    if (!customerId) {
      toast.show({ severity: "warning", summary: "Select a customer first" });
      return;
    }
    if (cart.length === 0) {
      toast.show({ severity: "warning", summary: "Cart is empty" });
      return;
    }
    setSubmitting(true);
    try {
      const request: invoiceApi.InvoiceRequest = {
        customerId,
        invoiceDate,
        lineItems: cart.map((line) => ({
          variantId: line.variantId,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
        })),
      };
      const created = await invoiceApi.createInvoice(request);
      toast.show({ severity: "success", summary: "Invoice created", detail: created.invoiceDisplayNumber });
      setLastInvoice(created);
      setCart([]);
      setCustomerId("");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create invoice", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDownloadLastInvoice() {
    if (!lastInvoice) return;
    try {
      await invoiceApi.downloadInvoicePdf(lastInvoice.id, lastInvoice.invoiceDisplayNumber);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not download PDF", detail: extractErrorMessage(err) });
    }
  }

  const columns: Column<CartLine>[] = [
    {
      field: "productName",
      header: "Item",
      body: (line) => (
        <div>
          <div>{line.productName}</div>
          <div className="sku-note">
            {line.sku}
            {Object.keys(line.attributes).length > 0 ? ` · ${describeAttributes(line.attributes)}` : ""}
          </div>
        </div>
      ),
    },
    { field: "unitPrice", header: "Price", body: (line) => `₹${line.unitPrice.toFixed(2)}` },
    {
      field: "quantity",
      header: "Qty",
      width: "5.5rem",
      body: (line) => (
        <input
          type="number"
          className="input"
          min={1}
          step={1}
          value={line.quantity}
          onChange={(e) => updateQuantity(line.variantId, Math.max(1, Number(e.target.value) || 1))}
        />
      ),
    },
    { field: "lineTotal", header: "Total", body: (line) => `₹${(line.unitPrice * line.quantity).toFixed(2)}` },
    {
      field: "actions",
      header: "",
      width: "3rem",
      body: (line) => (
        <button
          type="button"
          className="table-icon-btn table-icon-btn-danger"
          onClick={() => removeLine(line.variantId)}
          aria-label={`Remove ${line.sku}`}
        >
          <Icon name="x" size={15} />
        </button>
      ),
    },
  ];

  return (
    <div className="pos-layout">
      <Card title="Scan Item">
        <div className="pos-video-wrapper">
          <video ref={videoRef} className="pos-video" muted playsInline />
        </div>
        {cameraError && <div className="pos-camera-error">{cameraError}</div>}
        <form className="pos-manual-form" onSubmit={handleManualSubmit}>
          <input
            className="input"
            placeholder="Or type/scan barcode here"
            value={manualBarcode}
            onChange={(e) => setManualBarcode(e.target.value)}
            autoFocus
          />
          <Button type="submit" size="sm">
            Add
          </Button>
        </form>
      </Card>

      <Card title="Current Sale">
        <div className="form-row">
          <Select
            id="pos-customer"
            label="Customer"
            placeholder="Select a customer"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            options={customers.map((c) => ({ label: c.name, value: c.id }))}
          />
          <input
            id="pos-date"
            className="input"
            type="date"
            value={invoiceDate}
            onChange={(e) => setInvoiceDate(e.target.value)}
          />
        </div>

        <Table data={cart} columns={columns} rowKey={(line) => line.variantId} emptyMessage="Scan an item to begin." />

        <div className="pos-summary">
          <div className="pos-summary-inner">
            <div className="pos-summary-row grand-total">
              <span>Total</span>
              <span>₹{subtotal.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {lastInvoice ? (
          <div className="pos-finalize-row">
            <Button variant="outlined" icon="download" onClick={handleDownloadLastInvoice}>
              Download {lastInvoice.invoiceDisplayNumber}
            </Button>
            <Button onClick={startNewSale}>Start New Sale</Button>
          </div>
        ) : (
          <div className="pos-finalize-row">
            <Button variant="outlined" severity="secondary" onClick={startNewSale} disabled={cart.length === 0}>
              Clear
            </Button>
            <Button onClick={handleFinalize} loading={submitting}>
              Finalize Invoice
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
}
