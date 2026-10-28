import { useEffect, useRef, type ReactNode } from "react";
import { closeOnBackdropClick } from "../lib/dialogBackdrop";

/** Cart panel: a right-hand drawer on desktop, a bottom sheet on phones. Native <dialog> gives Esc, backdrop and focus-trap. */
export function CartDrawer({
  visible,
  onClose,
  children,
  footer,
}: {
  visible: boolean;
  onClose: () => void;
  children?: ReactNode;
  /** Pinned below the scrolling body; omit it (e.g. for an empty cart) and no footer renders. */
  footer?: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (visible && !dialog.open) dialog.showModal();
    if (!visible && dialog.open) dialog.close();
  }, [visible]);

  return (
    <dialog ref={ref} className="cart-drawer" aria-labelledby="cart-drawer-title" onClose={onClose} onClick={closeOnBackdropClick}>
      <div className="cart-drawer-head">
        <h2 id="cart-drawer-title">Your cart</h2>
        <button type="button" className="cart-drawer-close" aria-label="Close cart" onClick={() => ref.current?.close()}>
          ×
        </button>
      </div>
      <div className="cart-drawer-body">{children}</div>
      {footer && <div className="cart-drawer-footer">{footer}</div>}
    </dialog>
  );
}
