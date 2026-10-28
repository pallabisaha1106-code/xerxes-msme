import { useEffect, useRef, useState } from "react";
import { closeOnBackdropClick } from "../lib/dialogBackdrop";
import { resolveImagePath, safeURL } from "../lib/urlSafety";

/**
 * Enlarged product photo with click-to-zoom, for shops whose products people
 * want to inspect closely. Zoomed, the photo is shown at its full size inside
 * a scrollable frame, so a mouse drags/scrolls and a finger swipes it with no
 * gesture code needed.
 */
export function PhotoZoomDialog({
  image,
  title,
  onClose,
}: {
  image: string | null;
  title: string;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const frameRef = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);
  const [zoomed, setZoomed] = useState(false);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    setZoomed(false);
    if (image) {
      if (!dialog.open) dialog.showModal();
    } else if (dialog.open) {
      dialog.close();
    }
  }, [image]);

  function toggleZoom(event: React.MouseEvent<HTMLImageElement>) {
    const img = event.currentTarget;
    if (zoomed) {
      setZoomed(false);
      return;
    }
    // Where on the photo was clicked, as a fraction, so the zoomed view opens centred on that spot.
    const rect = img.getBoundingClientRect();
    const fx = (event.clientX - rect.left) / rect.width;
    const fy = (event.clientY - rect.top) / rect.height;
    setZoomed(true);
    requestAnimationFrame(() => {
      const frame = frameRef.current;
      const zoomedImg = imgRef.current;
      if (!frame || !zoomedImg) return;
      frame.scrollLeft = fx * zoomedImg.scrollWidth - frame.clientWidth / 2;
      frame.scrollTop = fy * zoomedImg.scrollHeight - frame.clientHeight / 2;
    });
  }

  return (
    <dialog id="photo-zoom" ref={ref} onClose={onClose} onClick={closeOnBackdropClick}>
      <button className="dialog-close" aria-label="Close photo" onClick={() => ref.current?.close()}>
        ×
      </button>
      {image && (
        <>
          <div className={zoomed ? "zoom-frame is-zoomed" : "zoom-frame"} ref={frameRef}>
            <img
              ref={imgRef}
              src={safeURL(resolveImagePath(image))}
              alt={title}
              onClick={toggleZoom}
              onError={(e) => {
                if (!e.currentTarget.src.endsWith("/placeholder.svg")) e.currentTarget.src = resolveImagePath("images/placeholder.svg");
              }}
            />
          </div>
          <h2>{title}</h2>
          <p className="zoom-hint">{zoomed ? "Drag or scroll to look around. Click to zoom out." : "Click the photo to zoom in."}</p>
        </>
      )}
    </dialog>
  );
}
