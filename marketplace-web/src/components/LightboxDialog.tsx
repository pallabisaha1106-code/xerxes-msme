import { useEffect, useRef } from "react";
import { closeOnBackdropClick } from "../lib/dialogBackdrop";
import { resolveImagePath, safeURL } from "../lib/urlSafety";
import type { Product } from "../types";

export function LightboxDialog({
  product,
  businessName,
  onClose,
}: {
  product: Product | null;
  businessName: string;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (product) {
      if (!dialog.open) dialog.showModal();
    } else if (dialog.open) {
      dialog.close();
    }
  }, [product]);

  return (
    <dialog id="lightbox" ref={ref} onClose={onClose} onClick={closeOnBackdropClick}>
      <button className="dialog-close" aria-label="Close preview" onClick={() => ref.current?.close()}>
        ×
      </button>
      {product && (
        <>
          <img
            id="preview-image"
            src={safeURL(resolveImagePath(product.image || "images/placeholder.svg"))}
            alt={product.name}
            onError={(e) => {
              if (!e.currentTarget.src.endsWith("/placeholder.svg")) e.currentTarget.src = resolveImagePath("images/placeholder.svg");
            }}
          />
          <h2 id="preview-title">{product.name}</h2>
          <p id="preview-shop">{businessName}</p>
        </>
      )}
    </dialog>
  );
}
