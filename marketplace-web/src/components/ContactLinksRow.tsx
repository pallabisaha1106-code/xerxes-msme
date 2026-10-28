import { CONTACT_PLATFORMS } from "../lib/contactPlatforms";
import { contactURL } from "../lib/urlSafety";
import type { Business } from "../types";

/** Ported from marketplace.js's renderContactLinks - hidden entirely if every link is missing/invalid. */
export function ContactLinksRow({ business }: { business: Pick<Business, "businessName" | "links"> }) {
  const buttons = CONTACT_PLATFORMS.map((platform) => {
    const href = contactURL(business.links?.[platform.key]);
    if (!href) return null;
    const label = business.businessName + (platform.key === "website" ? " website" : " on " + platform.name);
    return (
      <a
        key={platform.key}
        className="contact-link"
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={label}
        title={label}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
          focusable="false"
          dangerouslySetInnerHTML={{ __html: platform.iconPath }}
        />
      </a>
    );
  }).filter(Boolean);

  if (buttons.length === 0) return null;
  return <div className="card-actions">{buttons}</div>;
}
