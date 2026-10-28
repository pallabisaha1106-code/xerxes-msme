import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Input, useToast } from "../../../ui";
import { extractErrorMessage } from "../../../api/client";
import { getMarketplaceListing } from "../../../api/marketplaceListingApi";
import { listProducts } from "../../../api/productApi";
import { getProductCapacity } from "../../../api/quickAddApi";
import { createStarterProducts, listStarterCatalog, type StarterCatalogGroup } from "../../../api/starterCatalogApi";
import { resolveImagePath } from "../../../lib/mediaUrl";
import { CategoryProductsDialog } from "./CategoryProductsDialog";
import { atCapacity, filterGroups, ownedProductIds, selectedCountByGroup, selectedProducts } from "./starterCatalog";
import "./starter.css";

/**
 * "Add from starter catalogue": the same picker a shop owner uses at registration (a library of category tiles, a
 * dialog of products per category, and a "Your catalogue" list), for a logged-in vendor. Nothing is saved until the
 * vendor confirms; the products are added with no price and no stock, to be filled in from the products page.
 */
export function StarterCatalogPage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [catalog, setCatalog] = useState<StarterCatalogGroup[] | null>(null);
  const [ownedNames, setOwnedNames] = useState<string[]>([]);
  /** How many more products the plan allows; null when that can't be read (the server's own limit still applies). */
  const [remaining, setRemaining] = useState<number | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [openGroupId, setOpenGroupId] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getMarketplaceListing()
      .then((listing) => Promise.all([listing.categoryId ? listStarterCatalog(listing.categoryId) : [], getProductCapacity(), listProducts()]))
      .then(([groups, capacity, products]) => {
        if (cancelled) return;
        setCatalog(groups);
        setRemaining(capacity?.remaining ?? null);
        setOwnedNames(products.map((p) => p.name));
      })
      .catch((err) => {
        if (!cancelled) setLoadError(extractErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const ownedIds = useMemo(() => ownedProductIds(catalog ?? [], ownedNames), [catalog, ownedNames]);
  const visibleGroups = useMemo(() => filterGroups(catalog ?? [], searchTerm), [catalog, searchTerm]);
  const countByGroup = useMemo(() => selectedCountByGroup(catalog ?? [], selectedIds), [catalog, selectedIds]);
  const picked = useMemo(() => selectedProducts(catalog ?? [], selectedIds), [catalog, selectedIds]);
  const full = atCapacity(selectedIds.size, remaining);

  function toggle(id: string) {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else if (!ownedIds.has(id) && !atCapacity(current.size, remaining)) next.add(id);
      return next;
    });
  }

  async function addProducts() {
    setAdding(true);
    try {
      await createStarterProducts([...selectedIds]);
      toast.show({
        severity: "success",
        summary: `${selectedIds.size} ${selectedIds.size === 1 ? "product" : "products"} added`,
        detail: "Set a price on each so customers can order them.",
      });
      navigate("/products");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not add the products", detail: extractErrorMessage(err) });
    } finally {
      setAdding(false);
    }
  }

  if (loadError || (catalog && catalog.length === 0)) {
    return (
      <div className="sc-page">
        <Card>
          <p className="qa-problem">
            {loadError ? `Couldn't open this page: ${loadError}` : "There is no starter catalogue for your shop's category yet."}
          </p>
          <Button variant="outlined" onClick={() => navigate("/products")}>
            Back to products
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="sc-page">
      <header className="qa-header">
        <div>
          <h1>Add from starter catalogue</h1>
          <p className="qa-muted">Pick your products, then add them to your catalogue.</p>
        </div>
        <Button variant="text" severity="secondary" onClick={() => navigate("/products")}>
          Back to products
        </Button>
      </header>

      {!catalog ? (
        <p className="qa-muted">Getting ready...</p>
      ) : (
        <div className="sc-layout">
          <Card>
            <span className="sc-eyebrow">PRODUCT LIBRARY</span>
            <h2 className="sc-title">Choose your products</h2>
            <p className="qa-muted">
              Pick a category, then choose the products you stock. We&rsquo;ll add them to your product catalog with no price or
              stock set, so you can fill those in yourself.
              {remaining !== null && ` Your plan allows up to ${remaining} product${remaining === 1 ? "" : "s"}.`}
            </p>
            <Input
              id="starter-search"
              label="Search products"
              type="search"
              placeholder="Search products…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <div className="sc-tiles">
              {visibleGroups.length === 0 && <p className="qa-muted">No matching categories.</p>}
              {visibleGroups.map((group) => {
                const count = countByGroup.get(group.id) ?? 0;
                return (
                  <button
                    type="button"
                    key={group.id}
                    className={`sc-tile${count > 0 ? " has-selection" : ""}`}
                    aria-label={group.name}
                    onClick={() => setOpenGroupId(group.id)}
                  >
                    <span className="sc-tile-name">{group.name}</span>
                    <span className="sc-tile-meta">
                      {group.products.length} product{group.products.length === 1 ? "" : "s"}
                      {count > 0 ? ` · ${count} selected` : ""}
                    </span>
                  </button>
                );
              })}
            </div>
          </Card>

          <Card>
            <h2 className="sc-title">
              Your catalogue <span className="sc-count">{picked.length}</span>
            </h2>
            {picked.length === 0 ? (
              <p className="qa-muted">Select products from a category to start your catalogue.</p>
            ) : (
              <>
                <p className="qa-muted">Products you&rsquo;ve added - remove any you don&rsquo;t want.</p>
                <div className="sc-picked">
                  {picked.map((product) => (
                    <div className="sc-picked-card" key={product.id}>
                      <button type="button" className="sc-remove" aria-label={`Remove ${product.name}`} onClick={() => toggle(product.id)}>
                        ×
                      </button>
                      {product.imageUrl && <img className="sc-thumb" src={resolveImagePath(product.imageUrl)} alt="" />}
                      <span className="sc-product-name">{product.name}</span>
                    </div>
                  ))}
                </div>
              </>
            )}
            <div className="sc-actions">
              <Button variant="outlined" severity="secondary" onClick={() => navigate("/products")}>
                Cancel
              </Button>
              <Button loading={adding} disabled={picked.length === 0} onClick={() => void addProducts()}>
                {picked.length === 0 ? "Add products" : `Add ${picked.length} ${picked.length === 1 ? "product" : "products"}`}
              </Button>
            </div>
          </Card>
        </div>
      )}

      <CategoryProductsDialog
        group={catalog?.find((group) => group.id === openGroupId) ?? null}
        selectedIds={selectedIds}
        ownedIds={ownedIds}
        atCapacity={full}
        onToggle={toggle}
        onClose={() => setOpenGroupId(null)}
      />
    </div>
  );
}
