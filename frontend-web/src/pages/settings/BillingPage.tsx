import { useEffect, useState } from "react";
import { Button, Card, Tag, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as subscriptionApi from "../../api/subscriptionApi";
import type { SubscriptionStatus, SubscriptionStatusResponse } from "../../api/subscriptionApi";
import "./billing.css";

function statusTag(status: SubscriptionStatus) {
  if (status === "ACTIVE") return <Tag severity="success">ACTIVE</Tag>;
  if (status === "TRIALING") return <Tag severity="info">TRIAL</Tag>;
  if (status === "PAST_DUE") return <Tag severity="warning">PAYMENT DUE</Tag>;
  return <Tag severity="danger">SUSPENDED</Tag>;
}

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" }) : "-";
}

function formatAmount(amountPaise: number): string {
  return (amountPaise / 100).toLocaleString("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
}

export function BillingPage() {
  const toast = useToast();
  const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        setStatus(await subscriptionApi.getSubscriptionStatus());
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load billing status", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading || !status) {
    return (
      <Card title="Billing &amp; Subscription">
        <p>Loading&hellip;</p>
      </Card>
    );
  }

  return (
    <Card title="Billing &amp; Subscription">
      <div className="billing-status-row">
        {statusTag(status.status)}
        {status.status === "TRIALING" && <span className="field-hint">Trial ends {formatDate(status.trialEndsAt)}</span>}
        {status.status === "ACTIVE" && <span className="field-hint">Renews {formatDate(status.currentPeriodEnd)}</span>}
      </div>

      {status.status === "PAST_DUE" && (
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Your payment is overdue. Pay the invoice below within the grace period (until{" "}
          {formatDate(status.gracePeriodEndsAt)}) to avoid losing access.
        </p>
      )}
      {status.status === "SUSPENDED" && (
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Access is suspended because your subscription payment is overdue. Pay the invoice below to restore access
          immediately.
        </p>
      )}

      {status.pendingInvoice && (
        <div className="billing-invoice-card">
          <div>
            <div className="billing-invoice-amount">{formatAmount(status.pendingInvoice.amountPaise)}</div>
            <div className="field-hint">Invoiced {formatDate(status.pendingInvoice.createdAt)}</div>
          </div>
          <Button
            severity="success"
            onClick={() => window.open(status.pendingInvoice!.paymentUrl, "_blank", "noopener,noreferrer")}
          >
            Pay Now
          </Button>
        </div>
      )}
    </Card>
  );
}
