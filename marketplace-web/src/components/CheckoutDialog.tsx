import { useEffect, useRef, useState, type FormEvent } from "react";
import type { StorefrontOrderRequest } from "../api/storefrontApi";

export type CheckoutDetails = Omit<StorefrontOrderRequest, "lineItems">;

const EMPTY_FORM: CheckoutDetails = {
  customerName: "",
  customerPhone: "",
  customerEmail: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  pincode: "",
  notes: "",
};

export function CheckoutDialog({
  visible,
  itemCount,
  total,
  submitting,
  errorMessage,
  onSubmit,
  onClose,
}: {
  visible: boolean;
  itemCount: number;
  total: number;
  submitting: boolean;
  errorMessage: string | null;
  onSubmit: (details: CheckoutDetails) => void;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [form, setForm] = useState<CheckoutDetails>(EMPTY_FORM);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (visible && !dialog.open) dialog.showModal();
    if (!visible && dialog.open) dialog.close();
  }, [visible]);

  useEffect(() => {
    if (visible) setForm(EMPTY_FORM);
  }, [visible]);

  function update<K extends keyof CheckoutDetails>(key: K, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit(form);
  }

  return (
    <dialog ref={dialogRef} className="checkout-dialog" aria-labelledby="checkout-title" onClose={onClose}>
      <button className="dialog-close" type="button" aria-label="Close checkout" onClick={() => dialogRef.current?.close()}>
        ×
      </button>
      <h2 id="checkout-title">Checkout</h2>
      <p>
        {itemCount} item{itemCount === 1 ? "" : "s"} · Total{" "}
        {new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(total)}
      </p>
      <form className="checkout-form" onSubmit={handleSubmit}>
        <label>
          Name
          <input required value={form.customerName} onChange={(e) => update("customerName", e.target.value)} />
        </label>
        <label>
          Phone
          <input required value={form.customerPhone} onChange={(e) => update("customerPhone", e.target.value)} />
        </label>
        <label>
          Email (optional)
          <input type="email" value={form.customerEmail} onChange={(e) => update("customerEmail", e.target.value)} />
        </label>
        <label>
          Address
          <input value={form.addressLine1} onChange={(e) => update("addressLine1", e.target.value)} />
        </label>
        <div className="checkout-form-row">
          <label>
            City
            <input value={form.city} onChange={(e) => update("city", e.target.value)} />
          </label>
          <label>
            Pincode
            <input value={form.pincode} onChange={(e) => update("pincode", e.target.value)} />
          </label>
        </div>
        <label>
          Notes (optional)
          <input value={form.notes} onChange={(e) => update("notes", e.target.value)} />
        </label>
        {errorMessage && <p className="checkout-error">{errorMessage}</p>}
        <button type="submit" className="primary-button" disabled={submitting}>
          {submitting ? "Placing order…" : "Place order"}
        </button>
      </form>
    </dialog>
  );
}
