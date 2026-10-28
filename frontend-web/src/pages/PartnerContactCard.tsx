import { useEffect, useState } from "react";
import { Card } from "../ui";
import { getMyPartnerContact, type PartnerContact } from "../api/partnerApi";

/** Shop owner's dashboard: who their local partner is, with Call and WhatsApp links. Shows nothing without a partner. */
export function PartnerContactCard() {
  const [partner, setPartner] = useState<PartnerContact | null>(null);

  useEffect(() => {
    getMyPartnerContact().then(setPartner).catch(() => setPartner(null));
  }, []);

  if (!partner) return null;

  return (
    <Card title="Your local partner">
      <p data-testid="partner-contact-name">
        <strong>{partner.name}</strong> is your point of contact for your shop on Neighbourly.
      </p>
      <p>
        <a href={`https://wa.me/${partner.whatsapp}`} target="_blank" rel="noopener noreferrer">
          WhatsApp
        </a>
        {" · "}
        <a href={`tel:+${partner.whatsapp}`}>Call</a>
      </p>
    </Card>
  );
}
