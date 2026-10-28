import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { extractErrorMessage } from "../api/apiError";
import { applyAsPartner } from "../api/partnerApi";
import {
  listServiceAreas,
  listServiceCities,
  listServiceStates,
  type ServiceAreaDto,
  type ServiceCityDto,
  type ServiceStateDto,
} from "../api/marketplaceApi";
import { DASHBOARD_LOGIN_URL } from "../lib/dashboardUrl";
import { partnerFormProblems } from "../lib/partnerApplication";

type Stage = "idle" | "submitting" | "success";

/**
 * "Become a local partner": an independent sales partner asks to look after the shops in the areas they cover. Nothing
 * is switched on until Super Admin approves; the applicant then signs in to the dashboard with the email and password
 * set here.
 */
export function PartnerApplyPage() {
  const [form, setForm] = useState({ name: "", whatsapp: "", email: "", password: "" });
  const [stateCode, setStateCode] = useState("");
  const [cityId, setCityId] = useState("");
  const [areaIds, setAreaIds] = useState<string[]>([]);
  const [states, setStates] = useState<ServiceStateDto[]>([]);
  const [cities, setCities] = useState<ServiceCityDto[]>([]);
  const [areas, setAreas] = useState<ServiceAreaDto[]>([]);
  const [stage, setStage] = useState<Stage>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [appliedName, setAppliedName] = useState("");

  useEffect(() => {
    document.title = "Become a Local Partner | Neighbourly";
    listServiceStates().then(setStates).catch(() => setErrorMessage("Could not load the list of states. Please refresh the page."));
  }, []);

  // Cascading, like the shop application form: a new state clears the city and areas picked under the old one.
  useEffect(() => {
    setCityId("");
    setAreas([]);
    setAreaIds([]);
    if (!stateCode) {
      setCities([]);
      return;
    }
    listServiceCities(stateCode).then(setCities).catch(() => setCities([]));
  }, [stateCode]);

  useEffect(() => {
    setAreaIds([]);
    if (!cityId) {
      setAreas([]);
      return;
    }
    listServiceAreas(cityId).then(setAreas).catch(() => setAreas([]));
  }, [cityId]);

  function update(key: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function toggleArea(id: string) {
    setAreaIds((current) => (current.includes(id) ? current.filter((a) => a !== id) : [...current, id]));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const problems = partnerFormProblems({ ...form, areaIds });
    if (problems.length > 0) {
      setErrorMessage(`Before sending, please add: ${problems.join(", ")}.`);
      return;
    }
    setStage("submitting");
    setErrorMessage(null);
    try {
      const response = await applyAsPartner({
        name: form.name.trim(),
        whatsapp: form.whatsapp.trim(),
        email: form.email.trim(),
        password: form.password,
        areaIds,
      });
      setAppliedName(response.name);
      setStage("success");
    } catch (err) {
      setErrorMessage(extractErrorMessage(err));
      setStage("idle");
    }
  }

  if (stage === "success") {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="join-page">
          <section className="join-result">
            <h1>Application received</h1>
            <p>
              Thank you, {appliedName}. Our team will review your application. Once it is approved, you can sign in at the
              Xerxes MSME dashboard (<code>{DASHBOARD_LOGIN_URL}</code>) with the email and password you just set.
            </p>
            <Link className="primary-button" to="/">
              Back to Neighbourly
            </Link>
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
        <h1>Become a local partner</h1>
        <p className="join-subheading">
          Help the shops in your neighbourhood get listed on Neighbourly. As a local partner you are their point of
          contact and you review their applications. Tell us about yourself and the areas you can cover.
        </p>

        <form className="join-form" onSubmit={handleSubmit}>
          <section className="join-section">
            <h2>About you</h2>
            <div className="join-field">
              <label htmlFor="partner-name">Your name *</label>
              <input id="partner-name" required value={form.name} onChange={(e) => update("name", e.target.value)} />
            </div>
            <div className="checkout-form-row">
              <label>
                WhatsApp number *
                <input
                  type="tel"
                  required
                  placeholder="98765 43210"
                  value={form.whatsapp}
                  onChange={(e) => update("whatsapp", e.target.value)}
                />
              </label>
              <label>
                Email *
                <input type="email" required value={form.email} onChange={(e) => update("email", e.target.value)} />
              </label>
            </div>
            <div className="join-field">
              <label htmlFor="partner-password">Password (min. 8 characters) *</label>
              <input
                id="partner-password"
                type="password"
                required
                autoComplete="new-password"
                value={form.password}
                onChange={(e) => update("password", e.target.value)}
              />
            </div>
          </section>

          <section className="join-section">
            <h2>Where you can help</h2>
            <p className="join-section-note">Choose your state and city, then tick every area you can cover.</p>
            <div className="checkout-form-row">
              <label>
                State *
                <select value={stateCode} onChange={(e) => setStateCode(e.target.value)}>
                  <option value="">Choose a state</option>
                  {states.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                City *
                <select value={cityId} disabled={!stateCode} onChange={(e) => setCityId(e.target.value)}>
                  <option value="">{stateCode ? "Choose a city" : "Choose a state first"}</option>
                  {cities.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {cityId && areas.length === 0 && <p className="join-section-note">No areas are open in this city yet.</p>}
            <div className="partner-area-grid" role="group" aria-label="Areas you can cover">
              {areas.map((area) => (
                <label key={area.id} className="partner-area-option">
                  <input type="checkbox" checked={areaIds.includes(area.id)} onChange={() => toggleArea(area.id)} />
                  <span>
                    {area.name} <small>{area.pincode}</small>
                  </span>
                </label>
              ))}
            </div>
          </section>

          {errorMessage && (
            <p className="join-error" role="alert">
              {errorMessage}
            </p>
          )}
          <button type="submit" className="primary-button" disabled={stage === "submitting"}>
            {stage === "submitting" ? "Sending…" : "Send application"}
          </button>
        </form>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
