import type { Business } from "../types";

/** Ported from marketplace.js's productPageURL - only a simple local .html filename is ever accepted. */
export function productPageUrl(business: Business): string {
  const page = business.productPage;
  return page?.enabled === true && typeof page.url === "string" && /^[a-z0-9][a-z0-9-]*\.html$/.test(page.url)
    ? page.url
    : "";
}
