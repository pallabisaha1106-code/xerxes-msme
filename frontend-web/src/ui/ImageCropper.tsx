import { useEffect, useMemo, useRef, useState } from "react";
import { Button } from "./Button";

const VIEW_MAX = 320;
const MAX_OUTPUT_SIDE = 3000;
const MAX_ZOOM = 4;

interface ImageCropperProps {
  file: File;
  /** width / height of the frame the photo will be cut to */
  aspect: number;
  onApply: (cropped: File) => void;
  onCancel: () => void;
}

/**
 * A fixed-shape crop frame: drag the photo to position it, use the slider to zoom.
 * The cut is taken from the photo's full-resolution pixels, so cropping never
 * lowers quality beyond what the framing itself removes.
 */
export function ImageCropper({ file, aspect, onApply, onCancel }: ImageCropperProps) {
  const [url, setUrl] = useState<string | null>(null);
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [busy, setBusy] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);
  const drag = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null);

  const view = useMemo(
    () => (aspect >= 1 ? { w: VIEW_MAX, h: VIEW_MAX / aspect } : { w: VIEW_MAX * aspect, h: VIEW_MAX }),
    [aspect],
  );

  useEffect(() => {
    const objectUrl = URL.createObjectURL(file);
    setUrl(objectUrl);
    setNatural(null);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  // "cover" scale: the smallest size at which the photo still fills the whole frame.
  const baseScale = natural ? Math.max(view.w / natural.w, view.h / natural.h) : 1;
  const scale = baseScale * zoom;

  function clamp(x: number, y: number, s: number) {
    if (!natural) return { x, y };
    return {
      x: Math.min(0, Math.max(view.w - natural.w * s, x)),
      y: Math.min(0, Math.max(view.h - natural.h * s, y)),
    };
  }

  function handleLoad(e: React.SyntheticEvent<HTMLImageElement>) {
    const w = e.currentTarget.naturalWidth;
    const h = e.currentTarget.naturalHeight;
    const s = Math.max(view.w / w, view.h / h);
    setNatural({ w, h });
    setZoom(1);
    setOffset({ x: (view.w - w * s) / 2, y: (view.h - h * s) / 2 });
  }

  function handleZoom(next: number) {
    if (!natural) return;
    // Keep whatever is at the centre of the frame there while zooming.
    const centreX = (view.w / 2 - offset.x) / scale;
    const centreY = (view.h / 2 - offset.y) / scale;
    const nextScale = baseScale * next;
    setZoom(next);
    setOffset(clamp(view.w / 2 - centreX * nextScale, view.h / 2 - centreY * nextScale, nextScale));
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId);
    drag.current = { startX: e.clientX, startY: e.clientY, originX: offset.x, originY: offset.y };
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (!drag.current) return;
    setOffset(clamp(drag.current.originX + e.clientX - drag.current.startX, drag.current.originY + e.clientY - drag.current.startY, scale));
  }

  function handleApply() {
    const img = imgRef.current;
    if (!img || !natural) return;
    setBusy(true);
    const sx = -offset.x / scale;
    const sy = -offset.y / scale;
    const sw = view.w / scale;
    const sh = view.h / scale;
    const shrink = Math.min(1, MAX_OUTPUT_SIDE / Math.max(sw, sh));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(sw * shrink));
    canvas.height = Math.max(1, Math.round(sh * shrink));
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      setBusy(false);
      return;
    }
    ctx.imageSmoothingQuality = "high";
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        setBusy(false);
        if (!blob) return;
        const name = file.name.replace(/\.[^.]+$/, "") + "-cropped.jpg";
        onApply(new File([blob], name, { type: "image/jpeg" }));
      },
      "image/jpeg",
      0.92,
    );
  }

  return (
    <div className="image-cropper">
      <p className="field-hint">Drag the photo to position it, and use the slider to zoom.</p>
      <div
        className="image-cropper-frame"
        style={{ width: view.w, height: view.h }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={() => (drag.current = null)}
        onPointerCancel={() => (drag.current = null)}
      >
        {url && (
          <img
            ref={imgRef}
            src={url}
            alt="Photo being cropped"
            draggable={false}
            onLoad={handleLoad}
            style={
              natural
                ? { left: offset.x, top: offset.y, width: natural.w * scale, height: natural.h * scale }
                : { visibility: "hidden" }
            }
          />
        )}
      </div>
      <input
        type="range"
        min={1}
        max={MAX_ZOOM}
        step={0.01}
        value={zoom}
        aria-label="Zoom"
        style={{ width: view.w }}
        onChange={(e) => handleZoom(Number(e.target.value))}
      />
      <div className="image-cropper-actions">
        <Button type="button" size="sm" loading={busy} disabled={!natural} onClick={handleApply}>
          Use this crop
        </Button>
        <Button type="button" size="sm" variant="outlined" severity="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
