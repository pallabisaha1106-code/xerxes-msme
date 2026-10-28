import { useEffect, useRef, useState, type ReactNode } from "react";

/**
 * A trigger element that opens a floating panel below it, closing on
 * outside click or Escape. Used for the topbar's user menu.
 */
export function Popover({
  trigger,
  children,
  align = "right",
}: {
  trigger: (props: { onClick: () => void; ref: React.RefObject<HTMLButtonElement> }) => ReactNode;
  children: ReactNode;
  align?: "left" | "right";
}) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (panelRef.current?.contains(target) || triggerRef.current?.contains(target)) {
        return;
      }
      setOpen(false);
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  return (
    <div className="popover-container">
      {trigger({ onClick: () => setOpen((v) => !v), ref: triggerRef })}
      {open && (
        <div
          ref={panelRef}
          className={`popover-panel popover-align-${align}`}
          onClick={() => setOpen(false)}
        >
          {children}
        </div>
      )}
    </div>
  );
}
