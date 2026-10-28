import type { ContactLinks } from "../types";

export interface ContactPlatform {
  key: keyof ContactLinks;
  name: string;
  iconPath: string;
}

/** Icon paths ported verbatim from marketplace.js's contactPlatforms array. */
export const CONTACT_PLATFORMS: ContactPlatform[] = [
  {
    key: "instagram",
    name: "Instagram",
    iconPath:
      '<rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none"/>',
  },
  {
    key: "facebook",
    name: "Facebook",
    iconPath:
      '<path d="M14 22v-9h3l.5-4H14V7c0-1.2.4-2 2-2h2V1.5A22 22 0 0 0 15 1c-3 0-5 1.8-5 5v3H7v4h3v9Z" fill="currentColor" stroke="none"/>',
  },
  {
    key: "youtube",
    name: "YouTube",
    iconPath: '<rect x="2" y="5" width="20" height="14" rx="4"/><path d="m10 9 6 3-6 3Z" fill="currentColor" stroke="none"/>',
  },
  {
    key: "googleMaps",
    name: "Google Maps",
    iconPath: '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
  },
  {
    key: "whatsapp",
    name: "WhatsApp",
    iconPath:
      '<path d="M21 11.5a9 9 0 0 1-13.4 7.9L3 21l1.5-4.7A9 9 0 1 1 21 11.5Z"/><path d="m8 7 1.6 2.8-1.1 1.1c.8 1.6 2 2.8 3.7 3.5l1.1-1.2 2.7 1.5c-.2 1.6-1.5 2.3-3 1.8-3.5-1.1-6.2-3.8-7.1-7.1-.3-1.1.5-2.2 2.1-2.4Z" fill="currentColor" stroke="none"/>',
  },
  {
    key: "website",
    name: "Website",
    iconPath: '<circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18M5 6.5h14M5 17.5h14"/>',
  },
];
