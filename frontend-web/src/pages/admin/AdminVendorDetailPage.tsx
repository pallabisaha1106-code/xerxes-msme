import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Card, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { resolveImagePath } from "../../lib/mediaUrl";
import * as adminApi from "../../api/adminApi";
import type { AdminTenantResponse } from "../../api/adminApi";
import { listPartners, type AdminPartner } from "../../api/adminPartnerApi";
import type { PendingInvoice, SubscriptionStatus } from "../../api/subscriptionApi";
import type { ProductResponse } from "../../api/productApi";
import type { CustomerResponse } from "../../api/customerApi";
import type { InvoiceResponse } from "../../api/invoiceApi";
import type { StorefrontOrderResponse } from "../../api/storefrontApi";

const PLAN_OPTIONS = [
  { label: "Free (up to 10 products, ₹0/mo)", value: "FREE" },
  { label: "Plus (up to 100 products, ₹199/mo)", value: "PLUS" },
  { label: "Premium (up to 500 products, ₹999/mo)", value: "PREMIUM" },
];

const SUBSCRIPTION_STATUS_OPTIONS: { label: string; value: SubscriptionStatus }[] = [
  { label: "Trialing", value: "TRIALING" },
  { label: "Active", value: "ACTIVE" },
  { label: "Past Due", value: "PAST_DUE" },
  { label: "Suspended", value: "SUSPENDED" },
];

function subscriptionStatusTag(status: SubscriptionStatus) {
  if (status === "ACTIVE") return <Tag severity="success">ACTIVE</Tag>;
  if (status === "TRIALING") return <Tag severity="info">TRIAL</Tag>;
  if (status === "PAST_DUE") return <Tag severity="warning">PAST DUE</Tag>;
  return <Tag severity="danger">SUSPENDED</Tag>;
}

export function AdminVendorDetailPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  // A partner reviews the shop and can suspend or reactivate it; plan, billing and the shop's own data are Super Admin's.
  const isPartner = useAuth().user?.role === "PARTNER";

  const [vendor, setVendor] = useState<AdminTenantResponse | null>(null);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [storefrontOrders, setStorefrontOrders] = useState<StorefrontOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [activePartners, setActivePartners] = useState<AdminPartner[]>([]);
  const [partnerDraft, setPartnerDraft] = useState("");
  const [savingPartner, setSavingPartner] = useState(false);
  const [planDraft, setPlanDraft] = useState("FREE");
  const [savingPlan, setSavingPlan] = useState(false);
  const [togglingStatus, setTogglingStatus] = useState(false);
  const [subscriptionStatusDraft, setSubscriptionStatusDraft] = useState<SubscriptionStatus>("ACTIVE");
  const [savingSubscription, setSavingSubscription] = useState(false);
  const [generatingInvoice, setGeneratingInvoice] = useState(false);
  const [generatedInvoice, setGeneratedInvoice] = useState<PendingInvoice | null>(null);

  useEffect(() => {
    if (!tenantId) return;
    (async () => {
      setLoading(true);
      try {
        const [vendorData, productData, customerData, invoiceData, orderData] = await Promise.all([
          adminApi.getVendor(tenantId),
          isPartner ? [] : adminApi.listVendorProducts<ProductResponse>(tenantId),
          isPartner ? [] : adminApi.listVendorCustomers<CustomerResponse>(tenantId),
          isPartner ? [] : adminApi.listVendorInvoices<InvoiceResponse>(tenantId),
          isPartner ? [] : adminApi.listVendorStorefrontOrders<StorefrontOrderResponse>(tenantId),
        ]);
        setVendor(vendorData);
        setPartnerDraft(vendorData.partnerActive ? (vendorData.partnerId ?? "") : "");
        if (!isPartner) setActivePartners(await listPartners("ACTIVE"));
        setPlanDraft(vendorData.subscriptionPlan);
        setSubscriptionStatusDraft(vendorData.subscriptionStatus);
        setProducts(productData);
        setCustomers(customerData);
        setInvoices(invoiceData);
        setStorefrontOrders(orderData);
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load vendor", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId]);

  async function handleSavePartner() {
    if (!vendor) return;
    setSavingPartner(true);
    try {
      const updated = await adminApi.assignVendorPartner(vendor.id, partnerDraft || null);
      setVendor(updated);
      setPartnerDraft(updated.partnerId ?? "");
      toast.show({ severity: "success", summary: "Local partner updated", detail: updated.partnerName ?? "No partner" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update partner", detail: extractErrorMessage(err) });
    } finally {
      setSavingPartner(false);
    }
  }

  async function handleToggleStatus() {
    if (!vendor) return;
    setTogglingStatus(true);
    try {
      const updated = await adminApi.updateVendorStatus(vendor.id, !vendor.active);
      setVendor(updated);
      toast.show({
        severity: "success",
        summary: updated.active ? "Vendor reactivated" : "Vendor suspended",
        detail: vendor.businessName,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update status", detail: extractErrorMessage(err) });
    } finally {
      setTogglingStatus(false);
    }
  }

  async function handleSavePlan() {
    if (!vendor) return;
    setSavingPlan(true);
    try {
      const updated = await adminApi.updateVendorPlan(vendor.id, planDraft);
      setVendor(updated);
      toast.show({
        severity: "success",
        summary: "Plan change scheduled",
        detail: `Takes effect ${new Date(updated.currentPeriodEnd).toLocaleDateString()}`,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update plan", detail: extractErrorMessage(err) });
    } finally {
      setSavingPlan(false);
    }
  }

  async function handleSaveSubscription() {
    if (!vendor) return;
    setSavingSubscription(true);
    try {
      const updated = await adminApi.updateVendorSubscription(vendor.id, subscriptionStatusDraft, null);
      setVendor(updated);
      toast.show({ severity: "success", summary: "Subscription updated", detail: subscriptionStatusDraft });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update subscription", detail: extractErrorMessage(err) });
    } finally {
      setSavingSubscription(false);
    }
  }

  async function handleGenerateInvoice() {
    if (!vendor) return;
    setGeneratingInvoice(true);
    try {
      const invoice = await adminApi.generateVendorSubscriptionInvoice(vendor.id);
      setGeneratedInvoice(invoice);
      toast.show({ severity: "success", summary: "Payment link generated" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not generate payment link", detail: extractErrorMessage(err) });
    } finally {
      setGeneratingInvoice(false);
    }
  }

  const productColumns: Column<ProductResponse>[] = [
    { field: "name", header: "Product" },
    { field: "brand", header: "Brand", body: (p) => p.brand ?? "-" },
    { field: "variants", header: "Variants", body: (p) => p.variants.length },
    { field: "defaultGstRate", header: "GST Rate", body: (p) => `${p.defaultGstRate}%` },
  ];

  const customerColumns: Column<CustomerResponse>[] = [
    { field: "name", header: "Name" },
    { field: "phone", header: "Phone", body: (c) => c.phone ?? "-" },
    { field: "email", header: "Email", body: (c) => c.email ?? "-" },
    { field: "city", header: "City", body: (c) => c.city ?? "-" },
  ];

  const invoiceColumns: Column<InvoiceResponse>[] = [
    { field: "invoiceDisplayNumber", header: "Invoice #" },
    { field: "invoiceDate", header: "Date" },
    { field: "totalAmount", header: "Total", body: (i) => `₹${i.totalAmount.toFixed(2)}` },
    { field: "status", header: "Status" },
  ];

  const orderColumns: Column<StorefrontOrderResponse>[] = [
    { field: "customerName", header: "Customer" },
    { field: "customerPhone", header: "Phone" },
    { field: "subtotal", header: "Subtotal", body: (o) => `₹${o.subtotal.toFixed(2)}` },
    { field: "status", header: "Status" },
  ];

  if (loading || !vendor) {
    return (
      <Card title="Vendor">
        <p>Loading&hellip;</p>
      </Card>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
      <Card
        title={vendor.businessName}
        actions={
          <Button variant="outlined" severity="secondary" onClick={() => navigate("/admin")}>
            Back to Vendors
          </Button>
        }
      >
        <div style={{ display: "flex", gap: "2rem", flexWrap: "wrap", marginBottom: "1rem" }}>
          <div>
            <p className="field-hint">Email</p>
            <p>{vendor.email}</p>
          </div>
          <div>
            <p className="field-hint">Phone</p>
            <p>{vendor.phone ?? "-"}</p>
          </div>
          <div>
            <p className="field-hint">Local partner</p>
            <p>{vendor.partnerName ? `${vendor.partnerName}${vendor.partnerActive === false ? " (inactive)" : ""}` : "-"}</p>
          </div>
          <div>
            <p className="field-hint">GSTIN</p>
            <p>{vendor.gstin ?? "-"}</p>
          </div>
          <div>
            <p className="field-hint">GST State Code</p>
            <p>{vendor.stateCode}</p>
          </div>
          <div>
            <p className="field-hint">Address</p>
            <p>{[vendor.addressLine1, vendor.addressLine2, vendor.city, vendor.pincode].filter(Boolean).join(", ") || "-"}</p>
          </div>
          <div>
            <p className="field-hint">Landmark</p>
            <p>{vendor.landmark ?? "-"}</p>
          </div>
          <div>
            <p className="field-hint">Signed Up</p>
            <p>{new Date(vendor.createdAt).toLocaleDateString()}</p>
          </div>
          <div>
            <p className="field-hint">Status</p>
            {vendor.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="danger">SUSPENDED</Tag>}
          </div>
          <div>
            <p className="field-hint">Subscription</p>
            {subscriptionStatusTag(vendor.subscriptionStatus)}
          </div>
          <div>
            <p className="field-hint">Current Period Ends</p>
            <p>{new Date(vendor.currentPeriodEnd).toLocaleDateString()}</p>
          </div>
          {vendor.pendingSubscriptionPlan && (
            <div>
              <p className="field-hint">Plan Change Scheduled</p>
              <p>
                {vendor.pendingSubscriptionPlan} from {new Date(vendor.currentPeriodEnd).toLocaleDateString()}
              </p>
            </div>
          )}
          {vendor.businessType && (
            <div>
              <p className="field-hint">Business Type</p>
              <p>{vendor.businessType === "RETAILER" ? "Retailer" : "Wholesaler"}</p>
            </div>
          )}
          {vendor.registrationCode && (
            <div>
              <p className="field-hint">Registration Code</p>
              <p>
                <Tag severity={vendor.active ? "secondary" : "warning"}>{vendor.registrationCode}</Tag>
              </p>
            </div>
          )}
        </div>

        <div style={{ display: "flex", gap: "1rem", alignItems: "flex-end", flexWrap: "wrap" }}>
          <Button
            severity={vendor.active ? "danger" : "success"}
            variant="outlined"
            loading={togglingStatus}
            onClick={handleToggleStatus}
          >
            {vendor.active ? "Suspend Vendor" : "Reactivate Vendor"}
          </Button>

          {!isPartner && (
            <>
              <Select
                id="partner-select"
                label="Local Partner"
                value={partnerDraft}
                onChange={(e) => setPartnerDraft(e.target.value)}
                options={[
                  { label: "No partner (Super Admin's queue)", value: "" },
                  ...activePartners.map((p) => ({ label: p.name, value: p.id })),
                ]}
              />
              <Button
                loading={savingPartner}
                disabled={partnerDraft === (vendor.partnerActive ? (vendor.partnerId ?? "") : "")}
                onClick={handleSavePartner}
              >
                Save Partner
              </Button>
            <div>
              <Select
                id="plan-select"
                label="Subscription Plan"
                value={planDraft}
                onChange={(e) => setPlanDraft(e.target.value)}
                options={PLAN_OPTIONS}
                disabled={vendor.subscriptionStatus === "TRIALING"}
              />
              {vendor.subscriptionStatus === "TRIALING" && (
                <p className="field-hint">Plan can&rsquo;t be changed until the trial ends.</p>
              )}
            </div>
            <Button
              loading={savingPlan}
              disabled={planDraft === vendor.subscriptionPlan || vendor.subscriptionStatus === "TRIALING"}
              onClick={handleSavePlan}
            >
              Save Plan
            </Button>

            <Select
              id="subscription-status-select"
              label="Billing Status (manual override)"
              value={subscriptionStatusDraft}
              onChange={(e) => setSubscriptionStatusDraft(e.target.value as SubscriptionStatus)}
              options={SUBSCRIPTION_STATUS_OPTIONS}
            />
            <Button
              loading={savingSubscription}
              disabled={subscriptionStatusDraft === vendor.subscriptionStatus}
              onClick={handleSaveSubscription}
            >
              Save Billing Status
            </Button>

            <Button variant="outlined" loading={generatingInvoice} onClick={handleGenerateInvoice}>
              Generate Payment Link
            </Button>
            </>
          )}
        </div>

        {generatedInvoice && (
          <div style={{ marginTop: "1rem", display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
            <span className="field-hint">
              ₹{(generatedInvoice.amountPaise / 100).toLocaleString("en-IN")} invoice created{" "}
              {new Date(generatedInvoice.createdAt).toLocaleDateString()}
            </span>
            <a href={generatedInvoice.paymentUrl} target="_blank" rel="noopener noreferrer">
              {generatedInvoice.paymentUrl}
            </a>
          </div>
        )}
      </Card>

      {(vendor.shopFrontPhotoUrl || vendor.gstCertificateUrl || vendor.ownerIdDocumentUrl) && (
        <Card title="KYC Documents">
          <div style={{ display: "flex", gap: "2rem", flexWrap: "wrap" }}>
            <div>
              <p className="field-hint">Shop Front Photo</p>
              {vendor.shopFrontPhotoUrl ? (
                <a href={resolveImagePath(vendor.shopFrontPhotoUrl)} target="_blank" rel="noopener noreferrer">
                  <img
                    src={resolveImagePath(vendor.shopFrontPhotoUrl)}
                    alt="Shop front"
                    style={{ width: 160, height: 120, objectFit: "cover", borderRadius: 4 }}
                  />
                </a>
              ) : (
                <p>Not uploaded</p>
              )}
            </div>
            <div>
              <p className="field-hint">GST Certificate</p>
              {vendor.gstCertificateUrl ? (
                <a href={resolveImagePath(vendor.gstCertificateUrl)} target="_blank" rel="noopener noreferrer">
                  <img
                    src={resolveImagePath(vendor.gstCertificateUrl)}
                    alt="GST certificate"
                    style={{ width: 160, height: 120, objectFit: "cover", borderRadius: 4 }}
                  />
                </a>
              ) : (
                <p>Not uploaded</p>
              )}
            </div>
            <div>
              <p className="field-hint">Owner ID (PAN / Aadhaar)</p>
              {vendor.ownerIdDocumentUrl ? (
                <a href={resolveImagePath(vendor.ownerIdDocumentUrl)} target="_blank" rel="noopener noreferrer">
                  <img
                    src={resolveImagePath(vendor.ownerIdDocumentUrl)}
                    alt="Owner ID document"
                    style={{ width: 160, height: 120, objectFit: "cover", borderRadius: 4 }}
                  />
                </a>
              ) : (
                <p>Not uploaded</p>
              )}
            </div>
          </div>
        </Card>
      )}

      {!isPartner && (
        <>
        <Card title={`Products (${products.length})`}>
          <Table data={products} columns={productColumns} rowKey={(row) => row.id} emptyMessage="No products yet." />
        </Card>

        <Card title={`Customers (${customers.length})`}>
          <Table data={customers} columns={customerColumns} rowKey={(row) => row.id} emptyMessage="No customers yet." />
        </Card>

        <Card title={`Invoices (${invoices.length})`}>
          <Table data={invoices} columns={invoiceColumns} rowKey={(row) => row.id} emptyMessage="No invoices yet." />
        </Card>

        <Card title={`Storefront Orders (${storefrontOrders.length})`}>
          <Table data={storefrontOrders} columns={orderColumns} rowKey={(row) => row.id} emptyMessage="No storefront orders yet." />
        </Card>
        </>
      )}
    </div>
  );
}
