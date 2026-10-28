import { useState } from "react";
import type { StorefrontVariantDto } from "../api/storefrontApi";
import { resolveImagePath, safeURL } from "../lib/urlSafety";

const PLACEHOLDER = "images/placeholder.svg";

function imageSrc(url: string | undefined): string {
  return safeURL(resolveImagePath(url || PLACEHOLDER));
}

/**
 * One product photo on the live shop page: the main image (with a smaller
 * copy offered via srcset), a thumbnail strip when the variant has several
 * photos, and a "Sample image" label when it's only the starter-catalogue
 * placeholder rather than the vendor's own photo.
 */
export function VariantPhoto({
  variant,
  onEnlarge,
}: {
  variant: StorefrontVariantDto;
  onEnlarge: (photoIndex: number) => void;
}) {
  const [selected, setSelected] = useState(0);
  const images = variant.imageUrls;
  const thumbnails = variant.thumbnailUrls ?? [];
  const index = Math.min(selected, Math.max(images.length - 1, 0));
  const main = images[index];
  const thumb = thumbnails[index];

  const photo = (
    <>
      <img
        src={imageSrc(main)}
        srcSet={main && thumb ? `${imageSrc(thumb)} 480w, ${imageSrc(main)} 1600w` : undefined}
        sizes="(max-width: 950px) 50vw, 400px"
        alt={variant.productName}
        loading="lazy"
        width={600}
        height={800}
        onError={(e) => {
          e.currentTarget.removeAttribute("srcset");
          e.currentTarget.src = resolveImagePath(PLACEHOLDER);
        }}
      />
      {variant.sampleImage && <span className="sample-badge">Sample image</span>}
    </>
  );

  return (
    <>
      {main ? (
        <button
          type="button"
          className="catalogue-photo live-photo"
          aria-label={`Enlarge photo of ${variant.productName}`}
          onClick={() => onEnlarge(index)}
        >
          {photo}
        </button>
      ) : (
        <div className="catalogue-photo live-photo">{photo}</div>
      )}
      {images.length > 1 && (
        <div className="photo-strip">
          {images.map((url, i) => (
            <button
              key={url}
              type="button"
              className={i === index ? "active" : undefined}
              aria-label={`Show photo ${i + 1} of ${images.length}`}
              aria-pressed={i === index}
              onClick={() => setSelected(i)}
            >
              <img src={imageSrc(thumbnails[i] || url)} alt="" loading="lazy" width={48} height={64} />
            </button>
          ))}
        </div>
      )}
    </>
  );
}
