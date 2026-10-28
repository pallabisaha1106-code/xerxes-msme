import { useEffect, useMemo, useRef, useState } from "react";
import { listDefaultCatalog, type DefaultProductCatalogEntryDto } from "../api/marketplaceApi";
import { resolveImagePath } from "../lib/urlSafety";

export function DefaultCatalogPicker({
  marketplaceCategoryId,
  selectedIds,
  onToggle,
  remainingCapacity,
}: {
  marketplaceCategoryId: string;
  selectedIds: Set<string>;
  onToggle: (id: string) => void;
  /** Undefined = no plan-driven cap yet. 0 means the plan allows no catalogue at all. */
  remainingCapacity?: number;
}) {
  const [catalog, setCatalog] = useState<DefaultProductCatalogEntryDto[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [openCategoryId, setOpenCategoryId] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (!marketplaceCategoryId) {
      setCatalog([]);
      return;
    }
    listDefaultCatalog(marketplaceCategoryId)
      .then(setCatalog)
      .catch(() => setCatalog([]));
  }, [marketplaceCategoryId]);

  useEffect(() => {
    if (openCategoryId && dialogRef.current && !dialogRef.current.open) {
      dialogRef.current.showModal();
    }
  }, [openCategoryId]);

  const visibleCategories = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return catalog;
    return catalog.filter(
      (entry) => entry.name.toLowerCase().includes(term) || entry.products.some((p) => p.name.toLowerCase().includes(term)),
    );
  }, [catalog, searchTerm]);

  const selectedCountByCategory = useMemo(() => {
    const counts = new Map<string, number>();
    for (const entry of catalog) {
      counts.set(entry.id, entry.products.filter((p) => selectedIds.has(p.id)).length);
    }
    return counts;
  }, [catalog, selectedIds]);

  const selectedProducts = useMemo(
    () => catalog.flatMap((entry) => entry.products).filter((p) => selectedIds.has(p.id)),
    [catalog, selectedIds],
  );

  const openCategory = catalog.find((entry) => entry.id === openCategoryId) ?? null;

  function closeDialog() {
    dialogRef.current?.close();
    setOpenCategoryId(null);
  }

  if (catalog.length === 0) return null;

  const atCapacity = remainingCapacity !== undefined && selectedIds.size >= remainingCapacity;

  return (
    <div className="builder-layout">
      <div className="product-picker join-section">
        <div className="picker-heading">
          <span className="eyebrow">PRODUCT LIBRARY</span>
          <h3>Choose your products{remainingCapacity === undefined ? " (optional)" : ""}</h3>
          <p className="join-section-note">
            Pick a category, then choose the products you stock. We&rsquo;ll add them to your product catalog with
            no price or stock set, so you can fill those in yourself.
            {remainingCapacity !== undefined &&
              ` Your plan allows up to ${remainingCapacity} product${remainingCapacity === 1 ? "" : "s"}.`}
          </p>
        </div>
        <label>
          Search products
          <input
            type="search"
            placeholder="Search products…"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </label>
        <div className="category-grid">
          {visibleCategories.length === 0 && <p className="join-section-note">No matching categories.</p>}
          {visibleCategories.map((entry) => {
            const selectedCount = selectedCountByCategory.get(entry.id) ?? 0;
            return (
              <button
                type="button"
                key={entry.id}
                className={`category-tile${selectedCount > 0 ? " has-selection" : ""}`}
                aria-label={entry.name}
                onClick={() => setOpenCategoryId(entry.id)}
              >
                <span className="category-tile-name">{entry.name}</span>
                <span className="category-tile-meta">
                  {entry.products.length} product{entry.products.length === 1 ? "" : "s"}
                  {selectedCount > 0 ? ` · ${selectedCount} selected` : ""}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="selection-summary join-section">
        <h3>
          Your catalogue <span className="selection-count">{selectedProducts.length}</span>
        </h3>
        {selectedProducts.length === 0 ? (
          <p className="join-section-note">Select products from a category to start your catalogue.</p>
        ) : (
          <>
            <p className="join-section-note">Products you&rsquo;ve added - remove any you don&rsquo;t want.</p>
            <div className="selection-grid">
              {selectedProducts.map((product) => (
                <div className="selection-card" key={product.id}>
                  <button
                    type="button"
                    className="catalogue-remove"
                    aria-label={`Remove ${product.name}`}
                    onClick={() => onToggle(product.id)}
                  >
                    ×
                  </button>
                  {product.imageUrl && (
                    <img className="selection-card-thumb" src={resolveImagePath(product.imageUrl)} alt="" />
                  )}
                  <span className="selection-card-name">{product.name}</span>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      {openCategory && (
        <dialog className="category-products-dialog" ref={dialogRef} onClose={() => setOpenCategoryId(null)}>
          <button className="dialog-close" type="button" aria-label="Close" onClick={closeDialog}>
            ×
          </button>
          <h3>{openCategory.name}</h3>
          <p className="join-section-note">Select any products you stock.</p>
          <div className="category-products-grid">
            {openCategory.products.map((product) => {
              const checked = selectedIds.has(product.id);
              return (
                <label className={`product-tile${checked ? " selected" : ""}`} key={product.id}>
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={!checked && atCapacity}
                    onChange={() => onToggle(product.id)}
                  />
                  {product.imageUrl && (
                    <img className="product-tile-thumb" src={resolveImagePath(product.imageUrl)} alt="" />
                  )}
                  <span className="product-tile-name">{product.name}</span>
                </label>
              );
            })}
          </div>
          <button type="button" className="primary-button" onClick={closeDialog}>
            Done
          </button>
        </dialog>
      )}
    </div>
  );
}
