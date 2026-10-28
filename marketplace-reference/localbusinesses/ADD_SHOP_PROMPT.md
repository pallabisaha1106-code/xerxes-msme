Add a new shop to the existing Neighbourly marketplace, with an optional separate product catalogue page.

IMPORTANT: Ask me for the details below before editing any website files. Present the questions as one simple form that I can complete. Wait for my response. If required information is missing or unclear, ask only for those missing details. Do not invent business information, product specifications, prices, contact numbers or URLs.

STEP 1 — COLLECT DETAILS

SHOP INFORMATION

- Shop name:
- Business type/category:
- Complete address:
- Area:
- City:
- Pincode:
- Shop-front photo:

OPTIONAL CONTACT LINKS
Leave unavailable links blank.

- Instagram:
- Facebook:
- YouTube:
- Google Maps:
- WhatsApp group/channel/chat link:
- Business website:

SEPARATE PRODUCT PAGE

- Enable a separate product page for this shop? Yes / No
- Direct WhatsApp number for product enquiries, including country code:
  This must be a phone number, not a group or channel link.
- Which three products should appear as previews on the main marketplace card?

PRODUCT INFORMATION
Ask me to provide this for each product:

- Product name:
- Product photo or photos:
- Category:
- Reference number:
- Description, price, sizes or other details, if I want them displayed:

If I supply photos without product names or references, ask whether you may create simple descriptive names and unique reference codes. Do not assume permission.

STEP 2 — IMPLEMENT AFTER I PROVIDE THE DETAILS

Inspect the existing project and follow its current structure and conventions.

- Add the business to Explore Shops, not Explore Wholesaler.
- Store its information in dist/data/businesses.js.
- Reuse a suitable existing category, or add the supplied category.
- Copy supplied images into an organised shop-specific folder inside dist/images.
- Preserve the original image files.
- Show the shop photo, address, area, pincode, three chosen product previews and available contact icons.
- Preserve exact supplied URLs and query parameters.
- Hide contact icons when their URLs are missing or invalid.
- If a social URL points only to a platform homepage, ask whether to use it as supplied or wait for the shop’s profile URL.

IF THE SEPARATE PRODUCT PAGE IS ENABLED

- Follow the existing Veera Collection product-page implementation.
- Create a unique local page for this shop with appropriate title and metadata.
- Enable its productPage configuration.
- Add “View all products” only to this shop’s marketplace card.
- Display all supplied products with their names, reference numbers and approved details.
- Avoid duplicating products already included as the three marketplace previews.
- Let customers open larger image previews.
- Add a “WhatsApp enquiry” button to each product when a valid direct enquiry number is supplied.
- Store the direct number in this shop’s enquiryWhatsapp field, independently of its WhatsApp social/group/channel link.
- Each enquiry must open WhatsApp to this shop’s number with this message prefilled:

Hello [SHOP NAME], I found this product on Neighbourly.
Product: [PRODUCT NAME]
Ref: [REFERENCE NUMBER]
Please share the price, available sizes and availability.

- The customer must press Send in WhatsApp.
- If no direct number is provided, leave enquiry buttons hidden and explain this in the completion message.
- Include the shop’s contact links, address and a link back to Explore Shops.

IF THE SEPARATE PRODUCT PAGE IS NOT ENABLED

- Add only the standard marketplace listing.
- Do not create a separate page or show “View all products.”

PRESERVE THE EXISTING WEBSITE

- Reuse the existing HTML, CSS and vanilla JavaScript.
- Keep other shops, wholesalers, advertisements, branding and navigation working.
- Do not add accounts, registration forms, payments, carts or backend services.
- Do not enable product pages for other shops.
- Do not create new advertisements unless I request them.
- Keep the sample-data generator consistent so it does not erase the new listing.
- Update maintenance instructions where necessary.
- Do not publish or deploy unless I explicitly request it.

STEP 3 — VERIFY AND REPORT

Check:

- The new shop appears in Explore Shops.
- Search works for its name, category, products, area and pincode.
- All supplied images load.
- Contact icons use the correct destinations.
- The separate page and its link appear only when enabled.
- Product enquiry links contain the correct shop number, product name and reference.
- The page remains responsive.
- Existing marketplace, wholesaler, advertisement and product-page checks pass.

Finish with a concise summary, a link to the new local page if created, and any details still needed.
