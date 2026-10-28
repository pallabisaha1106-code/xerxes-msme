import { useEffect, useMemo, useState } from "react";
import { flushSync } from "react-dom";
import { Link, useParams } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { ContactLinksRow } from "../components/ContactLinksRow";
import { LightboxDialog } from "../components/LightboxDialog";
import { PhotoZoomDialog } from "../components/PhotoZoomDialog";
import { VariantPhoto } from "../components/VariantPhoto";
import { CheckoutDialog, type CheckoutDetails } from "../components/CheckoutDialog";
import { BookingDialog, type BookingService } from "../components/BookingDialog";
import { CartButton } from "../components/CartButton";
import { CartDrawer } from "../components/CartDrawer";
import { CartItemRows } from "../components/CartItemRows";
import { CartFooter } from "../components/CartFooter";
import { ProductStepper } from "../components/ProductStepper";
import { getMarketplaceListing, type MarketplaceListingDto } from "../api/marketplaceApi";
import { listStorefrontProducts, submitStorefrontOrder, type StorefrontVariantDto } from "../api/storefrontApi";
import { ApiRequestError, extractErrorMessage } from "../api/apiError";
import { useToast } from "../components/Toast";
import { normalizeEnquiryNumber, resolveImagePath, safeURL, shopWhatsappURL } from "../lib/urlSafety";

type LoadStatus = "loading" | "ready" | "not-found" | "error";

function formatRupees(amount: number): string {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount);
}

function variantAttributesLabel(variant: StorefrontVariantDto): string {
  const values = Object.values(variant.attributes ?? {}).filter(Boolean);
  return values.length ? values.join(" · ") : "";
}

function whatsappEnquiryUrl(listing: MarketplaceListingDto, variant: StorefrontVariantDto): string {
  const number = normalizeEnquiryNumber(listing.enquiryWhatsapp ?? undefined);
  if (!/^[1-9]\d{7,14}$/.test(number)) return "";
  const message =
    `Hello ${listing.businessName ?? "there"}, I found this product on Neighbourly.\n` +
    `Product: ${variant.productName}${variantAttributesLabel(variant) ? " (" + variantAttributesLabel(variant) + ")" : ""}\n` +
    `SKU: ${variant.sku}\nPlease share the price, available sizes and availability.`;
  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}

/** "" when the shop has no usable WhatsApp number - same rule as the per-product enquiry link above. */
function cartWhatsappUrl(
  listing: MarketplaceListingDto,
  cartEntries: { variantId: string; quantity: number; variant: StorefrontVariantDto }[],
  total: number,
): string {
  const number = normalizeEnquiryNumber(listing.enquiryWhatsapp ?? undefined);
  if (!/^[1-9]\d{7,14}$/.test(number) || cartEntries.length === 0) return "";
  const lines = cartEntries.map(({ quantity, variant }, i) => {
    const attributesLabel = variantAttributesLabel(variant);
    return (
      `${i + 1}. ${variant.productName}${attributesLabel ? ` (${attributesLabel})` : ""} | Ref: ${variant.sku} | ` +
      `Quantity: ${quantity} | Unit ${formatRupees(variant.sellingPrice)} | Total ${formatRupees(variant.sellingPrice * quantity)}`
    );
  });
  const message =
    `Hello ${listing.businessName ?? "there"}, I'd like to order these products from Neighbourly:\n\n` +
    `${lines.join("\n")}\n\n` +
    `Grand total: ${formatRupees(total)}\n\n` +
    `Please confirm availability and how I can complete payment/delivery. Thank you.`;
  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}

export function LiveShopPage() {
  const toast = useToast();
  const { tenantId } = useParams<{ tenantId: string }>();
  const [listing, setListing] = useState<MarketplaceListingDto | null>(null);
  const [variants, setVariants] = useState<StorefrontVariantDto[]>([]);
  const [status, setStatus] = useState<LoadStatus>("loading");
  const [quantities, setQuantities] = useState<Map<string, number>>(new Map());
  const [preview, setPreview] = useState<{ variant: StorefrontVariantDto; index: number } | null>(null);
  const [cartOpen, setCartOpen] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [checkoutSubmitting, setCheckoutSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [orderConfirmation, setOrderConfirmation] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [bookingService, setBookingService] = useState<BookingService | null>(null);

  useEffect(() => {
    if (!tenantId) return;
    let cancelled = false;
    setStatus("loading");
    Promise.all([getMarketplaceListing(tenantId), listStorefrontProducts(tenantId)])
      .then(([listingResult, variantsResult]) => {
        if (cancelled) return;
        setListing(listingResult);
        setVariants(variantsResult);
        setStatus("ready");
      })
      .catch((err) => {
        if (cancelled) return;
        setStatus(err instanceof ApiRequestError && err.status === 404 ? "not-found" : "error");
      });
    return () => {
      cancelled = true;
    };
  }, [tenantId]);

  useEffect(() => {
    document.title = listing ? `${listing.businessName} | Neighbourly` : "Shop | Neighbourly";
  }, [listing]);

  function setQuantity(variantId: string, quantity: number) {
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 999) return;
    setQuantities((current) => {
      const next = new Map(current);
      if (quantity === 0) next.delete(variantId);
      else next.set(variantId, quantity);
      return next;
    });
  }

  // A card's Add button and stepper replace each other, so the focused one unmounts. Flush the update, then
  // hand focus to its replacement: "+" after the first add, the Add button once the quantity is back to 0.
  function changeCardQuantity(variantId: string, quantity: number, from: HTMLElement) {
    const card = from.closest("article");
    const wasInCart = (quantities.get(variantId) || 0) > 0;
    flushSync(() => setQuantity(variantId, quantity));
    if (!wasInCart && quantity > 0) card?.querySelector<HTMLElement>(".product-stepper button:last-child")?.focus();
    if (quantity === 0) card?.querySelector<HTMLElement>(".product-add-cart")?.focus();
  }

  const cartEntries = useMemo(
    () =>
      [...quantities]
        .map(([variantId, quantity]) => ({ variantId, quantity, variant: variants.find((v) => v.variantId === variantId) }))
        .filter((entry): entry is { variantId: string; quantity: number; variant: StorefrontVariantDto } => Boolean(entry.variant)),
    [quantities, variants],
  );
  const cartTotal = cartEntries.reduce((sum, e) => sum + e.variant.sellingPrice * e.quantity, 0);
  const cartItemCount = cartEntries.reduce((sum, e) => sum + e.quantity, 0);

  // Grocery vendors' catalogues are big enough that shoppers benefit from filtering by
  // category - other vendor types don't get this yet (see regression.md's brainstorm notes).
  // Only categories the vendor actually has a product in ever show up as a chip.
  const productCategories = useMemo(
    () => [...new Set(variants.map((v) => v.categoryName).filter((name): name is string => Boolean(name)))].sort(),
    [variants],
  );
  const displayedVariants =
    selectedCategory === "All" ? variants : variants.filter((v) => v.categoryName === selectedCategory);

  async function handleCheckoutSubmit(details: CheckoutDetails) {
    if (!tenantId) return;
    setCheckoutSubmitting(true);
    setCheckoutError(null);
    try {
      const response = await submitStorefrontOrder(tenantId, {
        ...details,
        lineItems: cartEntries.map((e) => ({ variantId: e.variantId, quantity: e.quantity })),
      });
      setOrderConfirmation(`Order placed! The shop will confirm shortly. Order ID: ${response.id.slice(0, 8).toUpperCase()}`);
      setQuantities(new Map());
      setCheckoutOpen(false);
    } catch (err) {
      const message = extractErrorMessage(err);
      setCheckoutError(message);
      toast.show({ severity: "error", summary: "Could not place the order", detail: message });
    } finally {
      setCheckoutSubmitting(false);
    }
  }

  if (status === "loading") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="marketplace-status">
          <p>Loading shop…</p>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  if (status !== "ready" || !listing) {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="shop-product-page" id="shop-catalogue">
          <Link className="back-to-shops" to="/#marketplace">
            ← Back to Explore Shops
          </Link>
          <section id="catalogue-unavailable">
            <h1>Shop unavailable</h1>
            <p>
              {status === "not-found"
                ? "This shop isn't listed on the marketplace right now."
                : "We couldn't load this shop right now. Please try again shortly."}
            </p>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  // A shop in a bookable category (a salon, a clinic) takes appointment requests: no cart, no per-product enquiry and
  // no stock labels, whatever order mode the vendor happened to leave selected.
  const isBookable = listing.bookable === true;
  const isCheckout = !isBookable && listing.orderMode === "CHECKOUT";
  const shopWhatsapp = shopWhatsappURL(listing.businessName, listing.enquiryWhatsapp);
  const cartWhatsapp = cartWhatsappUrl(listing, cartEntries, cartTotal);
  const cartRows = cartEntries.map(({ variantId, quantity, variant }) => {
    const attributes = variantAttributesLabel(variant);
    return {
      id: variantId,
      name: variant.productName,
      detail: `${attributes ? `(${attributes}) ` : ""}· Unit ${formatRupees(variant.sellingPrice)} · Total ${formatRupees(variant.sellingPrice * quantity)}`,
      quantity,
    };
  });

  const productsHeader = (
    <>
      <div className="catalogue-heading">
        <div>
          <h2 id="collection-heading">Explore the collection</h2>
          <p>
            {isBookable
              ? "Pick a service and request a time that suits you."
              : isCheckout
                ? "Add products to your cart and check out directly."
                : "Tap WhatsApp enquiry to ask about a product's price and availability."}
          </p>
        </div>
        <span>{displayedVariants.length} products</span>
      </div>

      {orderConfirmation && <p className="checkout-confirmation">{orderConfirmation}</p>}

      {listing.categoryName === "Grocery" && productCategories.length > 1 && (
        <label className="product-category-filter">
          Filter by category
          <select value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)}>
            <option value="All">All</option>
            {productCategories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>
      )}
    </>
  );

  const productsGrid = (
    <>
      <div className={listing.imageProfile === "DETAIL" ? "catalogue-grid" : "catalogue-grid catalogue-grid-compact"}>
        {displayedVariants.map((variant) => {
          const attributesLabel = variantAttributesLabel(variant);
          const enquiryUrl = whatsappEnquiryUrl(listing, variant);
          return (
            <article className="catalogue-product" key={variant.variantId}>
              <VariantPhoto variant={variant} onEnlarge={(index) => setPreview({ variant, index })} />
              <h3>{variant.productName}</h3>
              <p>
                {attributesLabel || variant.sku} · Ref {variant.sku}
              </p>
              {variant.sellingPrice > 0 ? (
                <p className="product-price">{formatRupees(variant.sellingPrice)}</p>
              ) : (
                <p className="product-price product-price-unset">Ask for price</p>
              )}
              {!isBookable && !variant.inStock && <p className="checkout-error">Out of stock</p>}
              {isBookable ? (
                <button
                  type="button"
                  className="product-add-cart"
                  aria-label={`Request a slot for ${variant.productName}${attributesLabel ? ` (${attributesLabel})` : ""}`}
                  onClick={() =>
                    setBookingService({
                      variantId: variant.variantId,
                      name: `${variant.productName}${attributesLabel ? ` (${attributesLabel})` : ""}`,
                    })
                  }
                >
                  Request a slot
                </button>
              ) : isCheckout ? (
                (quantities.get(variant.variantId) || 0) > 0 ? (
                  <ProductStepper
                    name={variant.productName}
                    quantity={quantities.get(variant.variantId) || 0}
                    onChange={(quantity, from) => changeCardQuantity(variant.variantId, quantity, from)}
                  />
                ) : (
                  <button
                    type="button"
                    className="product-add-cart"
                    disabled={!variant.inStock}
                    aria-label={`Add ${variant.productName} to cart`}
                    onClick={(e) => changeCardQuantity(variant.variantId, 1, e.currentTarget)}
                  >
                    Add to cart
                  </button>
                )
              ) : (
                enquiryUrl && (
                  <a
                    className="product-enquiry"
                    href={enquiryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`Enquire with ${listing.businessName} on WhatsApp about ${variant.productName}`}
                  >
                    WhatsApp enquiry
                  </a>
                )
              )}
            </article>
          );
        })}
      </div>
      {variants.length === 0 && <p>This shop hasn’t added any products yet.</p>}
    </>
  );

  return (
    <>
      <TopStrip />
      <SiteHeader cart={isCheckout ? <CartButton itemCount={cartItemCount} onClick={() => setCartOpen(true)} /> : undefined} />
      <main className="shop-product-page" id="shop-catalogue">
        <Link className="back-to-shops" to="/#marketplace">
          ← Back to Explore Shops
        </Link>
        <div id="catalogue-content">
          <section className="catalogue-shop">
            <img
              src={safeURL(resolveImagePath(listing.shopPhotoUrl || "images/placeholder.svg"))}
              alt={listing.shopPhotoAlt || listing.businessName || "Shop photo"}
              width={200}
              height={180}
              onError={(e) => {
                e.currentTarget.src = resolveImagePath("images/placeholder.svg");
              }}
            />
            <div>
              <span className="eyebrow">SHOP COLLECTION</span>
              <h1>{listing.businessName}</h1>
              <p>{listing.area}</p>
              <ContactLinksRow
                business={{
                  businessName: listing.businessName ?? "",
                  links: {
                    instagram: listing.instagramUrl ?? undefined,
                    facebook: listing.facebookUrl ?? undefined,
                    youtube: listing.youtubeUrl ?? undefined,
                    googleMaps: listing.googleMapsUrl ?? undefined,
                    whatsapp: listing.whatsappUrl ?? undefined,
                    website: listing.websiteUrl ?? undefined,
                  },
                }}
              />
              {shopWhatsapp && (
                <a className="whatsapp-us-link whatsapp-us-inline" href={shopWhatsapp} target="_blank" rel="noopener noreferrer">
                  WhatsApp us ↗
                </a>
              )}
            </div>
          </section>
          <section aria-labelledby="collection-heading">
            {productsHeader}
            {productsGrid}
          </section>
        </div>
      </main>
      <SiteFooter listBusinessLabel="List your business" />

      {listing.imageProfile === "DETAIL" ? (
        <PhotoZoomDialog
          image={preview ? (preview.variant.zoomUrls?.[preview.index] ?? preview.variant.imageUrls[preview.index]) : null}
          title={preview?.variant.productName ?? ""}
          onClose={() => setPreview(null)}
        />
      ) : (
        <LightboxDialog
          product={preview ? { name: preview.variant.productName, image: preview.variant.imageUrls[preview.index] } : null}
          businessName={listing.businessName ?? ""}
          onClose={() => setPreview(null)}
        />
      )}

      {isBookable && tenantId && (
        <BookingDialog
          service={bookingService}
          tenantId={tenantId}
          businessName={listing.businessName ?? "The shop"}
          whatsappUrl={shopWhatsapp}
          onClose={() => setBookingService(null)}
        />
      )}

      {isCheckout && (
        <CartDrawer
          visible={cartOpen}
          onClose={() => setCartOpen(false)}
          footer={
            cartRows.length > 0 ? (
              <CartFooter
                summary={`${cartItemCount} item${cartItemCount === 1 ? "" : "s"} across ${cartRows.length} products`}
                totalLabel={formatRupees(cartTotal)}
                whatsappUrl={cartWhatsapp}
                onCheckout={() => {
                  setCartOpen(false);
                  setCheckoutOpen(true);
                }}
                onClear={() => setQuantities(new Map())}
              />
            ) : undefined
          }
        >
          <CartItemRows rows={cartRows} onSetQuantity={setQuantity} />
        </CartDrawer>
      )}

      {isCheckout && (
        <CheckoutDialog
          visible={checkoutOpen}
          itemCount={cartItemCount}
          total={cartTotal}
          submitting={checkoutSubmitting}
          errorMessage={checkoutError}
          onSubmit={handleCheckoutSubmit}
          onClose={() => setCheckoutOpen(false)}
        />
      )}
    </>
  );
}
