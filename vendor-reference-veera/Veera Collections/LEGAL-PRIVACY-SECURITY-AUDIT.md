# Veera Collections — legal, licensing, privacy and security audit

Audit date: 8 August 2026. This is a technical and content audit, not legal advice.

## Critical live-deployment mismatch

At the audit date, `https://veeracollections.com/` serves a materially different ecommerce-style website from this local static project. The live page includes named testimonials, an email subscription field, shipping/payment claims, a different contact presentation and ecommerce/cart behaviour that are not present in the audited files. The HTTP address resolves to HTTPS, but the live content mismatch means privacy, licensing, claims, data collection and security conclusions in this report apply only to this repository. **ACTION REQUIRED OUTSIDE THE CODEBASE:** confirm who controls the domain/hosting, decide which site is authoritative, and deploy the reviewed project only after backing up and separately auditing any live ecommerce system that must be retained.

## Project inventory

- 11 HTML pages, including the new privacy notice and website terms
- 8 CSS files and 4 first-party JavaScript files
- 17 catalog JPEGs, 2 campaign JPEGs, 1 logo PNG, 1 Apple touch icon and 1 SVG favicon
- 1 local Google-review data file; no locally published reviewer names or review text
- No forms, iframes, videos, PDFs, local font files, package manager, third-party JS libraries, service worker, source maps or build dependencies
- No login, SSO, OAuth, JWT, authentication cookies, API keys, passwords, payment processing or backend
- No analytics script, advertising pixel, cookie-writing code, localStorage, sessionStorage or IndexedDB use

## Image and brand licensing inventory

No licence documents, invoices, photographer assignments, supplier permissions, model releases or trademark records are stored in this project. “Unknown” therefore means the repository does not prove the right; it does not assert infringement.

| Files | Use/type | Source recorded | Licence evidence | Risk | Required action |
|---|---|---|---|---|---|
| `assets/catalog/*.jpeg` (17 files) | Product/model photographs across catalog, category and social-style sections | Described as business product references; original supplier/photographer not recorded | No | High | **LICENSE STATUS UNKNOWN — VERIFY BEFORE COMMERCIAL USE.** Confirm image copyright permission and identifiable-model commercial releases with the owner/supplier. |
| `veera-inclusive-boutique-campaign-v2.jpg` | Homepage hero with identifiable people | Project notes say generated specifically for Veera Collections | No generation record or applicable commercial-use terms retained | Medium | Retain the generation record and service terms that applied on the generation date; confirm commercial-use and synthetic-person disclosure requirements. |
| `veera-inclusive-size-editorial-v2.jpg` | Inclusive-size feature with an identifiable person | Project notes say generated specifically for Veera Collections | No generation record or applicable commercial-use terms retained | Medium | Same verification as the hero image; do not represent the person as a customer or real product endorser. |
| `veera-collections-official-logo.png` / `.webp` | Header, footer, app icons and social metadata | Official logo supplied by the business owner on 16 August 2026 | Source image retained in the project; formal design assignment/trademark record not included | Low | Retain the original artwork and any designer/source documentation with the business records. |
| `apple-touch-icon.png` | Device icon | Project asset, apparently brand-derived | No | Low | Confirm it is derived only from authorised Veera artwork. |
| `favicon.svg` | Browser icon | Project asset | No authorship record | Low | Confirm the symbol is original/authorised and does not copy another brand mark. |

No competitor logos, map screenshots, customer profile photographs or copied Google-review text were found.

## Fonts, icons and software

| Item | Source/version | Licence/purpose | Status |
|---|---|---|---|
| DM Sans | Google Fonts CSS API; version selected by Google’s current response | Google Fonts identifies its fonts as open-source; used for interface/body text | Required; HTTPS; verify/retain the exact font licence notice if self-hosted later. |
| Playfair Display | Google Fonts CSS API; version selected by Google’s current response | Open-source Google Fonts family; used for headings | Required; HTTPS; same record-keeping action. |
| Unicode symbols | Native characters, no icon library | Decorative/navigation symbols | Required; no third-party package. |
| First-party JavaScript | `site.js`, `main.js`, `catalog.js`, `google-reviews.js`; unversioned local code | Shared components, catalog rendering and reviews | Required. No npm/CDN JavaScript dependency. |

## Third-party services and privacy

| Service/domain | Purpose | Data potentially shared | Necessary? | Action |
|---|---|---|---|---|
| Google Fonts (`fonts.googleapis.com`, `fonts.gstatic.com`) | Typography loaded on page view | IP address, browser/request metadata and referrer as processed by Google | Design dependency, but can be replaced by properly licensed self-hosted files | Disclosed in privacy notice; consider official self-hosting to reduce third-party page-load requests. |
| WhatsApp (`wa.me`) | Customer-initiated enquiries | Data the visitor sends plus WhatsApp account/device metadata | Optional, conversion-critical link | Opens only after a click; no widget/script; privacy notice added. |
| Google Maps/Reviews (`maps.app.goo.gl`) | Directions and current reviews | Google account/device/location/activity data after click | Optional link | No embed or API; verify the short link belongs to the correct business listing. |
| Instagram (`instagram.com/veera.collectionss`) | Official social profile | Meta account/device/activity data after click | Optional link | No embed/pixel; verify business ownership of the account. |

The site has no direct form collection and no confirmed server-side customer-data store. Enquiries occur through third parties. Retention practices outside the website remain an owner decision and are not invented in the privacy notice.

## Google reviews and customer content

The local data file contains a displayed rating of 5.0, no review count and no review records. The interface directs visitors to Google for the current count and review text. Before launch, the owner must verify that the displayed 5.0 remains accurate or remove the numeric value. If reviews are later copied locally, retain source/date evidence, minimise reviewer identity, preserve wording faithfully and review Google’s then-current display terms.

## Code security findings

- No secrets, credentials, private keys, tokens, authentication code or confidential customer records found.
- No `eval`, `new Function`, `document.write`, dynamic script injection, user-controlled redirect, URL-query insertion or browser storage found.
- Local component markup is inserted with `innerHTML`; its source is trusted same-origin project content and is constrained by `connect-src 'self'`. Review content fields use `textContent`.
- Catalog cards use `innerHTML` only with fixed, first-party arrays. No visitor input reaches those templates.
- Every external new-tab link inspected has `rel="noopener noreferrer"`.
- No forms, service worker, source maps, debug build, archive, backup or temporary deployment files found.
- HTTPS external resources are used. The HTTP URL in `sitemap.xml` is the XML namespace identifier, not a fetched mixed-content resource.

## Implemented in the project

- Added factual privacy and website-terms pages and shared-footer links.
- Added a qualified copyright notice without claiming third-party ownership.
- Added the legal pages to clean-URL redirects and the sitemap.
- Strengthened the deployable CSP with exact hashes for the two trusted JSON-LD blocks; no `unsafe-eval`, wildcard source or broad third-party script domain.
- Added `Cross-Origin-Opener-Policy` and `Cross-Origin-Resource-Policy`.
- Kept clickjacking protection through `frame-ancestors 'none'` and `X-Frame-Options: DENY`.
- Reduced the asset cache lifetime because filenames are not content-hashed; removed `immutable` to prevent stale replacements.
- Removed HSTS `preload` and `includeSubDomains` until every subdomain and long-term HTTPS commitment is owner-verified.

## Hosting/CDN configuration required

The `_headers` and `_redirects` files work only on hosts that support their syntax (for example, compatible static hosts). Confirm in a production response, not merely in the repository:

- Force HTTP → HTTPS and choose one canonical host (`www` is used in metadata).
- Serve the `_headers` policy: CSP, HSTS, `nosniff`, referrer policy, permissions policy, COOP/CORP and frame protection.
- Confirm all active and future subdomains support HTTPS before considering HSTS `includeSubDomains`; submit to the preload list only after deliberate owner approval.
- Serve correct MIME types for HTML, CSS, JavaScript, JSON, SVG, PNG, JPEG and web manifest files.
- Apply revalidation to HTML/CSS/JS/components/data and the documented seven-day cache to image assets; use content-hashed filenames before enabling year-long immutable caching.
- Enable Brotli or gzip for HTML, CSS, JavaScript, JSON, XML and SVG.
- Keep ETags enabled if the host generates correct validators; do not expose internal filesystem metadata.
- Configure DNSSEC if supported by the registrar/DNS provider and protect registrar, DNS and hosting accounts with MFA.
- Enable deployment/audit logs and uptime/security monitoring; restrict administrative access to named users.
- Verify redirects, custom 404 handling and canonical `www` certificate coverage after deployment.

## Manual owner/legal actions

1. Reconcile the different website currently served by the public domain; separately audit or back it up before replacement.
2. Obtain and retain rights evidence for all 17 product/model photographs and any needed model releases.
3. Retain the AI-generation record and commercial-use terms for both campaign images.
4. Confirm ownership/licence of the logo, favicon and Apple icon; consider trademark advice separately.
5. Have the privacy notice and website terms reviewed against actual shop practices, especially enquiry retention, returns/exchanges and applicable consumer obligations.
6. Verify the public phone number, address, official Instagram profile and Google Maps listing.
7. Recheck the displayed Google rating immediately before launch and establish a process to keep it current.
8. Confirm the production host actually serves `_headers`, `_redirects`, the canonical HTTPS redirect and correct MIME/cache headers.
9. Decide whether to self-host officially sourced Google Fonts to reduce third-party page-load disclosure.
10. Document operational handling and deletion of enquiries received through WhatsApp/Instagram; do not publish a retention promise until confirmed.
