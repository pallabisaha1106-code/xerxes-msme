import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { DefaultCatalogPicker } from "../components/DefaultCatalogPicker";
import { DASHBOARD_LOGIN_URL } from "../lib/dashboardUrl";
import { PartnerContactCard } from "../components/PartnerContactCard";
import { ApiRequestError, extractErrorMessage } from "../api/apiError";
import { useToast } from "../components/Toast";
import { listMembershipPlans, type MembershipPlanDto } from "../api/marketplaceApi";
import { PLAN_LABEL, pickerDescription } from "../lib/planFormat";
import {
  getVendorApplication,
  selectMembershipPlan,
  updateVendorListing,
  createVendorStarterProducts,
  submitVendorListing,
  type MembershipType,
  type VendorApplicationDetails,
} from "../api/vendorOnboardingApi";
import type { MarketplaceOrderMode } from "../types";

type Stage = "loading" | "not-found" | "ready" | "submitting" | "success" | "error";

export function VendorCataloguePage() {
  const toast = useToast();
  const { code = "" } = useParams<{ code: string }>();
  const [stage, setStage] = useState<Stage>("loading");
  const [details, setDetails] = useState<VendorApplicationDetails | null>(null);
  const [orderMode, setOrderMode] = useState<MarketplaceOrderMode>("WHATSAPP");
  const [enquiryWhatsapp, setEnquiryWhatsapp] = useState("");
  const [instagramUrl, setInstagramUrl] = useState("");
  const [facebookUrl, setFacebookUrl] = useState("");
  const [googleMapsUrl, setGoogleMapsUrl] = useState("");
  const [websiteUrl, setWebsiteUrl] = useState("");
  const [selectedDefaultProductIds, setSelectedDefaultProductIds] = useState<Set<string>>(new Set());
  const [planSaving, setPlanSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  // The plans come from the backend, so the prices and limits shown here are the ones it enforces and bills.
  const [plans, setPlans] = useState<MembershipPlanDto[] | null>(null);
  const [plansFailed, setPlansFailed] = useState(false);

  useEffect(() => {
    document.title = "Build Your Catalogue | Neighbourly";
  }, []);

  useEffect(() => {
    let cancelled = false;
    listMembershipPlans()
      .then((result) => {
        if (!cancelled) setPlans(result);
      })
      .catch(() => {
        if (!cancelled) setPlansFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!code) {
      setStage("not-found");
      return;
    }
    getVendorApplication(code)
      .then((data) => {
        setDetails(data);
        if (data.listing) {
          setOrderMode(data.listing.orderMode ?? "WHATSAPP");
          setEnquiryWhatsapp(data.listing.enquiryWhatsapp ?? "");
          setInstagramUrl(data.listing.instagramUrl ?? "");
          setFacebookUrl(data.listing.facebookUrl ?? "");
          setGoogleMapsUrl(data.listing.googleMapsUrl ?? "");
          setWebsiteUrl(data.listing.websiteUrl ?? "");
        }
        setStage("ready");
      })
      .catch((err) => {
        // A real 404 means the code itself is wrong - any other failure (network down, a
        // 500, etc.) is a different problem and showing "double-check your code" for it
        // would be misleading, so these two outcomes get separate stages/messages.
        setStage(err instanceof ApiRequestError && err.status === 404 ? "not-found" : "error");
      });
  }, [code]);

  function toggleDefaultProduct(id: string) {
    setSelectedDefaultProductIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handlePlanChange(plan: MembershipType) {
    if (!details || plan === details.subscriptionPlan) return;
    setPlanSaving(true);
    setErrorMessage(null);
    try {
      const updated = await selectMembershipPlan(code, plan);
      setDetails(updated);
      setSelectedDefaultProductIds(new Set());
    } catch (err) {
      const message = extractErrorMessage(err);
      setErrorMessage(message);
      toast.show({ severity: "error", summary: "Could not save your membership plan", detail: message });
    } finally {
      setPlanSaving(false);
    }
  }

  const remainingCapacity = details ? Math.max(details.productLimit - details.productCount, 0) : undefined;

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (orderMode === "WHATSAPP" && !enquiryWhatsapp.trim()) missing.push("WhatsApp number");
    return missing;
  }, [orderMode, enquiryWhatsapp]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (missingFields.length > 0) {
      setErrorMessage(`Before submitting, please add: ${missingFields.join(", ")}.`);
      return;
    }

    setStage("submitting");
    setErrorMessage(null);

    try {
      // Category and area were already set in Step 1 - only re-sent here
      // because updateListing() isn't a partial patch (it overwrites every
      // field it receives), so leaving them out would null them back out.
      await updateVendorListing(code, {
        categoryId: details?.listing?.categoryId ?? undefined,
        area: details?.listing?.area ?? undefined,
        instagramUrl: instagramUrl || undefined,
        facebookUrl: facebookUrl || undefined,
        googleMapsUrl: googleMapsUrl || undefined,
        websiteUrl: websiteUrl || undefined,
        enquiryWhatsapp: enquiryWhatsapp || undefined,
        orderMode,
      });
      if (selectedDefaultProductIds.size > 0) {
        await createVendorStarterProducts(code, [...selectedDefaultProductIds]);
      }
      await submitVendorListing(code);
      setStage("success");
    } catch (err) {
      const message = extractErrorMessage(err);
      setErrorMessage(message);
      toast.show({ severity: "error", summary: "Could not finish your marketplace listing", detail: message });
      setStage("ready");
    }
  }

  if (stage === "loading") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <p>Loading&hellip;</p>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  if (stage === "not-found") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <section className="join-result">
            <h1>We couldn&rsquo;t find that application</h1>
            <p>Double-check your registration code, or start a new application.</p>
            <Link className="primary-button" to="/list-your-business/catalogue">
              Try Again
            </Link>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  if (stage === "error") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <section className="join-result">
            <h1>We couldn&rsquo;t load your application</h1>
            <p>Something went wrong on our end. Please try again shortly.</p>
            <Link className="primary-button" to="/list-your-business/catalogue">
              Try Again
            </Link>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  if (stage === "success") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <section className="join-result">
            <h1>You&rsquo;re all set, {details?.businessName}!</h1>
            <p>
              Your marketplace listing has been submitted for review. Our team will take a look and your shop will
              go live once approved.
            </p>
            <p>
              You can log in anytime with your registered email and password to manage your listing and inventory
              at the Xerxes MSME dashboard (<code>{DASHBOARD_LOGIN_URL}</code>).
            </p>
            <Link className="primary-button" to="/shops">
              Back to Explore Shops
            </Link>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  if (!details) return null;

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="join-page join-page-wide">
        <h1>Build your catalogue, {details.businessName}</h1>
        <p className="join-subheading">Pick your membership plan and set up your marketplace listing.</p>
        <PartnerContactCard partner={details.partner} />

        <section className="join-section">
          <h2>Membership plan</h2>
          {plansFailed && <p>We couldn&rsquo;t load the plans right now. Please refresh the page.</p>}
          {!plans && !plansFailed && <p>Loading plans&hellip;</p>}
          {plans && (
            <div className="plan-picker">
              {plans.map((option) => (
                <label
                  key={option.plan}
                  className={`plan-picker-card${details.subscriptionPlan === option.plan ? " selected" : ""}`}
                >
                  <input
                    type="radio"
                    name="plan"
                    value={option.plan}
                    checked={details.subscriptionPlan === option.plan}
                    disabled={planSaving}
                    onChange={() => handlePlanChange(option.plan)}
                  />
                  <span className="plan-picker-label">{PLAN_LABEL[option.plan]}</span>
                  <span className="plan-picker-description">{pickerDescription(option)}</span>
                </label>
              ))}
            </div>
          )}
          <p>
            <a href="/pricing" target="_blank" rel="noopener noreferrer">
              Compare plans
            </a>
          </p>
        </section>

        {details.listing?.categoryId && (
          <DefaultCatalogPicker
            marketplaceCategoryId={details.listing.categoryId}
            selectedIds={selectedDefaultProductIds}
            onToggle={toggleDefaultProduct}
            remainingCapacity={remainingCapacity}
          />
        )}

        <form className="join-form" onSubmit={handleSubmit}>
          <section className="join-section">
            <h2>Your marketplace listing</h2>

            <label>
              How will customers order?
              <select value={orderMode} onChange={(e) => setOrderMode(e.target.value as MarketplaceOrderMode)}>
                <option value="WHATSAPP">WhatsApp enquiry</option>
                <option value="CHECKOUT">Checkout on the marketplace</option>
              </select>
            </label>
            {orderMode === "WHATSAPP" && (
              <label>
                WhatsApp number for enquiries
                <input required value={enquiryWhatsapp} onChange={(e) => setEnquiryWhatsapp(e.target.value)} />
              </label>
            )}

            <div className="checkout-form-row">
              <label>
                Instagram (optional)
                <input value={instagramUrl} onChange={(e) => setInstagramUrl(e.target.value)} />
              </label>
              <label>
                Facebook (optional)
                <input value={facebookUrl} onChange={(e) => setFacebookUrl(e.target.value)} />
              </label>
            </div>
            <div className="checkout-form-row">
              <label>
                Google Maps link (optional)
                <input value={googleMapsUrl} onChange={(e) => setGoogleMapsUrl(e.target.value)} />
              </label>
              <label>
                Website (optional)
                <input value={websiteUrl} onChange={(e) => setWebsiteUrl(e.target.value)} />
              </label>
            </div>
          </section>

          {errorMessage && <p className="checkout-error">{errorMessage}</p>}
          <button type="submit" className="primary-button" disabled={stage === "submitting"}>
            {stage === "submitting" ? "Submitting…" : "Submit Listing"}
          </button>
        </form>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
