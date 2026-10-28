import { useEffect } from "react";
import { Link } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { DASHBOARD_LOGIN_URL } from "../lib/dashboardUrl";

export function JoinLandingPage() {
  useEffect(() => {
    document.title = "List Your Business | Neighbourly";
  }, []);

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="join-page">
        <h1>Grow your business on Neighbourly</h1>
        <p className="join-subheading">
          Choose how you&rsquo;d like to get started. <Link to="/pricing">See pricing</Link>
        </p>
        <p className="join-subheading">
          Want to help local shops get listed? <Link to="/partners/apply">Become a local partner</Link>
        </p>

        <div className="join-landing-links">
          <Link className="join-landing-card" to="/list-your-business/apply">
            <h2>Register Your Retail Business</h2>
            <p>New here? Submit your business details and documents to get a registration code.</p>
          </Link>
          <Link className="join-landing-card" to="/list-your-business/catalogue">
            <h2>Grocery shop owner? Build your product catalogue</h2>
            <p>Already have a registration code? Pick your plan and set up your catalogue.</p>
          </Link>
          <a className="join-landing-card" href={DASHBOARD_LOGIN_URL}>
            <h2>Already listed? Manage your shop</h2>
            <p>Log in to the Xerxes MSME dashboard to manage your listing and inventory.</p>
          </a>
        </div>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
