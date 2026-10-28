import { BusinessCard } from "./BusinessCard";
import type { Business, Product } from "../types";

export function BusinessGrid({
  businesses,
  demoNote,
  emptyHeading,
  onPreviewProduct,
  onResetFilters,
}: {
  businesses: Business[];
  demoNote?: string;
  emptyHeading: string;
  onPreviewProduct: (business: Business, product: Product) => void;
  onResetFilters: () => void;
}) {
  return (
    <section className="listings" aria-labelledby="shops-heading">
      {demoNote && <p className="demo-note">{demoNote}</p>}
      <div id="business-grid" className="business-grid">
        {businesses.map((business) => (
          <BusinessCard key={business.id} business={business} onPreviewProduct={onPreviewProduct} />
        ))}
      </div>
      {businesses.length === 0 && (
        <div id="empty">
          <span>⌕</span>
          <h3>{emptyHeading}</h3>
          <p>Try a different product, neighbourhood or pincode.</p>
          <button id="empty-reset" onClick={onResetFilters}>
            Clear all filters
          </button>
        </div>
      )}
    </section>
  );
}
