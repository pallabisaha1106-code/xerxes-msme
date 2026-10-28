import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { whatsappURL } from "../lib/urlSafety";

export function OnboardingSection({
  eyebrow,
  heading,
  paragraph,
  buttonLabel,
  listItems,
  adminWhatsapp,
  submissionMessage,
  onDemoContactClick,
  ctaTo,
}: {
  eyebrow: string;
  heading: ReactNode;
  paragraph: string;
  buttonLabel: string;
  listItems: string[];
  adminWhatsapp: string;
  submissionMessage: string;
  onDemoContactClick: () => void;
  /** An in-app route (e.g. "/list-your-business") that takes priority over the WhatsApp/demo-notice fallback below - see ExploreShopsPage vs ExploreWholesalerPage. */
  ctaTo?: string;
}) {
  const adminURL = whatsappURL(adminWhatsapp, submissionMessage);

  return (
    <section id="list-business" className="onboarding">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h2>{heading}</h2>
        <p>{paragraph}</p>
        {ctaTo ? (
          <Link className="primary-button admin-link" to={ctaTo}>
            {buttonLabel}
          </Link>
        ) : adminURL ? (
          <a className="primary-button admin-link" href={adminURL} target="_blank" rel="noopener noreferrer">
            {buttonLabel}
          </a>
        ) : (
          <a
            className="primary-button admin-link demo-contact"
            href="#"
            onClick={(e) => {
              e.preventDefault();
              onDemoContactClick();
            }}
          >
            {buttonLabel}
          </a>
        )}
      </div>
      <div className="submission-details">
        <h3>Your shop’s introduction, in one message.</h3>
        <p>Send these details to get started:</p>
        <ol>
          {listItems.map((item, i) => (
            <li key={i}>{item}</li>
          ))}
        </ol>
        <span>No sign-ups. Just a conversation.</span>
      </div>
    </section>
  );
}
