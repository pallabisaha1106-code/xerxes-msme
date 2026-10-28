import { apiClient } from "./client";
import type { PendingInvoice, SubscriptionStatus } from "./subscriptionApi";

export type MembershipType = "FREE" | "PLUS" | "PREMIUM";
export type BusinessType = "RETAILER" | "WHOLESALER";

export interface AdminTenantResponse {
  id: string;
  businessName: string;
  email: string;
  phone?: string | null;
  gstin?: string | null;
  stateCode: string;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  pincode?: string | null;
  subscriptionPlan: MembershipType;
  /** Scheduled by an admin plan change; takes effect (and clears) at currentPeriodEnd. */
  pendingSubscriptionPlan: MembershipType | null;
  active: boolean;
  createdAt: string;
  productCount: number;
  customerCount: number;
  invoiceCount: number;
  storefrontOrderCount: number;
  subscriptionStatus: SubscriptionStatus;
  /** Local partner looking after the shop, or null. */
  partnerName?: string | null;
  partnerId?: string | null;
  /** False when the shop's partner has been switched off (it needs a new one); null without a partner. */
  partnerActive?: boolean | null;
  trialEndsAt: string | null;
  currentPeriodEnd: string;
  gracePeriodEndsAt: string | null;
  registrationCode: string | null;
  businessType: BusinessType | null;
  shopFrontPhotoUrl: string | null;
  gstCertificateUrl: string | null;
  ownerIdDocumentUrl: string | null;
  landmark: string | null;
}

export async function listVendors(): Promise<AdminTenantResponse[]> {
  const { data } = await apiClient.get<AdminTenantResponse[]>("/admin/tenants");
  return data;
}

export async function getVendor(tenantId: string): Promise<AdminTenantResponse> {
  const { data } = await apiClient.get<AdminTenantResponse>(`/admin/tenants/${tenantId}`);
  return data;
}

export async function updateVendorStatus(tenantId: string, active: boolean): Promise<AdminTenantResponse> {
  const { data } = await apiClient.put<AdminTenantResponse>(`/admin/tenants/${tenantId}/status`, { active });
  return data;
}

/** Gives the shop to a partner, or with null takes it back to Super Admin's own queue. */
export async function assignVendorPartner(tenantId: string, partnerId: string | null): Promise<AdminTenantResponse> {
  const { data } = await apiClient.put<AdminTenantResponse>(`/admin/tenants/${tenantId}/partner`, { partnerId });
  return data;
}

export async function updateVendorPlan(tenantId: string, subscriptionPlan: string): Promise<AdminTenantResponse> {
  const { data } = await apiClient.put<AdminTenantResponse>(`/admin/tenants/${tenantId}/plan`, { subscriptionPlan });
  return data;
}

export async function updateVendorSubscription(
  tenantId: string,
  status: SubscriptionStatus,
  currentPeriodEnd: string | null,
): Promise<AdminTenantResponse> {
  const { data } = await apiClient.put<AdminTenantResponse>(`/admin/tenants/${tenantId}/subscription`, {
    status,
    currentPeriodEnd,
  });
  return data;
}

export async function generateVendorSubscriptionInvoice(tenantId: string): Promise<PendingInvoice> {
  const { data } = await apiClient.post<PendingInvoice>(`/admin/tenants/${tenantId}/subscription/invoice`);
  return data;
}

export async function listVendorProducts<T>(tenantId: string): Promise<T[]> {
  const { data } = await apiClient.get<T[]>(`/admin/tenants/${tenantId}/products`);
  return data;
}

export async function listVendorInvoices<T>(tenantId: string): Promise<T[]> {
  const { data } = await apiClient.get<T[]>(`/admin/tenants/${tenantId}/invoices`);
  return data;
}

export async function listVendorCustomers<T>(tenantId: string): Promise<T[]> {
  const { data } = await apiClient.get<T[]>(`/admin/tenants/${tenantId}/customers`);
  return data;
}

export async function listVendorStorefrontOrders<T>(tenantId: string): Promise<T[]> {
  const { data } = await apiClient.get<T[]>(`/admin/tenants/${tenantId}/storefront-orders`);
  return data;
}
