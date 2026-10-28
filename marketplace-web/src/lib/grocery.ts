import type { Business, Product } from "../types";
import { normalizeEnquiryNumber } from "./urlSafety";

export function isGroceryShop(shop: Pick<Business, "businessType">): boolean {
  return ["grocery", "grocery shop"].includes(String(shop.businessType || "").trim().toLowerCase());
}

/** null means "no valid price" -> UI shows "Price on enquiry" instead of a total. */
export function productPricePaise(product: Product): number | null {
  return typeof product.mrp === "number" && Number.isFinite(product.mrp) && product.mrp >= 0 && Number.isSafeInteger(Math.round(product.mrp * 100))
    ? Math.round(product.mrp * 100)
    : null;
}

export function formatPrice(paise: number): string {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(paise / 100);
}

export function productPack(product: Product): string {
  return product.name + (product.packSize ? " (" + product.packSize + ")" : "");
}

export interface CartEntry {
  index: number;
  quantity: number;
  product: Product;
}

export function cartTotal(entries: CartEntry[]): number | null {
  if (entries.some((e) => productPricePaise(e.product) === null)) return null;
  return entries.reduce((sum, e) => sum + productPricePaise(e.product)! * e.quantity, 0);
}

export function cartWhatsappUrl(shop: Business, entries: CartEntry[]): string {
  const number = normalizeEnquiryNumber(shop.enquiryWhatsapp);
  if (!isGroceryShop(shop) || entries.length === 0 || !/^[1-9]\d{7,14}$/.test(number)) return "";
  const lines = entries.map(({ index, quantity, product }, i) => {
    const price = productPricePaise(product);
    return (
      i + 1 + ". " + productPack(product) + " | Ref: " + (product.id || String(index + 1)) + " | Quantity: " + quantity +
      (price === null ? " | Price on enquiry" : " | Unit MRP: " + formatPrice(price) + " | Total: " + formatPrice(price * quantity))
    );
  });
  const total = cartTotal(entries);
  const message =
    "Hello " + shop.businessName + ", I would like to enquire about these products from Neighbourly:\n\n" +
    lines.join("\n") +
    "\n\nGrand total: " + (total === null ? "Awaiting prices" : formatPrice(total)) +
    "\n\nPlease confirm prices and availability. Thank you.";
  return "https://wa.me/" + number + "?text=" + encodeURIComponent(message);
}

export function productEnquiryUrl(shop: Business, product: Product, reference: string): string {
  const number = normalizeEnquiryNumber(shop.enquiryWhatsapp);
  if (!/^[1-9]\d{7,14}$/.test(number)) return "";
  const message =
    "Hello " + shop.businessName + ", I found this product on Neighbourly.\nProduct: " + product.name + "\nRef: " + reference +
    "\nPlease share the price, available sizes and availability.";
  return "https://wa.me/" + number + "?text=" + encodeURIComponent(message);
}
