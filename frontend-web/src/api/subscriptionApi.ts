import { apiClient } from "./client";

export type SubscriptionStatus = "TRIALING" | "ACTIVE" | "PAST_DUE" | "SUSPENDED";

export interface PendingInvoice {
  amountPaise: number;
  paymentUrl: string;
  createdAt: string;
}

export interface SubscriptionStatusResponse {
  status: SubscriptionStatus;
  trialEndsAt: string | null;
  currentPeriodEnd: string;
  gracePeriodEndsAt: string | null;
  pendingInvoice: PendingInvoice | null;
}

export async function getSubscriptionStatus(): Promise<SubscriptionStatusResponse> {
  const { data } = await apiClient.get<SubscriptionStatusResponse>("/subscription");
  return data;
}
