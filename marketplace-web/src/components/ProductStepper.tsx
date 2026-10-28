/**
 * In-card − / qty / + that replaces a product's "Add to cart" button once it's in the cart. Typing a
 * quantity stays in the cart drawer; labels differ from the drawer's ("Increase quantity of ...") so the
 * two never share an accessible name.
 */
export function ProductStepper({
  name,
  quantity,
  onChange,
  max = 999,
}: {
  name: string;
  quantity: number;
  /** `from` is the clicked button, so the caller can find the card to move focus within. */
  onChange: (quantity: number, from: HTMLElement) => void;
  max?: number;
}) {
  return (
    <div className="product-stepper" role="group" aria-label={`${name} in cart`}>
      <button type="button" aria-label={`Remove one ${name}`} onClick={(e) => onChange(quantity - 1, e.currentTarget)}>
        −
      </button>
      <span aria-live="polite">{quantity}</span>
      <button type="button" aria-label={`Add one more ${name}`} disabled={quantity >= max} onClick={(e) => onChange(quantity + 1, e.currentTarget)}>
        +
      </button>
    </div>
  );
}
