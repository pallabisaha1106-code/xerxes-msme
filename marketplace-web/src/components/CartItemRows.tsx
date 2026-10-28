export interface CartRow {
  id: string;
  name: string;
  /** Pre-formatted, e.g. "500 g · Unit ₹40 · Total ₹80" - the shop page owns price formatting. */
  detail: string;
  quantity: number;
}

/** Item rows for CartDrawer: −/+/typed quantity and Remove, reusing the .grocery-cart-* look. */
export function CartItemRows({ rows, onSetQuantity }: { rows: CartRow[]; onSetQuantity: (id: string, quantity: number) => void }) {
  // A removed row takes its focused button with it - park focus on the list so keyboard users keep their place.
  function change(id: string, quantity: number, from: HTMLElement) {
    onSetQuantity(id, quantity);
    if (quantity <= 0) from.closest<HTMLElement>(".grocery-cart-items")?.focus();
  }

  return (
    <div className="grocery-cart-items" tabIndex={-1}>
      {rows.length === 0 && <p className="cart-empty">Your cart is empty. Add products from the shop.</p>}
      {rows.map(({ id, name, detail, quantity }) => (
        <div className="grocery-cart-row" key={id}>
          <span>
            {name}
            {detail ? ` ${detail}` : ""}
          </span>
          <div className="grocery-quantity">
            <button type="button" aria-label={`Decrease quantity of ${name}`} onClick={(e) => change(id, quantity - 1, e.currentTarget)}>
              −
            </button>
            <input
              type="number"
              min={1}
              max={999}
              step={1}
              value={quantity}
              aria-label={`Quantity for ${name}`}
              onChange={(e) => onSetQuantity(id, Number(e.target.value))}
            />
            <button type="button" aria-label={`Increase quantity of ${name}`} onClick={() => onSetQuantity(id, quantity + 1)}>
              +
            </button>
            <button type="button" aria-label={`Remove ${name}`} onClick={(e) => change(id, 0, e.currentTarget)}>
              Remove
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
