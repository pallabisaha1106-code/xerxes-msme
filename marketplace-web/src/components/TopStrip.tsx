import { socialLinksData } from "../data/socialLinks";
import { contactURL } from "../lib/urlSafety";

const ICONS: { key: keyof typeof socialLinksData; name: string; path: string }[] = [
  { key: "instagram", name: "Instagram", path: '<rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none"/>' },
  { key: "facebook", name: "Facebook", path: '<path d="M14 22v-9h3l.5-4H14V7c0-1.2.4-2 2-2h2V1.5A22 22 0 0 0 15 1c-3 0-5 1.8-5 5v3H7v4h3v9Z" fill="currentColor" stroke="none"/>' },
  { key: "youtube", name: "YouTube", path: '<rect x="2" y="5" width="20" height="14" rx="4"/><path d="m10 9 6 3-6 3Z" fill="currentColor" stroke="none"/>' },
  { key: "whatsapp", name: "WhatsApp", path: '<path d="M21 11.5a9 9 0 0 1-13.4 7.9L3 21l1.5-4.7A9 9 0 1 1 21 11.5Z"/><path d="m8 7 1.6 2.8-1.1 1.1c.8 1.6 2 2.8 3.7 3.5l1.1-1.2 2.7 1.5c-.2 1.6-1.5 2.3-3 1.8-3.5-1.1-6.2-3.8-7.1-7.1-.3-1.1.5-2.2 2.1-2.4Z" fill="currentColor" stroke="none"/>' },
];

export function TopStrip() {
  return (
    <div className="top-strip">
      <div className="top-strip-inner">
        <div className="top-socials" role="group" aria-label="Follow Neighbourly">
          {ICONS.map((icon) => {
            const href = contactURL(socialLinksData[icon.key]);
            if (!href) return null;
            return (
              <a key={icon.key} href={href} target="_blank" rel="noopener noreferrer" aria-label={`Neighbourly on ${icon.name}`}>
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                  dangerouslySetInnerHTML={{ __html: icon.path }}
                />
              </a>
            );
          })}
        </div>
        <p>Discover the small businesses that make Pune, PUNE.</p>
      </div>
    </div>
  );
}
