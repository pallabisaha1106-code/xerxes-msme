import { Link } from "react-router-dom";
import { ContactLinksRow } from "./ContactLinksRow";
import { productPageUrl } from "../lib/productPage";
import { resolveImagePath, safeURL, shopWhatsappURL } from "../lib/urlSafety";
import type { Business, Product } from "../types";

export function BusinessCard({
  business,
  onPreviewProduct,
}: {
  business: Business;
  onPreviewProduct: (business: Business, product: Product) => void;
}) {
  const pageUrl = business.tenantId ? `shop/${business.tenantId}` : productPageUrl(business);
  const whatsappUrl = shopWhatsappURL(business.businessName, business.enquiryWhatsapp);
  const location = [business.area, business.pincode].filter(Boolean).join(" · ");
  // Live listings carry no street address - it's just a copy of the area, which the location line already shows.
  const showAddress = Boolean(business.address) && business.address !== business.area;

  return (
    <article className="business-card" data-shop={business.id}>
      <img
        className="shop-image"
        src={safeURL(resolveImagePath(business.shopPhoto || "images/placeholder.svg"))}
        alt={business.shopPhotoAlt || business.businessName + " — illustrative shop preview"}
        loading="lazy"
        width={400}
        height={247}
        onError={(e) => {
          e.currentTarget.src = resolveImagePath("images/placeholder.svg");
        }}
      />
      <div className="card-content">
        <span className="business-type">{business.businessType}</span>
        {business.bookable && <span className="appointments-tag">Appointments</span>}
        <h3>{business.businessName}</h3>
        {location && <p className="location">⌖ &nbsp;{location}</p>}
        <div className="product-gallery">
          {business.products.slice(0, 3).map((product, index) => (
            <button
              key={index}
              className="product-button"
              aria-label={`Preview ${product.name}`}
              onClick={() => onPreviewProduct(business, product)}
            >
              <img
                src={safeURL(resolveImagePath(product.image || "images/placeholder.svg"))}
                alt={product.name}
                loading="lazy"
                width={120}
                height={120}
                onError={(e) => {
                  e.currentTarget.src = resolveImagePath("images/placeholder.svg");
                }}
              />
            </button>
          ))}
        </div>
        <p className="product-caption">{business.products.length ? "A little look inside · Tap to explore" : "Product previews coming soon"}</p>
        {showAddress && <p className="address">{business.address}</p>}
        <ContactLinksRow business={business} />
        {whatsappUrl && (
          <a className="whatsapp-us-link" href={whatsappUrl} target="_blank" rel="noopener noreferrer" aria-label={`WhatsApp ${business.businessName}`}>
            WhatsApp us ↗
          </a>
        )}
        {pageUrl && (
          <Link className="view-products-link" to={"/" + pageUrl} aria-label={`View all products from ${business.businessName}`}>
            View all products ↗
          </Link>
        )}
      </div>
    </article>
  );
}
