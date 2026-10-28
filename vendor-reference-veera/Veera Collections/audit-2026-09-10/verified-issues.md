# Verified issues before modifications — 10 September 2026

Audit completed before edits: live crawl, all local pages at nine widths, catalogue inventory, image path checks, modal/source review, reduced-motion review, shared-component parity, font-license lookup, targeted HTML and JavaScript review.

| Priority | Classification | Finding | Safe action |
|---|---|---|---|
| Medium | FIX REQUIRED | Footer lacks textual Home, Anarkali, Punjabi dress, Co-Ord sets, and More navigation destinations. | Add these to existing Explore column; retain all existing destinations; use each page's existing HTTPS/www canonical URL. |
| Medium | FIX REQUIRED | Product lightbox prepares images by hiding descriptions and category/colour metadata. More than 1,000 detail elements are hidden across repeated page occurrences. | Keep card details visible; retain popup, zoom, and all original text/data. |
| Medium | FIX REQUIRED | Catalog CSS hides paragraph content below 500px, including legacy card availability wording. | Remove paragraph-hiding declaration only. |
| Low | FIX REQUIRED | Copyright border uses invalid `var(--color-white)fff20`. | Use intended translucent white border. |
| Low | NEEDS IMPROVEMENT | Shared header/footer fallback copies differ from current fetched components (home URL/markup). | Synchronize fallback markup with shared components. |
| Manual verification | PRODUCT INFORMATION REQUIRED | All 192 current records lack verified commercial facts; 10 have existing classification review notes. | Report individual references; invent no data. |
| Manual verification | DOCUMENTATION RECOMMENDED | Product imagery and logo ownership evidence is not included; generated campaign provenance is recorded in IMAGE-AUDIT.md. | Retain images; request owner evidence in report. |
| Manual verification | NEEDS IMPROVEMENT | Live Hostinger responses do not implement the `_headers` and `_redirects` configuration in this source folder; current canonicals still resolve with HTTP 200. | Report hosting configuration mismatch; do not change established canonicals or assume Cloudflare deployment. |
| Optional | GOOD | Responsive WebP variants, lazy catalogue images, dimensions, descriptive ALT, eager hero, reduced-motion rules, native photo dialog. | Preserve. |
| Optional | ALREADY CORRECT | All 29 local pages: no horizontal overflow at 320/375/390/414/768/1024/1366/1440/1920; no duplicate IDs or unnamed links/buttons in browser DOM. | Preserve layout. |

No Lighthouse package or full HTML conformance validator is installed. Live PerformanceObserver samples are unthrottled lab snapshots, not field CWV or Lighthouse scores. INP needs field measurement.

Additional verified interaction issue (Medium): opening the mobile menu then resizing above 1050px left body.menu-open set and disabled page scrolling. Fixed by closing the menu when entering the existing desktop breakpoint.

Additional verified card-layout issue (Low): first-row Kurtis action bottoms differed by 99.25px despite equal card bottoms. Use a flex column and auto top margin for the existing CTA, preserving all text and image presentation.
