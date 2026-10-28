# Parked suggestions

Ideas raised while working on the marketplace (cart drawer, card stepper, Theme A). Nothing here is started.
Parked on 2026-10-16. Each row is sized to fit the "small change" rule in `CLAUDE.md`; larger ones say how to split.

Priority: **H** = worth doing soon, **M** = nice to have, **L** = low value or only if it bites.

## 1. Marketplace look and feel (from the Zepto comparison)

| # | Suggestion | What was observed | Proposed change | Size | Priority |
|---|---|---|---|---|---|
| GAP-2 | Sticky cart bar on phones | The cart is only the small header icon; no running total or "View cart" button near the thumb | New bottom bar such as "2 items · ₹120 · View cart" that opens the drawer; shown only on checkout shops with items | ~20 lines (component + CSS) | H |
| GAP-3 | City visible and changeable on phones | `.header-location` is hidden below 1100px; the phone menu has no city option; the top strip reads "Pune, PUNE." (name repeated in two cases) | Show the city in the phone header or menu; fix the strip text | Small header change + 1-line text fix | H |
| GAP-4a | Hide the SKU line when it repeats the name | Cards show "AMUL-MILK · Ref AMUL-MILK" | Hide the "Ref" part when it equals the name or SKU | ~5 lines in `LiveShopPage` | M |
| GAP-4b | Price format | "₹60.00" | Drop ".00" for whole amounts (`formatRupees`) | 1-3 lines | L |
| GAP-4c | Bigger tap target on compact cards | Compact Add button and stepper are 28px tall (under the 44px guideline) | Raise the compact height to 44px; re-check card heights at 320 to 412px | ~6 lines CSS + re-measure | M |
| GAP-4d | "Sample image" pill on every card | Clutter over the photo | Lower priority: real vendor photos will replace the placeholders | - | L |
| GAP-5 | Too much page before the first product | Shop intro is about 500px on a phone; home shop cards are about 477px tall, one per row | Compact the shop intro on phones, category chips instead of a dropdown, denser home cards. Larger layout work: split into small steps first | Several steps | M |
| GAP-6 | Richer cart drawer rows | No thumbnails; no bill breakdown | Thumbnail in each row; clearer bill summary | ~10 lines per item | M |
| GAP-7 | Search scrolls away | Search sits below the banner and scrolls off; placeholder is cut off on phones | Sticky or more prominent search; shorter placeholder | Small | M |
| Theme B | Alternate colour direction | Chosen: Theme A. Theme B (deep green header, marigold buttons) kept as an option | If revisited: marigold top strip, keep the banner dark green, header buttons need restyling on dark | 6 steps | L |

## 2. Small fixes and cleanups

| # | Item | Where | Notes | Size | Priority |
|---|---|---|---|---|---|
| F1 | Cart summary says "5 items across 1 products" | Summary string built in `marketplace-web/src/pages/LiveShopPage.tsx` (passed to `CartFooter`) | Copied unchanged from the old inline cart. Pluralise "product" | 1 line | M |
| F2 | Result counter flashes "0 shops to discover" while loading | `MarketplacePage.tsx` (`resultCount`) | Hide or replace the text while `loadingListings` | ~3 lines | L |
| F3 | Dead CSS | `style.css` | 7 unused selectors: `.ad-bottom`, `.ad-small`, `.ad-panel a`, `.art-label small`, `.hero-notes`, `.pune-pill`, `.rail-note`; plus the `.demo-note` rules | Pure deletion | L |
| F4 | Does the shop page have a loading skeleton? | `LiveShopPage.tsx` | Not checked. Only the home shop grid is known to have one | Check first | L |
| F5 | Out-of-stock stepper path | `LiveShopPage.tsx` | The Add button keeps `disabled={!variant.inStock}`, but no out-of-stock product exists in the test data, so it was never exercised in the browser | Needs test data | L |
| F6 | `todo.md` is out of date | `todo.md` | No entry for pagination, cart drawer, card stepper or Theme A | Docs only | M |

## 3. Tests and flakiness

| # | Item | Where | Notes | Size | Priority |
|---|---|---|---|---|---|
| T1 | Admin listings stale-response race (hit again in the 2026-10-16 full run: ladies-wear checkout spec, passes alone) | `frontend-web/src/pages/admin/AdminMarketplaceListingsPage.tsx` (`loadListings`) | Root cause of the intermittent "Approve → LIVE" failure: an older fetch can finish last and overwrite the approved row. A longer timeout does not help (15s was tried). Fix: a request counter so only the latest request may call `setListings` / `setLoading`. Memory note: `admin-listings-stale-response-race` | ~8 lines | M (do it if the flake returns) |
| T2 | Same pattern on Admin Feed Posts | `frontend-web/src/pages/admin/AdminFeedPostsPage.tsx` | Confirmed on 2026-10-16: the feed-posts spec failed at "Approve -> APPROVED" in a full run (row never showed the new status) and passed alone. Same fix as T1 (request counter). The admin list also looks up each post's tenant one by one (`AdminFeedPostService.toResponse`), which slows the response and widens the race | ~8 lines + optional batching | H now |
| T3 | Six other specs repeat the "Approve → LIVE" check | `grocery-vendor-free-checkout`, `ladies-wear-vendor-free-checkout`, `grocery-vendor-category-filter`, `grocery-vendor-free-whatsapp`, `ladies-wear-vendor-free-whatsapp`, `vendor-registration` (first test) | Fixing T1 in the app covers all of them, which beats patching each spec | - | - |

## 3b. Feed revamp follow-ups (Phase 1 is done; see the Feed section of the project notes)

| # | Item | Where | Notes | Size | Priority |
|---|---|---|---|---|---|
| FD1 | Phase 2 (discount label + end date): DONE | - | - | Built and verified end to end (steps 1-7); kept here only so the numbering of the rows below stays stable | - | - |
| FD2 | Phase 3: posting limit and moderation | `FeedPostService` (1 post per 7 days), admin approval queue | Needs two decisions first: a higher posting limit (for example 1 per day, or 3 per week), and a lighter moderation model (pre-approve a shop's first post then auto-publish with a report/takedown, or review only reported posts) | Policy, then small changes | H |
| FD3 | Phase 4: reasons to return | `FeedPage`, home page | Save offer and follow shop via local storage (no shopper accounts), share to WhatsApp, an "Offers" strip near the top of the home page, view and click counts for vendors | Mixed | M |
| FD4 | Subscription check is one query per live shop | `PublicMarketplaceService.listPublicFeed` (`hasSubscriptionAccess`) | The tenants are already loaded in the same method, so status can be checked in memory. A city with thousands of shops still costs thousands of small queries per request | ~3 lines + 1 test | M |
| FD5 | `shopPhotoUrl` is unused | `PublicFeedPostResponse`, `listPublicFeed`, `PublicFeedPostDto` | The new card shows no photo, so the field and its media URL work can go | Small | L |
| FD6 | Dismissing the city picker forgets the previous city | `DiscoveryWelcomeDialog` | Same as the header's picker. A prop so a dismissed reopen keeps the old city would avoid it | Small | L |
| FD7 | Category label is one mint style | `FeedPage` card, `style.css` | Could tint per category like the home pills (needs a `data-category` attribute on the label) | ~6 lines | L |

## 4. Accessibility notes from the Theme A contrast sweep

| # | Item | Measured | Notes | Priority |
|---|---|---|---|---|
| A1 | Grey text on pale tints (already failing before Theme A) | about 4.31:1 (needs 4.5) | `.plan-picker-description` on the apply page; the onboarding paragraphs on the home page. Slightly darkening `--muted` or those tints would fix it | L |
| A2 | Selected states, only just passing | about 4.53:1 | `.category-tile.has-selection`, `.product-tile.selected`, `.plan-picker-card.selected` use brand green on a pale tint. Could use the deeper `#14663a` for extra margin | L |

## Already done (for context, not parked)

Cart drawer, header cart icon and badge bump, card stepper with focus hand-off, spec updates, `catalog-apparel` exact selector fix, Theme A (fresh and warm; revert instructions are in the comment at the end of `style.css`).
