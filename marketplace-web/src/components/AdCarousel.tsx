import { resolveImagePath, safeURL } from "../lib/urlSafety";
import { AdLink, resolveAdLink } from "./AdLink";
import type { AdvertisementDto } from "../api/marketplaceApi";

/**
 * Super Admin-managed via /admin/advertisements - only one ad is ever live
 * for this slot at a time (no more rotation between several), so this is
 * just a single banner now, not an actual carousel. Kept the name and the
 * original CSS classes (.top-advertisement/.banner-slide/.banner-copy) for
 * visual continuity. `ad` is a real ad, or a built-in default when none is
 * live (see MarketplacePage); null only while the ads request is still
 * loading - reserves the banner's height so the page doesn't jump.
 */
export function AdCarousel({ ad }: { ad: AdvertisementDto | null }) {
  if (!ad) {
    return (
      <section className="top-advertisement" aria-hidden="true">
        <div className="ad-slides ad-slides-reserved" />
      </section>
    );
  }

  const imageOnly = !ad.headline && !ad.description;
  const slideClass = ["banner-slide", imageOnly && "banner-image-only", ad.isDefault && "banner-default"].filter(Boolean).join(" ");

  return (
    <section className="top-advertisement" aria-label="Advertisement">
      <span className="ad-label">ADVERTISEMENT</span>
      <div className="ad-slides">
        <AdLink ad={ad} className={slideClass} label={ad.headline ?? ad.altText}>
          {!imageOnly && (
            <div className="banner-copy">
              {ad.headline && <h2>{ad.headline}</h2>}
              {ad.description && <p>{ad.description}</p>}
              <span className="banner-cta">{resolveAdLink(ad) ? `${ad.ctaLabel ?? "Explore"} ↗` : ""}</span>
            </div>
          )}
          <img
            src={safeURL(resolveImagePath(ad.imageUrl)) || resolveImagePath("images/placeholder.svg")}
            alt={ad.altText}
            width={600}
            height={700}
            loading="eager"
            onError={(e) => {
              e.currentTarget.src = resolveImagePath("images/placeholder.svg");
            }}
          />
        </AdLink>
      </div>
    </section>
  );
}
