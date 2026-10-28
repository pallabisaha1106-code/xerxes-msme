import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { getVendorApplication } from "../api/vendorOnboardingApi";
import { extractErrorMessage } from "../api/apiError";
import { useToast } from "../components/Toast";

export function VendorCatalogueEntryPage() {
  const toast = useToast();
  const [code, setCode] = useState("");
  const [checking, setChecking] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    document.title = "Build Your Catalogue | Neighbourly";
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = code.trim().toUpperCase();
    if (!trimmed) {
      setErrorMessage("Enter the registration code from your application.");
      return;
    }
    setChecking(true);
    setErrorMessage(null);
    try {
      await getVendorApplication(trimmed);
      navigate(`/list-your-business/catalogue/${trimmed}`);
    } catch (err) {
      const message = extractErrorMessage(err);
      setErrorMessage(message);
      toast.show({ severity: "error", summary: "Could not find that registration code", detail: message });
    } finally {
      setChecking(false);
    }
  }

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="join-page">
        <h1>Build your product catalogue</h1>
        <p className="join-subheading">
          Enter the registration code you received after applying to pick your plan and set up your catalogue.
        </p>

        <form className="join-form" onSubmit={handleSubmit}>
          <section className="join-section">
            <label>
              Registration code
              <input
                required
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="e.g. AB3D9KQM"
                autoCapitalize="characters"
              />
            </label>
          </section>
          {errorMessage && <p className="checkout-error">{errorMessage}</p>}
          <button type="submit" className="primary-button" disabled={checking}>
            {checking ? "Checking…" : "Continue"}
          </button>
        </form>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
