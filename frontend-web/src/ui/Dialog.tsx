import { useEffect, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./Icon";

export function Dialog({
  visible,
  onHide,
  title,
  children,
  footer,
  width = 480,
}: {
  visible: boolean;
  onHide: () => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  width?: number;
}) {
  useEffect(() => {
    if (!visible) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onHide();
    }
    document.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [visible, onHide]);

  if (!visible) return null;

  return createPortal(
    <div className="dialog-mask" onMouseDown={(e) => e.target === e.currentTarget && onHide()}>
      <div className="dialog-panel" style={{ width }} role="dialog" aria-modal="true" aria-label={title}>
        <div className="dialog-header">
          <h2 className="dialog-title">{title}</h2>
          <button type="button" className="dialog-close" onClick={onHide} aria-label="Close">
            <Icon name="x" size={18} />
          </button>
        </div>
        <div className="dialog-body">{children}</div>
        {footer && <div className="dialog-footer">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}
