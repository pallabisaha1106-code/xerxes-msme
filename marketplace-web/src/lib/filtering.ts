import type { Business } from "../types";

export interface Filters {
  search: string;
  area: string;
  pincode: string;
}

/**
 * Ported from marketplace.js's matches() - searches name/type/address/area/pincode and every
 * product name. No category check - the listings fetch is already server-filtered by category
 * (see MarketplacePage), so every business passed in here already matches whatever's selected.
 */
export function matchesFilters(business: Business, filters: Filters): boolean {
  const terms = filters.search.toLocaleLowerCase().trim().split(/\s+/).filter(Boolean);
  const productNames = [
    ...business.products,
    ...(business.productPage?.enabled ? business.productPage.products ?? [] : []),
  ].map((p) => p.name);
  const haystack = [business.businessName, business.businessType, business.address, business.area, business.pincode, ...productNames]
    .join(" ")
    .toLocaleLowerCase();

  return (
    (!filters.area || business.area === filters.area) &&
    (!filters.pincode || business.pincode === filters.pincode) &&
    terms.every((term) => haystack.includes(term))
  );
}

export function uniqueSorted(values: (string | undefined)[]): string[] {
  return [...new Set(values.filter((v): v is string => Boolean(v)))].sort();
}
