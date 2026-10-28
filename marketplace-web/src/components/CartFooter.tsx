/** Drawer footer: summary, grand total and the three cart actions. Amounts arrive pre-formatted from the shop page. */
export function CartFooter({
  summary,
  totalLabel,
  whatsappUrl,
  onCheckout,
  onClear,
}: {
  summary: string;
  totalLabel: string;
  whatsappUrl: string | null;
  onCheckout: () => void;
  onClear: () => void;
}) {
  return (
    <>
      <p className="cart-drawer-summary" role="status">
        {summary}
      </p>
      <p className="grocery-cart-total" aria-live="polite">
        Grand total: {totalLabel}
      </p>
      <div className="grocery-cart-actions">
        <button type="button" className="primary-button" onClick={onCheckout}>
          Checkout
        </button>
        {whatsappUrl && (
          <a className="product-enquiry" href={whatsappUrl} target="_blank" rel="noopener noreferrer">
            Send via WhatsApp
          </a>
        )}
        <button
          type="button"
          onClick={(e) => {
            // The footer unmounts with the last item - hand focus to the (still mounted) list first.
            const list = e.currentTarget.closest("dialog")?.querySelector<HTMLElement>(".grocery-cart-items");
            onClear();
            list?.focus();
          }}
        >
          Clear cart
        </button>
      </div>
    </>
  );
}
