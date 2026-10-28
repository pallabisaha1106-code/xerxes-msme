import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { ContactLinksRow } from "../components/ContactLinksRow";
import { GroceryCart, type GroceryCartHandle } from "../components/GroceryCart";
import { businessesData } from "../data/businesses";
import { isGroceryShop, productEnquiryUrl } from "../lib/grocery";
import { productPageUrl } from "../lib/productPage";
import { resolveImagePath, safeURL } from "../lib/urlSafety";
import type { Product } from "../types";

/** Ported from shop-products.js: opt-in per shop, matched by its own fixed URL - no query-param spoofing. */
export function ShopProductPage() {
  const { pageFile } = useParams<{ pageFile: string }>();
  const shop = useMemo(
    () => businessesData.businesses.find((b) => productPageUrl(b) === pageFile),
    [pageFile],
  );

  const [previewIndex, setPreviewIndex] = useState<number | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cartHandleRef = useRef<GroceryCartHandle | null>(null);

  const products: Product[] = useMemo(() => {
    if (!shop) return [];
    const previews = shop.products.map((p, i) => ({
      ...p,
      id: p.id || (shop.id === "veera" ? "VC" : shop.id.toUpperCase()) + "-" + String(i + 1).padStart(2, "0"),
      category: p.category || p.name,
    }));
    return [...previews, ...(shop.productPage?.products ?? [])];
  }, [shop]);

  const grocery = shop ? isGroceryShop(shop) : false;

  useEffect(() => {
    document.title = shop ? `${shop.businessName} Products | Neighbourly` : "Product page unavailable | Neighbourly";
  }, [shop]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (previewIndex !== null && !dialog.open) dialog.showModal();
    if (previewIndex === null && dialog.open) dialog.close();
  }, [previewIndex]);

  if (!shop) {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="shop-product-page" id="shop-catalogue">
          <Link className="back-to-shops" to="/#marketplace">
            ← Back to Explore Shops
          </Link>
          <section id="catalogue-unavailable">
            <h1>Product page unavailable</h1>
            <p>This shop’s expanded catalogue is not currently available.</p>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  const previewProduct = previewIndex !== null ? products[previewIndex] : null;

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="shop-product-page" id="shop-catalogue">
        <Link className="back-to-shops" to="/#marketplace">
          ← Back to Explore Shops
        </Link>
        <div id="catalogue-content">
          <section className="catalogue-shop">
            <img
              id="catalogue-shop-image"
              src={safeURL(resolveImagePath(shop.shopPhoto || "images/placeholder.svg"))}
              alt={shop.shopPhotoAlt || shop.businessName}
              width={200}
              height={180}
              onError={(e) => {
                e.currentTarget.src = resolveImagePath("images/placeholder.svg");
              }}
            />
            <div>
              <span className="eyebrow">SHOP COLLECTION</span>
              <h1 id="catalogue-shop-name">{shop.businessName}</h1>
              <p id="catalogue-address">{shop.address}</p>
              <div id="catalogue-contact">
                <ContactLinksRow business={shop} />
              </div>
            </div>
          </section>
          <section aria-labelledby="collection-heading">
            <div className="catalogue-heading">
              <div>
                <h2 id="collection-heading">Explore the collection</h2>
                <p>Tap a photo for a closer look. Contact the shop for prices, sizes and availability.</p>
              </div>
              <span id="catalogue-count">{products.length} products</span>
            </div>
            {grocery && <GroceryCart shop={shop} products={products} handleRef={cartHandleRef} />}
            <div id="catalogue-grid" className="catalogue-grid">
              {products.map((product, i) => {
                const reference = product.id || String(i + 1);
                const enquiryURL = productEnquiryUrl(shop, product, reference);
                const price = grocery ? product.mrp : undefined;
                return (
                  <article className="catalogue-product" key={reference}>
                    <button className="catalogue-photo" type="button" aria-label={`Preview ${product.name}`} onClick={() => setPreviewIndex(i)}>
                      <img
                        src={safeURL(resolveImagePath(product.image || "images/placeholder.svg"))}
                        alt={product.name}
                        loading="lazy"
                        width={600}
                        height={800}
                        onError={(e) => {
                          e.currentTarget.src = resolveImagePath("images/placeholder.svg");
                        }}
                      />
                    </button>
                    <h3>{product.name}</h3>
                    <p>
                      {product.category || "Collection"} · Ref {reference}
                    </p>
                    {grocery && (
                      <p className="product-price">
                        {product.packSize ? product.packSize + " · " : ""}
                        {typeof price === "number" ? "MRP " + new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(price) : "Price on enquiry"}
                      </p>
                    )}
                    {enquiryURL && (
                      <a
                        className="product-enquiry"
                        href={enquiryURL}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`Enquire with ${shop.businessName} on WhatsApp about ${product.name}, Ref ${reference}`}
                      >
                        WhatsApp enquiry
                      </a>
                    )}
                    {grocery && (
                      <button type="button" className="product-add-cart" aria-label={`Add ${product.name} to cart`} onClick={() => cartHandleRef.current?.add(i)}>
                        Add to cart
                      </button>
                    )}
                  </article>
                );
              })}
            </div>
          </section>
        </div>
      </main>
      <SiteFooter listBusinessLabel="List your business" />

      <dialog id="catalogue-preview" ref={dialogRef} aria-labelledby="catalogue-preview-title" onClose={() => setPreviewIndex(null)}>
        <button className="dialog-close" aria-label="Close product preview" onClick={() => dialogRef.current?.close()}>
          ×
        </button>
        {previewProduct && (
          <>
            <img
              id="catalogue-preview-image"
              src={safeURL(resolveImagePath(previewProduct.image || "images/placeholder.svg"))}
              alt={previewProduct.name}
              onError={(e) => {
                if (!e.currentTarget.src.endsWith("/placeholder.svg")) e.currentTarget.src = resolveImagePath("images/placeholder.svg");
              }}
            />
            <h2 id="catalogue-preview-title">{previewProduct.name}</h2>
            <p id="catalogue-preview-code">
              {shop.businessName} · Ref {previewProduct.id || String((previewIndex ?? 0) + 1)}
            </p>
            <p>Contact {shop.businessName} for prices, sizes and availability.</p>
          </>
        )}
      </dialog>
    </>
  );
}
