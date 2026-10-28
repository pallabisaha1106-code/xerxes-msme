import { resolveImagePath, safeURL } from "../lib/urlSafety";
import { AdLink } from "./AdLink";
import type { AdvertisementDto } from "../api/marketplaceApi";

/**
 * `ad` is a real Super Admin-published ad, or a built-in default when none is live (see MarketplacePage).
 * null only while the ads request is still loading - renders an empty aside so the grid column holds its width.
 */
export function SideRailAd({ side, ad }: { side: "left" | "right"; ad: AdvertisementDto | null }) {
  if (!ad) return <aside className={`side-rail-ad ${side}-ad`} aria-hidden="true" />;

  return (
    <aside className={`side-rail-ad ${side}-ad`} aria-label={`${side === "left" ? "Left" : "Right"} Side-Rail Ad`}>
      <span className="ad-label">ADVERTISEMENT</span>
      <AdLink ad={ad} className="shop-ad" label={ad.headline ?? ad.altText}>
        <img
          src={safeURL(resolveImagePath(ad.imageUrl)) || resolveImagePath("images/placeholder.svg")}
          alt={ad.altText}
          width={800}
          height={2000}
          loading="lazy"
        />
      </AdLink>
    </aside>
  );
}
