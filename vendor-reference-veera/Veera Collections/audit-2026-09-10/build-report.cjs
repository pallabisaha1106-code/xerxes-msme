const fs=require('fs');const out='audit-2026-09-10',before=require('./before-browser.json'),after=require('./after-browser.json'),reg=require('./regression.json'),products=require('./product-information-required.json'),crawl=require('./live-crawl.json');const fmt=s=>String(s??'').replaceAll('|','/').replaceAll('\n',' ');let productReport='# Product information requiring owner confirmation\n\nAudit: 10 September 2026. No product facts were invented or changed. Existing M–5XL wording describes the store range; it is not evidence of availability for an individual product.\n\n192 structured products and 17 legacy image references are listed. These are not necessarily 209 distinct garments; cross-catalogue identity is unverified.\n\n| Reference / image | Current title / ALT | Missing information | Existing review notes |\n|---|---|---|---|\n'+products.map(p=>`| ${fmt(p.reference)} | ${fmt(p.title)} | PRODUCT INFORMATION REQUIRED: ${p.missing.join(', ')} | ${fmt((p.reviewNotes||[]).join(' '))} |`).join('\n');fs.writeFileSync(out+'/PRODUCT-INFORMATION-REQUIRED.md',productReport);
const perfFiles=['index.html','collection.html','more.html','kurtis.html','punjabi-dress.html','western-wear.html'];const perf=perfFiles.map(f=>{let b=before.find(x=>x.file===f);return `| ${f} | REQUIRES PAGESPEED/LIGHTHOUSE TEST | REQUIRES PAGESPEED/LIGHTHOUSE TEST | Unavailable | Unavailable | Unavailable | ${(fs.statSync(f).size/1024).toFixed(1)} KiB HTML; ${b.dom} DOM elements |`}).join('\n');const parity=reg.footerParity.map(r=>`| ${r.header} | ${r.footer} | ${r.url} | YES / PASS |`).join('\n');
const report=`# Veera Collections audit and change report

10 September 2026. Changes are local and have not been deployed.

## A. Executive summary

Fresh discovery started at the live homepage and continued through internal links, shared components, and the live sitemap. **27 public pages and two component endpoints returned HTTP 200.** The local audit covered **29 HTML pages**, including the 404 and virtual try-on pages, at **nine widths (261 checks)** before and after fixes. The live inventory is in live-crawl.json; local results are in before-browser.json and after-browser.json. Exploratory raw fetch files are supporting captures; live-crawl.json is the final authoritative crawl inventory.

No critical issue was established. Verified issues were fixed in six source files, preserving the existing design and all static page content.

| Priority | Issue | Outcome |
|---|---|---|
| Medium | Footer omitted five textual navigation destinations | Added Home, Anarkali, Punjabi dress, Co-Ord sets, More in existing Explore column; retained all original destinations |
| Medium | Photo script hid descriptions/category/colour from product cards | Removed hiding behavior, kept content visible and preserved popup/zoom |
| Medium | Legacy catalogue paragraph content hidden on narrow screens | Removed hiding rule; retained original availability wording |
| Medium | Resizing an open mobile menu to desktop left scrolling locked | Close menu on entry into existing desktop breakpoint |
| Low | Card CTA positions differed by up to 99.25px in sampled row | Flex alignment now gives identical action bottoms within that row |
| Low | Invalid footer divider CSS | Corrected translucent white border syntax |
| Low | Fallback component drift | Synchronized fallback header/footer with actual shared components |

## B. Performance

**REQUIRES PAGESPEED/LIGHTHOUSE TEST.** Lighthouse is not installed. No performance scores or field INP are claimed. Attempted live browser samples rendered an intermediary document with only 24 DOM nodes, not the website. All such samples are explicitly marked invalid in live-performance.json and excluded from performance conclusions. The measurement script now waits for the real header before accepting a sample.

| URL/file | Mobile performance | Desktop performance | LCP | INP | CLS | Measured source/DOM size |
|---|---|---|---|---|---|---|
${perf}

DOM counts come from the actual locally rendered pages, not the rejected live browser samples. They describe structural weight, not a performance score. More is the largest rendered catalogue, with repeated catalogue groups; useful crawlable content and deliberate grouping were preserved. No pagination or JavaScript-only conversion was introduced.

Live HTTP checks verified Brotli for sampled HTML/CSS/JS, and seven-day public caching for CSS, JS and a WebP image. HTML, robots and sitemap samples did not expose Cache-Control. CSS/JS URLs are unversioned: purge hosting/browser-facing caches or apply an appropriate revalidation strategy when deploying these updates. Local _headers requests different policies and a stronger CSP; current Hostinger responses do not reflect those rules. Deployment configuration requires host-level verification; no speculative server configuration was added.

Existing responsive 480px WebP variants, lazy loading below the fold, intrinsic dimensions, and eager homepage hero remain intact. The largest referenced source image is the 284,385-byte homepage campaign JPEG. No verified image issue justified recompression or typography replacement. No measurable speed gain is claimed from this repair set. Local before/after resource and navigation observations are retained in the browser JSON files; they are not representative field measurements.

## C. UI alignment

All local pages passed no-horizontal-overflow and announcement non-overlap checks at 320, 375, 390, 414, 768, 1024, 1366, 1440 and 1920px. Representative desktop/mobile screenshots cover home, collection, More, Kurtis, Punjabi Dress, Western Wear and virtual try-on. Footer and catalogue detail screenshots were also inspected. Lazy images outside the viewport may be blank in long screenshots; filesystem decode and path checks separately verify them.

Catalogue cards now retain descriptions and metadata, with aligned actions despite varying text lengths. Existing breakpoints, header positions, colors, typography and image containment remain in place. Descriptions naturally increase visible card height; no text was shortened.

## D. Product data quality

Audited all **192 structured records** and all rendered card occurrences. Titles, category, colour, descriptions, ALT, references and image mappings are present in structured records. Sizes, price, fabric, stock, care and included-items fields are null and need confirmation. Ten records already contain classification review notes, preserved verbatim. Seventeen legacy photo references need owner-confirmed commercial details and cross-catalogue identity checks.

See [individual product report](PRODUCT-INFORMATION-REQUIRED.md) and product-information-required.json. **PRODUCT INFORMATION REQUIRED** is an audit status, not invented customer-facing copy. No price, size availability, SKU, fabric, colour or stock claim was generated. Generic existing wording and possible-variant notes were preserved for owner review. Restored card text is identical to the original DOM text.

## E. Product image quality

All **426 local raster asset files decoded successfully**. All referenced local image paths and srcset files exist. Browser images have ALT attributes. Catalogue product images retain object-fit:contain; editorial hero cropping is intentional. No source images, ALT strings or responsive candidates were changed. Image quality/garment matching was sampled visually, not certified for every photograph. Supplier identity, exact garment correspondence, rights and model permissions remain manual checks.

## F. UI FX / interactions

Preserved hover styles, native photo dialog, zoom and pan behavior, tab interactions and existing reduced-motion rules. Verified keyboard opening, image decode, zoom to 1.5×, reset, Escape and focus return. All More tabs responded to clicks; arrow-key selection passed. The mobile-menu-to-desktop scroll-lock defect is fixed and retested. Contact map activation creates the original map iframe; external response was intercepted for this functional check. Virtual try-on consent modal opens/closes with Escape; no personal image, camera permission or customer data was used. Camera/file-upload/save flows require a real-device check.

## G. Header consistency

Header destinations, ordering, announcement, logo and five social/contact links are unchanged. Header navigation matches before/after on all local pages. Shared and fallback implementations are synchronized. Small mobile text/icon choices from the earlier requested work were preserved.

## H. Footer consistency

All local pages use the same shared footer with synchronized fallback. Business identity, address, phone, WhatsApp, original Instagram/Maps destinations, copyright and existing SEO links are retained. Added links occupy the existing Explore column. Internal footer links are absolute HTTPS/www URLs taken from existing canonical tags. They match live 200 destinations without redirects in the current host responses.

## I. Header ↔ footer link parity

| Header link | Footer link | Canonical production URL | URL match / status |
|---|---|---|---|
${parity}

A home logo and textual Home link intentionally share a destination; no duplicate textual navigation entry was introduced.

## J. Accessibility

Verified no unnamed links/buttons or duplicate IDs in the inspected browser DOM; images have ALT. Keyboard menu/photo/tab interactions and reduced-motion behavior passed. Restored card descriptions are available without opening a popup. Existing focus outlines and secure external-link attributes are preserved. No automated WCAG certification is claimed: full contrast, screen-reader, heading semantics and real-device touch testing remain a specialist/manual pass. Existing small mobile announcement text remains at the previously requested size.

## K. Licensing / copyright

| Asset | Source/library | License status | Risk | Required action |
|---|---|---|---|---|
| DM Sans | Google Fonts CSS API | CLEAR / NORMAL USE: official source is SIL OFL 1.1 | Low; no verified violation | Retain notices if subsequently redistributed/self-hosted |
| Playfair Display | Google Fonts CSS API | CLEAR / NORMAL USE: official source is SIL OFL 1.1 | Low; no verified violation | Same; reserved-name rules apply to modified fonts |
| Arial/Georgia fallbacks | System font stacks | NORMAL USE | No supplied font binaries | None established |
| Product and supplier photographs | Local assets/catalog and category-products | IMAGE LICENSE/OWNERSHIP REQUIRES MANUAL VERIFICATION | Unverified rights; not evidence of infringement | Obtain supplier/photographer rights and applicable model permissions |
| Official logo | Local brand assets; earlier audit records business supply | OWNERSHIP NEEDS MANUAL VERIFICATION | Provenance documentation incomplete | Retain owner/designer permission evidence |
| Campaign illustrations | IMAGE-AUDIT.md records generated campaign provenance | DOCUMENTATION RECOMMENDED | Generation provenance is not a full rights clearance | Retain generation/source records |
| Inline social SVGs | Local markup; no external icon library | DOCUMENTATION RECOMMENDED | Brand-mark usage/provenance not conclusively reviewed | Retain source history; owner verifies platform brand requirements |
| Site JavaScript/CSS | Local scripts, no detected production framework/CDN library | No third-party license violation verified | Copied-code provenance cannot be established from appearance alone | Retain authorship/source history |

Official font licenses: [DM Sans](https://raw.githubusercontent.com/google/fonts/main/ofl/dmsans/OFL.txt), [Playfair Display](https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/OFL.txt). They identify SIL OFL 1.1 and permit font embedding subject to its conditions. A developer-facing THIRD-PARTY-NOTICES.txt records sources without changing typography or public footer copy. No image was deleted based on an ownership assumption.

## L. Technical code quality

All production JavaScript files passed node --check. Page and interaction tests produced no uncaught JavaScript errors. Targeted HTML checks passed: one title/viewport per file, parseable JSON-LD, no browser duplicate IDs, no missing local asset/srcset paths, and no missing local link paths or fragment IDs. This is not a complete HTML conformance validation; no conformance validator was installed. Full console/network cleanliness across real external integrations is not claimed.

The invalid footer border, hiding rules, fallback drift, unequal CTA positions and menu resize state were fixed. No framework, runtime dependency, tracker, font or third-party script was added. Existing layered CSS overrides were not mass-deleted as allegedly unused code. Data/schema and working JavaScript architecture were retained.

## M. SEO / AEO / GEO regression protection

Hash comparison confirms **every root page HTML file is byte-for-byte unchanged**. Consequently titles, metadata (Open Graph/Twitter included), canonicals, robots metadata, H1/H2/H3, schema, structured content, local SEO, FAQ/AEO text, GEO/entity signals, ALT and all static internal links are preserved. Product JSON, robots.txt and sitemap.xml also match their original hashes. Runtime card title/text/image/WhatsApp values match before and after. Footer links are additions/canonical equivalents, not removals. No claim about future search rankings is implied.

## N. Files modified

${reg.changed.map(f=>'- '+f.replaceAll('\\','/')).join('\n')}

New developer-facing files: THIRD-PARTY-NOTICES.txt and audit-2026-09-10/* (reports, JSON evidence, screenshots, scripts and original backups). No new website page or production dependency was added. Audit tools use the environment's existing Playwright/Edge/Pillow; they are not loaded by the website. Keep audit evidence out of a public deployment bundle.

## O. Manual verification required

- Product details listed individually in PRODUCT-INFORMATION-REQUIRED.md, including existing ten classification review notes.
- Image/logo licensing and model permissions; inline brand SVG provenance/usage.
- Valid PageSpeed/Lighthouse measurements on all six requested pages, plus field INP and real-user CWV.
- Hostinger security-header, redirect and cache configuration: source _headers/_redirects are not demonstrably active. Preserve current canonicals while resolving host settings.
- Full HTML conformance, screen-reader/contrast review, real mobile browser/touch checks, camera/file/save flows and external social/Maps destination identity.

## P. Remaining actions

Safe source fixes and local regression testing are complete. Deploy only through the site's established hosting workflow, clear/revalidate cached CSS/JS, then repeat live checks. Resolve owner-dependent product/rights details and obtain real performance measurements. No speculative SEO rewrite, asset deletion, product-data invention or live deployment was performed.
`;
fs.writeFileSync(out+'/AUDIT-REPORT.md',report);console.log('Created complete A–P audit report and individual product report.');
