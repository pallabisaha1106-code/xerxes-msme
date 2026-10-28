import type { MouseEvent } from "react";

/** Ported from the original site's dialog backdrop-click-to-close behavior for native <dialog>. */
export function closeOnBackdropClick(event: MouseEvent<HTMLDialogElement>) {
  const dialog = event.currentTarget;
  if (event.target !== dialog) return;
  const rect = dialog.getBoundingClientRect();
  if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) {
    dialog.close();
  }
}
