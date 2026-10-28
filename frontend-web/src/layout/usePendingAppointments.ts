import { useEffect, useState } from "react";
import { getPendingCount } from "../api/appointmentApi";

const CHANGED_EVENT = "appointments-changed";
const REFRESH_MS = 60_000;

/** Tell the sidebar badge that requests were decided, so it refreshes now instead of at the next minute. */
export function notifyAppointmentsChanged() {
  window.dispatchEvent(new Event(CHANGED_EVENT));
}

/**
 * How many appointment requests are waiting for this business. Refreshed on every page change, every minute, and
 * when requests are decided. A failed read just shows nothing - the badge is a courtesy, not something to error on.
 */
export function usePendingAppointments(pathname: string): number {
  const [pending, setPending] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const refresh = () =>
      getPendingCount()
        .then((count) => !cancelled && setPending(count))
        .catch(() => !cancelled && setPending(0));
    refresh();
    const timer = window.setInterval(refresh, REFRESH_MS);
    window.addEventListener(CHANGED_EVENT, refresh);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
      window.removeEventListener(CHANGED_EVENT, refresh);
    };
  }, [pathname]);

  return pending;
}
