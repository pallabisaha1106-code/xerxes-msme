import type { PartnerContact } from "../api/partnerApi";
import { whatsappURL } from "../lib/urlSafety";

/**
 * Who a shop owner can turn to: their local partner, with Call and WhatsApp buttons. With no partner (nobody covers
 * their area, or theirs is switched off) it says the platform team will be in touch instead.
 */
export function PartnerContactCard({ partner }: { partner: PartnerContact | null | undefined }) {
  if (!partner) {
    return (
      <section className="partner-card">
        <h2>Your local partner</h2>
        <p>Our platform team will contact you shortly to help with your application.</p>
      </section>
    );
  }
  const whatsapp = whatsappURL(partner.whatsapp, `Hello ${partner.name}, I am applying to list my business on Neighbourly.`);
  return (
    <section className="partner-card">
      <h2>Your local partner</h2>
      <p>
        <strong>{partner.name}</strong> will help you with your application and is your point of contact.
      </p>
      <div className="partner-card-actions">
        {whatsapp && (
          <a className="primary-button" href={whatsapp} target="_blank" rel="noopener noreferrer">
            WhatsApp {partner.name}
          </a>
        )}
        <a className="secondary-button" href={`tel:+${partner.whatsapp}`}>
          Call {partner.name}
        </a>
      </div>
    </section>
  );
}
