import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";

export type ToastSeverity = "success" | "error" | "info";

export interface ToastMessage {
  id: number;
  severity: ToastSeverity;
  summary: string;
  detail?: string;
}

interface ToastContextValue {
  show: (message: Omit<ToastMessage, "id">) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

const AUTO_DISMISS_MS = 5000;

/** Same shape/behaviour as frontend-web's ToastProvider/useToast - a global, portal-rendered toast stack. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<ToastMessage[]>([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id: number) => {
    setMessages((current) => current.filter((m) => m.id !== id));
  }, []);

  const show = useCallback(
    (message: Omit<ToastMessage, "id">) => {
      const id = nextId.current++;
      setMessages((current) => [...current, { ...message, id }]);
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {createPortal(
        <div className="toast-stack">
          {messages.map((message) => (
            <div key={message.id} className={`toast toast-${message.severity}`}>
              <div className="toast-content">
                <div className="toast-summary">{message.summary}</div>
                {message.detail && <div className="toast-detail">{message.detail}</div>}
              </div>
              <button type="button" className="toast-close" onClick={() => dismiss(message.id)} aria-label="Dismiss notification">
                ×
              </button>
            </div>
          ))}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
