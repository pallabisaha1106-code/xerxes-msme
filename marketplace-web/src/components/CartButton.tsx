import { useEffect, useRef } from "react";
import { ShoppingCart } from "lucide-react";

/** Header cart icon with an item-count badge - the shop page passes it to SiteHeader's `cart` slot. */
export function CartButton({ itemCount, onClick }: { itemCount: number; onClick: () => void }) {
  // Keying the badge by its count remounts it on every change, which restarts the CSS "bump" - but
  // only increases get the class, so removing items doesn't pop.
  const previousCount = useRef(itemCount);
  const increased = itemCount > previousCount.current;
  useEffect(() => {
    previousCount.current = itemCount;
  }, [itemCount]);

  return (
    <button
      type="button"
      className="cart-button"
      aria-label={`Cart, ${itemCount} item${itemCount === 1 ? "" : "s"}`}
      onClick={onClick}
    >
      <ShoppingCart size={20} strokeWidth={1.75} aria-hidden="true" />
      {itemCount > 0 && (
        <span key={itemCount} className={increased ? "cart-badge cart-badge-bump" : "cart-badge"} aria-hidden="true">
          {itemCount > 99 ? "99+" : itemCount}
        </span>
      )}
    </button>
  );
}
