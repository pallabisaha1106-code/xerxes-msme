import { useEffect, useRef } from "react";
import { closeOnBackdropClick } from "../lib/dialogBackdrop";

export function ContactNoticeDialog({
  visible,
  title,
  onClose,
}: {
  visible: boolean;
  title: string;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (visible && !dialog.open) dialog.showModal();
    if (!visible && dialog.open) dialog.close();
  }, [visible]);

  return (
    <dialog id="contact-notice" ref={ref} onClose={onClose} onClick={closeOnBackdropClick}>
      <button className="dialog-close" aria-label="Close message" onClick={() => ref.current?.close()}>
        ×
      </button>
      <h2>{title}</h2>
      <p>Contact details are not available in this demo. Real retailer and marketplace WhatsApp numbers can be added before launch.</p>
      <button className="primary-button dismiss" onClick={() => ref.current?.close()}>
        Got it
      </button>
    </dialog>
  );
}
