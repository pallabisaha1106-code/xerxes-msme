import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Icon, Input, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as customerApi from "../../api/customerApi";
import * as productApi from "../../api/productApi";
import * as invoiceApi from "../../api/invoiceApi";
import type { CustomerResponse } from "../../api/customerApi";
import "../purchase/new-purchase.css";

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

export function NewInvoicePage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [variantOptions, setVariantOptions] = useState<{ label: string; value: string }[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const [customerId, setCustomerId] = useState("");
  const [invoiceDate, setInvoiceDate] = useState(todayIso());
  const [placeOfSupplyStateCode, setPlaceOfSupplyStateCode] = useState("");
  const [lineItems, setLineItems] = useState<LineItemForm[]>([blankLineItem()]);

  useEffect(() => {
    customerApi
      .listCustomers()
      .then(setCustomers)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load customers", detail: extractErrorMessage(err) }));
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
      const request: invoiceApi.InvoiceRequest = {
        customerId,
        invoiceDate,
        placeOfSupplyStateCode: placeOfSupplyStateCode || undefined,
        lineItems: lineItems.map((li) => ({
          variantId: li.variantId,
          quantity: Number(li.quantity || 0),
          unitPrice: Number(li.unitPrice || 0),
        })),
      };

      const created = await invoiceApi.createInvoice(request);
      toast.show({ severity: "success", summary: "Invoice created", detail: created.invoiceDisplayNumber });
      navigate("/invoices");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create invoice", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <Card title="Invoice details">
        <div className="form-row">
          <Select
            id="customerId"
            label="Customer"
            placeholder="Select a customer"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            options={customers.map((c) => ({ label: c.name, value: c.id }))}
            required
          />
          <Input
            id="invoiceDate"
            label="Invoice date"
            type="date"
            value={invoiceDate}
            onChange={(e) => setInvoiceDate(e.target.value)}
            required
          />
        </div>

        <Input
          id="placeOfSupplyStateCode"
          label="Place of supply state code (optional)"
          hint="Defaults to the customer's own GST state code when left blank."
          placeholder="e.g. 27"
          maxLength={2}
          value={placeOfSupplyStateCode}
          onChange={(e) => setPlaceOfSupplyStateCode(e.target.value)}
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
        <Button type="button" variant="outlined" severity="secondary" onClick={() => navigate("/invoices")}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Create Invoice
        </Button>
      </div>
    </form>
  );
}
