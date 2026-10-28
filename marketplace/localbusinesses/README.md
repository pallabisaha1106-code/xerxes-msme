# Neighbourly

A responsive, single-page local shop directory using only HTML, CSS and vanilla JavaScript. There are no runtime packages, accounts, databases, analytics, carts or payments.

## Open and host

Open `dist/index.html` in a modern browser, or serve the `dist` directory with any static web host. All application assets are local and work without a network connection. WhatsApp and Google Maps links require internet access. The optional Sites manifest does not affect portability.

## Update the catalogue

Edit `dist/data/businesses.js`. Copy one business object, give it a unique `id`, and update its fields. Add or remove objects to update the catalogue. Edit `categories` in the same file; businessType must match a category name exactly.

- `businessName`, `businessType`: display name and category.
- `shopPhoto`: relative image path. Blank or unavailable images use a local placeholder.
- Shop contact buttons are controlled exclusively by the links object described below.
- `address`, `area`, `pincode`: text fields, with pincodes stored as strings.
- `products`: an array of `{ name, image }` objects. The card previews the first three; each opens a keyboard-accessible dialog.
- `config.adminWhatsapp`: marketplace administrator's actual WhatsApp number. This enables the retailer submission button with a pre-filled introduction. Submissions and review happen manually on WhatsApp.

The catalogue uses sample business details and illustrations. Veera Collection has the six supplied live contact URLs; other sample map links search their neighbourhoods. Replace remaining sample details before a public launch. The page labels the sample catalogue.

## Shop contact links

Each business has a links object with these optional keys: instagram, facebook, youtube, googleMaps, whatsapp, and website. Paste the full raw HTTP/HTTPS URL, not a Markdown link. Veera Collection demonstrates all six.

- Add or change a URL to show or update that platform’s icon.
- Set its value to an empty string, or remove the key, to hide that icon.
- If every link is missing, empty or invalid, the whole contact row is omitted.
- WhatsApp accepts full chat and group-invitation URLs. For a direct number, use https://wa.me/ followed by the real international number, with digits only.
- Keep URL query parameters intact. Unsafe protocols, relative paths, malformed URLs and URLs containing whitespace or credentials are rejected.
- Links open in a new tab. Icons have accessible labels, hover tooltips and compact 30 × 30 px buttons with 16 px icons. All six stay on one row with 5 px gaps; buttons can shrink slightly on narrow cards.
- The separate config.adminWhatsapp field still accepts international digits and controls the marketplace onboarding CTA exactly as before.

Business onboarding requests optional social, WhatsApp and website URLs alongside the existing required details. No new registration form is introduced.
## Images and ads

All supplied illustrations are original SVG placeholders created for this project, with no third-party asset dependencies. Replace them with retailer-supplied photographs that you have permission to use. Use optimized WebP/JPEG images, around 800 × 500 for shop covers and 300 × 300 for products. Keep each file under about 100 KB where practical.

Replace the contents of `.left-ad` and `.right-ad` in `dist/index.html` to add advertising images or HTML. Retain the Advertisement labels. Ad rails have 160px desktop columns and are hidden below 951px. No ad scripts are included.

## Files

- `dist/index.html`: page, onboarding copy, SEO metadata and advertisement slots.
- `dist/css/style.css`: responsive styling, focus states and reduced motion handling.
- `dist/js/marketplace.js`: combined keyword/category/area/pincode filters, contacts, mobile navigation and lightbox.
- `dist/data/businesses.js`: administrator-maintained catalogue and contact configuration.
- `dist/images/`: local original illustrations and fallback image.
- `verify.cjs`: dependency-free verification of data, assets and marketplace behaviours.
- `generate-assets.cjs`: development-only sample asset generator. Do not run after customising the catalogue: it regenerates the sample data and illustrations.

## Verification

Run `node verify.cjs` to check local references, catalogue integrity, search fields, combined filters, empty/reset results, contact URL construction, product dialogs and mobile navigation. Run `node --check dist/js/marketplace.js` for syntax validation. Layout uses one column on mobile, two on tablet/laptop and three on large screens, with side rails on desktop. Native dialogs support Escape and focus return. Browser visual checks are separate from these automated checks.

## Licensing

The original code and SVG illustrations are provided under the MIT license in LICENSE. No paid APIs, components, fonts, frameworks or templates are needed. WhatsApp and Google Maps are external link destinations only.

Veera Collection uses the supplied shop and product PNG photographs in dist/images/veera-*.png. Keep these files when running the sample generator; its Veera listing preserves their paths and product names. The product lightbox opens the full supplied image.

Both advertisement rails now display dist/images/ads/veera-collection.png, generated from the supplied Veera photos. Each entire banner links to https://www.veeracollections.com/ in a new tab. Update both shop-ad anchors in dist/index.html to change the destination; the existing mobile rules still hide the side rails.

## Top advertisement carousel

Edit dist/data/advertisements.js to add, remove or reorder advertisements. Each slide has image, alt, shopName, headline, description and url fields. Store images inside dist/images and use their relative paths. The complete slide opens its HTTP/HTTPS website URL in a new tab. Images are contained without cropping, so both portrait photos and horizontal artwork work.

The carousel rotates automatically every 3 seconds with a gentle fade and no arrows, slide selectors or Pause/Play buttons. Change intervalMs to adjust timing (minimum 3 seconds). Rotation pauses only while keyboard-focused or in a hidden tab. Mouse hover does not pause it; reduced-motion users keep the three-second rotation without the fade animation. Empty slide arrays hide the area; a single slide remains static. Both existing side-rail advertisements remain in place.

Run node verify-ads.cjs to check carousel behaviour and section order. The description, search and trust points now form the responsive full-width strip immediately above the category section.

## Explore Wholesaler

Open dist/wholesalers.html or choose Explore Wholesaler in the header. Explore Shops returns to dist/index.html. Both pages share the marketplace JavaScript, styles, photo previews, contact icons and advertisement carousel; each loads its own catalogue.

Edit dist/data/wholesalers.js to maintain wholesale categories and businesses. Copy a business object, assign a unique id, then replace its name, category, address, area, pincode, photo, products and optional links. Eight fictional samples are included to demonstrate the page. No real shop has been relabelled as a wholesaler, and sample suppliers have no contact buttons until real URLs are added. The retail sample generator does not overwrite this catalogue.

Set config.adminWhatsapp in the wholesaler file to your administrator's international phone number (digits only). This enables List Your Wholesale Business on WhatsApp with a wholesale-specific message requesting the business details, product range, minimum order information and photos. Review submissions manually and add approved businesses to this file. This keeps the existing static, no-login approach; it does not create a self-service registration system. The shop page's administrator number remains in dist/data/businesses.js.

Run node verify-wholesalers.cjs for wholesale search, filters, previews, submission messaging and both pages' navigation/asset references.

The ₹9,999 business-online package is the first top-carousel slide on both pages. Its imageOnly flag displays the finished banner without duplicate text. Its URL is blank because no enquiry destination was supplied; add a full HTTP(S) URL in dist/data/advertisements.js to make it clickable.

Computer Town is listed under Computer/Laptop shop in Thergaon (411033), using the supplied photos and four URLs. Its Instagram URL is the general Instagram homepage as supplied; replace it with the business profile when available. Its WhatsApp link opens the supplied channel. The sample generator preserves this listing and expects the copied JPEG files in dist/images.

The right-side advertisement on both marketplace pages uses dist/images/ads/computer-town.png and links to https://www.computertownstore.com/. The left-side advertisement remains Veera Collection.

## Optional shop product pages

Veera Collection now has dist/veera-collection.html with its three existing previews and nine new Indo-western photos. Its marketplace card alone shows View all products. Original images remain untouched, and the copied images are lazy-loaded from dist/images/veera-products. Product names describe the supplied photos; reference codes identify enquiries, without implying prices, stock or material specifications.

The opt-in lives in that shop's dist/data/businesses.js object:

    productPage: {
      enabled: true,
      url: "veera-collection.html",
      products: [{ id: "VC-IW-01", name: "Purple floral set", category: "Indo-western", image: "images/veera-products/indo-western-01.png" }]
    }

Existing card previews stay in products; extra catalogue images go in productPage.products. Only enabled: true with a simple local .html filename creates the card link. Missing or disabled configuration produces no link. The catalogue checks the page's fixed shop ID and URL before displaying products; changing query parameters cannot select another shop.

To offer this add-on to another shop, copy dist/veera-collection.html to a new shop filename, update data-product-shop on its body to the exact shop ID, update the title/meta descriptions and shop name in the preview help text, then add that shop's productPage configuration with the matching filename. The shared shop-products.js fills its name, address, existing contacts and products from the catalogue. No other shop is enabled automatically. The wholesale page stays unchanged.

To remove the add-on, set enabled to false and remove its .html file when publishing. The page also shows Product page unavailable if visited while disabled. This is a manually managed static feature, not paid-account authentication; product files themselves are public website assets.

The retail sample generator preserves Veera's productPage configuration. As before, do not rerun it after manually editing other catalogue details unless you also update its samples. Run node verify-product-pages.cjs for opt-in visibility, direct-page gating, images and preview behaviour.

Product enquiries: set enquiryWhatsapp on each enabled shop to its direct international WhatsApp number, e.g. digits without spaces or +. Veera uses 919921959748. Every product card opens that shop’s wa.me link with the product name and displayed Ref prefilled; the customer presses Send in WhatsApp. Missing or invalid numbers hide enquiry buttons. This field is independent of social/group/channel links, so Veera’s existing WhatsApp group button stays intact.

## First-visit discovery popup

Explore Shops and Explore Wholesaler each show an area/category popup on the first visit in a browser. Areas come from their respective catalogues; category choices are limited to businesses in the selected area. Applying the selection filters businesses and their product previews. All areas, All categories and Browse all businesses remain available.

Preferences are stored locally under neighbourly.discovery.v1.shops or neighbourly.discovery.v1.wholesalers. Later area/category changes and Reset update those preferences. No login, geolocation or server storage is used. Clearing browser site data makes the popup appear again; if storage is blocked, browsing still works but the popup may reappear. Product detail pages do not show this popup.

Run node verify-welcome.cjs for first-visit behaviour, filtering, storage and reset checks.

## Mateshwari Super Mart

Listed in Explore Shops under Grocery, at Datta Mandir Road, Thergaon, Pune 411033. Its opt-in page is `dist/mateshwari-super-mart.html`. The first three of 50 products live in products; the remaining 47 live in productPage.products. References run from MSM-001 through MSM-050. Names, pack sizes and MRPs follow the supplied grocery sheet.

Images are in `dist/images/mateshwari-super-mart/`. Front_photo.jpeg is a copy of the supplied original, which remains untouched. The product-01.jpg through product-50.jpg images are extracted from the supplied grocery sheet. The Google Maps URL retains its g_st=ac query parameter. The direct enquiry number 919545159444 is independent of the blank WhatsApp social link.

The generator includes this listing and expects its maintained image folder to remain present. Update both the listing and generator when changing its details. Run the existing verification scripts, including verify-product-pages.cjs. Veera remains enabled; other shops remain unchanged. Use ADD_SHOP_PROMPT.md for future shop additions.

## Grocery enquiry cart

Expanded product pages for businessType Grocery or Grocery Shop now include Add to cart on every product. The cart is isolated to the current shop/page and kept in memory: reloading clears it. Customers can adjust whole-number quantities from 1 to 999, remove products, or clear the cart. Quantities refer to the listed product/pack; no weights or pack sizes are invented.

Send cart on WhatsApp opens the shop's enquiryWhatsapp number with all selected product names, references and quantities. The customer reviews and sends it in WhatsApp; the shop confirms pricing and availability. Empty carts and missing/invalid recipient numbers cannot send. This is an enquiry list, with no checkout or payment. Non-grocery product pages retain individual enquiry buttons and do not show a cart.

### Mateshwari priced catalogue
Mateshwari now has 50 products from the supplied grocery sheet, replacing the former 15 placeholders. The first three are marketplace previews; the remaining 47 are in productPage.products. Each product uses id, name, packSize, numeric mrp (INR per pack), and image. Update the matching shop in generate-assets.cjs when changing the catalogue. Quantity means number of listed packs. Grocery carts display unit MRP, line totals and grand total; the same amounts are included in the prepared WhatsApp message. Missing/invalid prices show Price on enquiry and leave the grand total awaiting prices. The customer reviews and sends the message in WhatsApp.
