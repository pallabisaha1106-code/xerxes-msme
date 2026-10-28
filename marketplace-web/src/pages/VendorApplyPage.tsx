import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { applyForVendorAccount, getVendorApplicationConfig, type BusinessType } from "../api/vendorOnboardingApi";
import { extractErrorMessage } from "../api/apiError";
import { useToast } from "../components/Toast";
import { DASHBOARD_LOGIN_URL } from "../lib/dashboardUrl";
import { isValidEmail } from "../lib/emailFormat";
import { PartnerContactCard } from "../components/PartnerContactCard";
import type { PartnerContact } from "../api/partnerApi";
import {
  listMarketplaceCategories,
  listServiceStates,
  listServiceCities,
  listServiceAreas,
  type MarketplaceCategoryDto,
  type ServiceStateDto,
  type ServiceCityDto,
  type ServiceAreaDto,
} from "../api/marketplaceApi";

const BUSINESS_TYPE_OPTIONS: { value: BusinessType; label: string; description: string }[] = [
  { value: "RETAILER", label: "Retailer", description: "Shop selling directly to customers" },
  { value: "WHOLESALER", label: "Wholesaler", description: "Supplier selling in bulk to businesses" },
];

interface FormState {
  businessName: string;
  businessType: BusinessType;
  marketplaceCategoryId: string;
  ownerName: string;
  email: string;
  password: string;
  phone: string;
  stateCode: string;
  gstin: string;
  addressLine1: string;
  cityId: string;
  areaId: string;
  landmark: string;
}

const EMPTY_FORM: FormState = {
  businessName: "",
  businessType: "RETAILER",
  marketplaceCategoryId: "",
  ownerName: "",
  email: "",
  password: "",
  phone: "",
  stateCode: "",
  gstin: "",
  addressLine1: "",
  cityId: "",
  areaId: "",
  landmark: "",
};

type Stage = "idle" | "submitting" | "success";

export function VendorApplyPage() {
  const toast = useToast();
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [categories, setCategories] = useState<MarketplaceCategoryDto[]>([]);
  const [states, setStates] = useState<ServiceStateDto[]>([]);
  const [cities, setCities] = useState<ServiceCityDto[]>([]);
  const [areas, setAreas] = useState<ServiceAreaDto[]>([]);
  const [shopFrontPhoto, setShopFrontPhoto] = useState<File | null>(null);
  const [shopFrontPreview, setShopFrontPreview] = useState<string | null>(null);

  useEffect(() => {
    if (!shopFrontPhoto) {
      setShopFrontPreview(null);
      return;
    }
    const url = URL.createObjectURL(shopFrontPhoto);
    setShopFrontPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [shopFrontPhoto]);
  const [gstCertificate, setGstCertificate] = useState<File | null>(null);
  const [ownerIdDocument, setOwnerIdDocument] = useState<File | null>(null);
  // Safe default until the real value loads - requiring the uploads is the more conservative
  // behavior, so a slow/failed config fetch never lets someone skip them when they shouldn't.
  const [kycDocumentsRequired, setKycDocumentsRequired] = useState(true);
  const [stage, setStage] = useState<Stage>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [registrationCode, setRegistrationCode] = useState("");
  const [partner, setPartner] = useState<PartnerContact | null>(null);
  const [copied, setCopied] = useState(false);
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    document.title = "Register Your Business | Neighbourly";
    listMarketplaceCategories()
      .then(setCategories)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load business categories", detail: extractErrorMessage(err) }));
    listServiceStates()
      .then(setStates)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load GST states", detail: extractErrorMessage(err) }));
    // Silently keeps the safe (required) default on failure - not worth alarming an applicant
    // with a toast over a background settings check they haven't done anything wrong to cause.
    getVendorApplicationConfig()
      .then((config) => setKycDocumentsRequired(config.kycDocumentsRequired))
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Cascading: changing the state invalidates whatever city/area was picked
  // under the old one - registration is gated on Super Admin's curated list,
  // so there's no free-text fallback if a business's real city isn't here yet.
  useEffect(() => {
    setForm((current) => ({ ...current, cityId: "", areaId: "" }));
    setAreas([]);
    if (!form.stateCode) {
      setCities([]);
      return;
    }
    listServiceCities(form.stateCode)
      .then(setCities)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load cities", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.stateCode]);

  useEffect(() => {
    setForm((current) => ({ ...current, areaId: "" }));
    if (!form.cityId) {
      setAreas([]);
      return;
    }
    listServiceAreas(form.cityId)
      .then(setAreas)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load areas", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.cityId]);

  const selectedAreaPincode = areas.find((a) => a.id === form.areaId)?.pincode ?? "";

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!form.businessName.trim()) missing.push("business name");
    if (!form.marketplaceCategoryId) missing.push("business category");
    if (!form.stateCode) missing.push("GST state");
    if (!form.cityId) missing.push("city");
    if (!form.areaId) missing.push("area");
    if (!form.landmark.trim()) missing.push("landmark");
    if (!form.ownerName.trim()) missing.push("your name");
    if (!isValidEmail(form.email)) missing.push("a valid email address (like name@example.com)");
    if (form.password.length < 8) missing.push("a password of at least 8 characters");
    if (!shopFrontPhoto) missing.push("shop front photo");
    if (kycDocumentsRequired && !gstCertificate) missing.push("shop ACT or GST certificate");
    if (kycDocumentsRequired && !ownerIdDocument) missing.push("owner ID document (PAN / Aadhaar)");
    if (!confirmed) missing.push("confirmation that the details are accurate");
    return missing;
  }, [form, shopFrontPhoto, gstCertificate, ownerIdDocument, confirmed, kycDocumentsRequired]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (missingFields.length > 0 || !shopFrontPhoto) {
      setErrorMessage(`Before submitting, please add: ${missingFields.join(", ")}.`);
      return;
    }

    setStage("submitting");
    setErrorMessage(null);

    try {
      const response = await applyForVendorAccount(
        {
          businessName: form.businessName,
          businessType: form.businessType,
          marketplaceCategoryId: form.marketplaceCategoryId,
          gstin: form.gstin || undefined,
          stateCode: form.stateCode,
          addressLine1: form.addressLine1 || undefined,
          cityId: form.cityId,
          areaId: form.areaId,
          landmark: form.landmark,
          phone: form.phone || undefined,
          ownerName: form.ownerName,
          email: form.email.trim(),
          password: form.password,
        },
        shopFrontPhoto,
        gstCertificate,
        ownerIdDocument,
      );
      setRegistrationCode(response.registrationCode);
      setPartner(response.partner ?? null);
      setStage("success");
    } catch (err) {
      setStage("idle");
      const message = extractErrorMessage(err);
      setErrorMessage(message);
      toast.show({ severity: "error", summary: "Could not submit your application", detail: message });
    }
  }

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(registrationCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API unavailable in this browser - the code is already shown on screen to copy manually.
    }
  }

  if (stage === "success") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <section className="join-result">
            <h1>Application submitted!</h1>
            <p>
              Save this registration code - you&rsquo;ll need it to build your product catalogue and to check on
              your application later.
            </p>
            <p className="registration-code">{registrationCode}</p>
            <button type="button" className="primary-button" onClick={copyCode}>
              {copied ? "Copied!" : "Copy code"}
            </button>
            <p>
              Our team will review your documents next. Once approved, you can log in at the Xerxes MSME dashboard (
              <code>{DASHBOARD_LOGIN_URL}</code>) with the email and password you just set.
            </p>
            <PartnerContactCard partner={partner} />
            <Link className="primary-button" to={`/list-your-business/catalogue/${registrationCode}`}>
              Build My Catalogue Now
            </Link>
            <Link to="/shops">Back to Explore Shops</Link>
          </section>
        </main>
        <SiteFooter listBusinessLabel="List your business" />
      </>
    );
  }

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="join-page">
        <section className="registration-intro">
          <span className="eyebrow">JOIN NEIGHBOURLY</span>
          <h1>
            Put your business
            <br />
            <span>on the local map.</span>
          </h1>
          <p>
            Register your retail shop or wholesale business for review. Complete the form and upload a few
            documents - a Super Admin will review your application before your shop goes live.
          </p>
          <div className="registration-steps" aria-label="Registration process">
            <span>
              <b>1</b> Add your details
            </span>
            <span>
              <b>2</b> Select documents
            </span>
            <span>
              <b>3</b> Get your registration code
            </span>
          </div>
        </section>

        <form className="join-form" onSubmit={handleSubmit}>
          <p className="required-note">* Required fields</p>
          <section className="join-section">
            <h2>Business details</h2>
            <div className="plan-picker business-type-picker">
              {BUSINESS_TYPE_OPTIONS.map((option) => (
                <label
                  key={option.value}
                  className={`plan-picker-card${form.businessType === option.value ? " selected" : ""}`}
                >
                  <input
                    type="radio"
                    name="businessType"
                    value={option.value}
                    checked={form.businessType === option.value}
                    onChange={() => update("businessType", option.value)}
                  />
                  <span className="plan-picker-label">{option.label}</span>
                  <span className="plan-picker-description">{option.description}</span>
                </label>
              ))}
            </div>
            <label>
              Business category *
              <select
                required
                value={form.marketplaceCategoryId}
                onChange={(e) => update("marketplaceCategoryId", e.target.value)}
              >
                <option value="">Choose a category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.icon} {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Business name *
              <input required value={form.businessName} onChange={(e) => update("businessName", e.target.value)} />
            </label>
            <div className="checkout-form-row">
              <label>
                GSTIN (optional)
                <input value={form.gstin} onChange={(e) => update("gstin", e.target.value)} />
              </label>
              <label>
                GST state *
                <select required value={form.stateCode} onChange={(e) => update("stateCode", e.target.value)}>
                  <option value="">Choose your state</option>
                  {states.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <label>
              Address (optional)
              <input value={form.addressLine1} onChange={(e) => update("addressLine1", e.target.value)} />
            </label>
            <div className="checkout-form-row">
              <div className="join-field">
                <label htmlFor="vendor-apply-city">City *</label>
                <select
                  id="vendor-apply-city"
                  required
                  disabled={!form.stateCode}
                  value={form.cityId}
                  onChange={(e) => update("cityId", e.target.value)}
                >
                  <option value="">{form.stateCode ? "Choose your city" : "Choose a state first"}</option>
                  {cities.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="join-field">
                <label htmlFor="vendor-apply-area">Area *</label>
                <select
                  id="vendor-apply-area"
                  required
                  disabled={!form.cityId}
                  value={form.areaId}
                  onChange={(e) => update("areaId", e.target.value)}
                >
                  <option value="">{form.cityId ? "Choose your area" : "Choose a city first"}</option>
                  {areas.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            {form.stateCode && !cities.length && (
              <p className="join-section-note">
                We don&rsquo;t support any cities in this state yet - choose a different state, or contact us to
                request coverage.
              </p>
            )}
            {form.cityId && !areas.length && (
              <p className="join-section-note">
                We don&rsquo;t support any areas in this city yet - choose a different city, or contact us to request
                coverage.
              </p>
            )}
            <div className="checkout-form-row">
              <label>
                Pincode
                <input disabled value={selectedAreaPincode} placeholder="Set by the area you choose" />
              </label>
              <label>
                Landmark *
                <input required value={form.landmark} onChange={(e) => update("landmark", e.target.value)} />
              </label>
            </div>
          </section>

          <section className="join-section">
            <h2>Your account</h2>
            <p className="join-section-note">
              You&rsquo;ll use this email and password to log in once your application is approved.
            </p>
            <div className="checkout-form-row">
              <label>
                Your name *
                <input required value={form.ownerName} onChange={(e) => update("ownerName", e.target.value)} />
              </label>
              <label>
                Phone (optional)
                <input value={form.phone} onChange={(e) => update("phone", e.target.value)} />
              </label>
            </div>
            <div className="checkout-form-row">
              <label>
                Email *
                <input type="email" required value={form.email} onChange={(e) => update("email", e.target.value)} />
              </label>
              <label>
                Password (min. 8 characters) *
                <input
                  type="password"
                  required
                  minLength={8}
                  value={form.password}
                  onChange={(e) => update("password", e.target.value)}
                />
              </label>
            </div>
          </section>

          <section className="join-section">
            <h2>Verification documents</h2>
            <p className="join-section-note">
              {kycDocumentsRequired
                ? "These are reviewed by our team before your shop goes live - they aren’t shown to customers."
                : "Our team will verify your GSTIN and ID by phone before your shop goes live - no documents to upload."}
            </p>
            <div className="upload-grid">
              <label className="upload-card">
                <span className="upload-icon">▧</span>
                <b>Shop front photo *</b>
                <small>JPG, PNG or WebP · up to 8 MB</small>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  required
                  onChange={(e) => setShopFrontPhoto(e.target.files?.[0] ?? null)}
                />
                {shopFrontPreview && <img className="upload-preview" src={shopFrontPreview} alt="Shop front photo preview" />}
                <span className="file-name">{shopFrontPhoto?.name ?? "No file selected"}</span>
              </label>
              {kycDocumentsRequired && (
                <>
                  <label className="upload-card">
                    <span className="upload-icon">✓</span>
                    <b>Shop ACT or GST certificate *</b>
                    <small>Image or PDF · up to 8 MB</small>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,application/pdf"
                      required
                      onChange={(e) => setGstCertificate(e.target.files?.[0] ?? null)}
                    />
                    <span className="file-name">{gstCertificate?.name ?? "No file selected"}</span>
                  </label>
                  <label className="upload-card">
                    <span className="upload-icon">⌁</span>
                    <b>Owner PAN or Aadhaar *</b>
                    <small>Image or PDF · up to 8 MB</small>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,application/pdf"
                      required
                      onChange={(e) => setOwnerIdDocument(e.target.files?.[0] ?? null)}
                    />
                    <span className="file-name">{ownerIdDocument?.name ?? "No file selected"}</span>
                  </label>
                </>
              )}
            </div>
          </section>

          <label className="join-starter-checkbox">
            <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />
            I confirm these details are accurate and I am authorised to submit these documents for verification.
          </label>

          {errorMessage && <p className="checkout-error">{errorMessage}</p>}
          <button type="submit" className="primary-button" disabled={stage === "submitting"}>
            {stage === "submitting" ? "Submitting…" : "Submit Application"}
          </button>
        </form>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
