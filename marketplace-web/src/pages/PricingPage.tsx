import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { listMembershipPlans, type MembershipPlanDto } from "../api/marketplaceApi";
import { PLAN_LABEL, isPaid, priceText, productLimitText } from "../lib/planFormat";

export function PricingPage() {
  // The prices and limits come from the backend, so this page can never disagree with what is enforced and billed.
  const [plans, setPlans] = useState<MembershipPlanDto[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    document.title = "Pricing | Neighbourly";
  }, []);

  useEffect(() => {
    let cancelled = false;
    listMembershipPlans()
      .then((result) => {
        if (!cancelled) setPlans(result);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <main className="join-page join-page-wide pricing-page">
        <h1>Simple pricing for local businesses</h1>
        <p className="join-subheading">Every plan starts with a 30-day free trial. Pick the size of catalogue you need.</p>

        {failed && <p>We couldn&rsquo;t load the plans right now. Please try again shortly.</p>}
        {!plans && !failed && <p>Loading plans&hellip;</p>}
        {plans && (
          <div className="pricing-grid">
            {plans.map((plan) => (
              <section className="pricing-card" key={plan.plan} aria-labelledby={`plan-${plan.plan}`}>
                <h2 id={`plan-${plan.plan}`}>{PLAN_LABEL[plan.plan]}</h2>
                <p className="pricing-price">
                  {priceText(plan)} <span>{isPaid(plan) ? "a month" : "forever"}</span>
                </p>
                <p className="pricing-gst">{isPaid(plan) ? "plus GST" : ""}</p>
                <p className="pricing-limit">{productLimitText(plan)}</p>
                <Link className="primary-button" to="/list-your-business/apply">
                  Register your business
                </Link>
              </section>
            ))}
          </div>
        )}

        <section className="pricing-details">
          <h2>Included in every plan</h2>
          <ul>
            <li>Your shop page on Neighbourly, found by shoppers in your city</li>
            <li>Orders and enquiries on WhatsApp, or checkout on your shop page</li>
            <li>Offers on the Trending page</li>
            <li>Appointment requests for salons and clinics</li>
          </ul>
        </section>

        <div className="pricing-columns">
          <section className="pricing-details">
            <h2>How billing works</h2>
            <ul>
              <li>Your 30 days start when you apply</li>
              <li>Free carries on at no cost after the trial</li>
              <li>Plus and Premium: we send a payment link each month</li>
              <li>If a payment is missed you have 7 days, then your shop is hidden until it is paid</li>
            </ul>
          </section>
          <section className="pricing-details">
            <h2>Common questions</h2>
            <p>
              <strong>Can I change plan later?</strong>
              <br />
              Yes. Contact us and we will switch it.
            </p>
            <p>
              <strong>Is GST included?</strong>
              <br />
              No. Prices are shown without GST.
            </p>
          </section>
        </div>
      </main>
      <SiteFooter listBusinessLabel="List your business" />
    </>
  );
}
