import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as subscriptionApi from "../api/subscriptionApi";
import type { SubscriptionStatus } from "../api/subscriptionApi";
import "./subscription-banner.css";

export function SubscriptionBanner() {
  const [status, setStatus] = useState<SubscriptionStatus | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const response = await subscriptionApi.getSubscriptionStatus();
        setStatus(response.status);
      } catch {
        // Silent on purpose: a transient failure here shouldn't block the
        // whole app shell from rendering - the Billing page itself will
        // surface a proper error if the user navigates there.
      }
    })();
  }, []);

  if (status !== "PAST_DUE" && status !== "SUSPENDED") {
    return null;
  }

  const message =
    status === "SUSPENDED"
      ? "Access is suspended because your subscription payment is overdue."
      : "Your subscription payment is due. Pay soon to avoid losing access.";

  return (
    <div className={`subscription-banner subscription-banner-${status === "SUSPENDED" ? "danger" : "warning"}`}>
      <span>{message}</span>
      <Link to="/settings/billing">Pay now</Link>
    </div>
  );
}
