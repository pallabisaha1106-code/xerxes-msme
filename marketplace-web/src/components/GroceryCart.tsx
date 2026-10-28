import { useEffect, useMemo, useState, type MutableRefObject } from "react";
import { cartTotal, cartWhatsappUrl, formatPrice, productPack, productPricePaise, type CartEntry } from "../lib/grocery";
import type { Business, Product } from "../types";

export interface GroceryCartHandle {
  add: (index: number) => void;
}

/** Ported from shop-products.js's createGroceryCart/mountGroceryCart - an in-memory (reload clears it) WhatsApp order builder. */
export function GroceryCart({
  shop,
  products,
  handleRef,
}: {
  shop: Business;
  products: Product[];
  handleRef: MutableRefObject<GroceryCartHandle | null>;
}) {
  const [quantities, setQuantities] = useState<Map<number, number>>(new Map());

  function setQuantity(index: number, quantity: number) {
    if (!products[index]) return;
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 999) return;
    setQuantities((current) => {
      const next = new Map(current);
      if (quantity === 0) next.delete(index);
      else next.set(index, quantity);
      return next;
    });
  }

  useEffect(() => {
    handleRef.current = {
      add(index: number) {
        setQuantities((current) => {
          const next = new Map(current);
          next.set(index, Math.min(999, (next.get(index) || 0) + 1));
          return next;
        });
      },
    };
    return () => {
      handleRef.current = null;
    };
  }, [handleRef]);

  const entries: CartEntry[] = useMemo(
    () => [...quantities].map(([index, quantity]) => ({ index, quantity, product: products[index] })),
    [quantities, products],
  );
  const total = cartTotal(entries);
  const url = cartWhatsappUrl(shop, entries);

  return (
    <section className="grocery-cart" aria-label="Your grocery cart">
      <h2>Your cart</h2>
      <p role="status">
        {entries.length
          ? `${entries.reduce((sum, e) => sum + e.quantity, 0)} items across ${entries.length} products` +
            (entries.length && !url ? " · WhatsApp enquiries are not available for this shop yet." : "")
          : "Your cart is empty. Add products below."}
      </p>
      <div className="grocery-cart-items">
        {entries.map(({ index, quantity, product }) => {
          const price = productPricePaise(product);
          return (
            <div className="grocery-cart-row" key={index}>
              <span>
                {productPack(product)} · Ref {product.id || String(index + 1)}
                {price === null ? " · Price on enquiry" : ` · Unit MRP: ${formatPrice(price)} · Total: ${formatPrice(price * quantity)}`}
              </span>
              <div className="grocery-quantity">
                <button type="button" aria-label={`Decrease quantity of ${product.name}`} onClick={() => setQuantity(index, quantity - 1)}>
                  −
                </button>
                <input
                  type="number"
                  min={1}
                  max={999}
                  step={1}
                  value={quantity}
                  aria-label={`Quantity for ${product.name}`}
                  onChange={(e) => setQuantity(index, Number(e.target.value))}
                />
                <button
                  type="button"
                  aria-label={`Increase quantity of ${product.name}`}
                  disabled={quantity >= 999}
                  onClick={() => setQuantity(index, quantity + 1)}
                >
                  +
                </button>
                <button type="button" aria-label={`Remove ${product.name}`} onClick={() => setQuantity(index, 0)}>
                  Remove
                </button>
              </div>
            </div>
          );
        })}
      </div>
      <div className="grocery-cart-actions">
        {url && (
          <a className="product-enquiry" href={url} target="_blank" rel="noopener noreferrer">
            Send cart on WhatsApp
          </a>
        )}
        {entries.length > 0 && (
          <button type="button" onClick={() => setQuantities(new Map())}>
            Clear cart
          </button>
        )}
      </div>
      <p className="grocery-cart-note">
        Quantities are for each listed product or pack. The shop will confirm prices and availability. You send the message in WhatsApp.
      </p>
      {entries.length > 0 && <p className="grocery-cart-total" aria-live="polite">{`Grand total: ${total === null ? "Awaiting prices" : formatPrice(total)}`}</p>}
    </section>
  );
}
