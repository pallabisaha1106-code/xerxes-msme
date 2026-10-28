import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Icon, Input, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as vendorApi from "../../api/vendorApi";
import * as productApi from "../../api/productApi";
import * as purchaseApi from "../../api/purchaseApi";
import type { VendorResponse } from "../../api/vendorApi";
import "./new-purchase.css";

interface LineItemForm {
  variantId: string;
  quantity: string;
  unitPrice: string;
}

function blankLineItem(): LineItemForm {
  return { variantId: "", quantity: "", unitPrice: "" };
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function NewPurchasePage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [vendors, setVendors] = useState<VendorResponse[]>([]);
  const [variantOptions, setVariantOptions] = useState<{ label: string; value: string }[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const [vendorId, setVendorId] = useState("");
  const [purchaseDate, setPurchaseDate] = useState(todayIso());
  const [referenceNumber, setReferenceNumber] = useState("");
  const [lineItems, setLineItems] = useState<LineItemForm[]>([blankLineItem()]);

  useEffect(() => {
    vendorApi
      .listVendors()
      .then(setVendors)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load vendors", detail: extractErrorMessage(err) }));
    productApi
      .listProducts()
      .then((products) => {
        const options = products.flatMap((product) =>
          product.variants
            .filter((variant) => variant.active)
            .map((variant) => ({
              label: `${product.name} - ${variant.sku}`,
              value: variant.id,
            })),
        );
        setVariantOptions(options);
      })
      .catch((err) => toast.show({ severity: "error", summary: "Could not load products", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function updateLineItem(index: number, patch: Partial<LineItemForm>) {
    setLineItems((current) => current.map((li, i) => (i === index ? { ...li, ...patch } : li)));
  }

  function addLineItem() {
    setLineItems((current) => [...current, blankLineItem()]);
  }

  function removeLineItem(index: number) {
    setLineItems((current) => current.filter((_, i) => i !== index));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const request: purchaseApi.PurchaseRequest = {
        vendorId,
        referenceNumber: referenceNumber || undefined,
        purchaseDate,
        lineItems: lineItems.map((li) => ({
          variantId: li.variantId,
          quantity: Number(li.quantity || 0),
          unitPrice: Number(li.unitPrice || 0),
        })),
      };

      await purchaseApi.createPurchase(request);
      toast.show({ severity: "success", summary: "Purchase entry recorded" });
      navigate("/purchases");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not record purchase", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <Card title="Purchase details">
        <div className="form-row">
          <Select
            id="vendorId"
            label="Vendor"
            placeholder="Select a vendor"
            value={vendorId}
            onChange={(e) => setVendorId(e.target.value)}
            options={vendors.map((v) => ({ label: v.name, value: v.id }))}
            required
          />
          <Input
            id="purchaseDate"
            label="Purchase date"
            type="date"
            value={purchaseDate}
            onChange={(e) => setPurchaseDate(e.target.value)}
            required
          />
        </div>

        <Input
          id="referenceNumber"
          label="Reference number (optional)"
          value={referenceNumber}
          onChange={(e) => setReferenceNumber(e.target.value)}
        />
      </Card>

      <Card
        title="Line items"
        className="line-items-card"
        actions={
          <Button type="button" icon="plus" size="sm" variant="outlined" onClick={addLineItem}>
            Add Line Item
          </Button>
        }
      >
        {lineItems.map((lineItem, index) => (
          <div key={index} className="line-item-row">
            <Select
              id={`variantId-${index}`}
              label="Product variant"
              placeholder="Select a variant"
              value={lineItem.variantId}
              onChange={(e) => updateLineItem(index, { variantId: e.target.value })}
              options={variantOptions}
              required
            />
            <Input
              id={`quantity-${index}`}
              label="Quantity"
              type="number"
              step="0.001"
              min={0.001}
              value={lineItem.quantity}
              onChange={(e) => updateLineItem(index, { quantity: e.target.value })}
              required
            />
            <Input
              id={`unitPrice-${index}`}
              label="Unit price"
              type="number"
              step="0.01"
              min={0}
              value={lineItem.unitPrice}
              onChange={(e) => updateLineItem(index, { unitPrice: e.target.value })}
              required
            />
            {lineItems.length > 1 && (
              <button
                type="button"
                className="line-item-remove"
                onClick={() => removeLineItem(index)}
                aria-label="Remove line item"
              >
                <Icon name="trash" size={15} />
              </button>
            )}
          </div>
        ))}
      </Card>

      <div className="form-actions">
        <Button type="button" variant="outlined" severity="secondary" onClick={() => navigate("/purchases")}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Record Purchase
        </Button>
      </div>
    </form>
  );
}
