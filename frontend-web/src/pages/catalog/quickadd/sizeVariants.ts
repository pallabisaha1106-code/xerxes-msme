import type { ProductRequest, VariantRequest } from "../../../api/productApi";

/** The one-click sizes on a draft card; "Other" lets the vendor type their own (Free size, 42, ...). */
export const SIZE_PRESETS = ["XS", "S", "M", "L", "XL", "XXL"] as const;

const MAX_SKU_BASE_LENGTH = 40;

const slug = (text: string) =>
  text
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

/**
 * KURTI-BLUE-COTTON for a product with no sizes, KURTI-BLUE-COTTON-M for its M variant. `attempt` is bumped by
 * the caller when the server says a SKU already exists (KURTI-BLUE-COTTON-2-M, -3-M, ...), the same idea as the
 * starter catalogue's unique-SKU suffix. ASCII only, like the starter SKUs; a name with no Latin letters gets ITEM.
 */
export function skuFor(name: string, size: string | null, attempt = 1): string {
  let base = slug(name).slice(0, MAX_SKU_BASE_LENGTH).replace(/-+$/, "") || "ITEM";
  if (attempt > 1) base += `-${attempt}`;
  const sizePart = size ? slug(size) : "";
  return sizePart ? `${base}-${sizePart}` : base;
}

/** What a draft card holds that the product needs. Price is the raw input text: blank means "not priced yet". */
export interface DraftProduct {
  name: string;
  categoryId: string | null;
  price: string;
  sizes: string[];
}

/**
 * The create-product request for one draft: one default variant when no sizes are picked, otherwise one variant
 * per size (attribute Size), all at the same price. Purchase price, GST and unit take the same safe defaults as the
 * starter catalogue, so a product is listable straight away and priced or stocked properly later.
 */
export function buildProductRequest(draft: DraftProduct, attempt = 1): ProductRequest {
  const parsed = Number(draft.price);
  const sellingPrice = Number.isFinite(parsed) && parsed > 0 ? Math.round(parsed * 100) / 100 : 0;
  const sizes = [...new Set(draft.sizes.map((size) => size.trim()).filter(Boolean))];
  const variant = (size: string | null): VariantRequest => ({
    sku: skuFor(draft.name, size, attempt),
    attributes: size ? { Size: size } : {},
    purchasePrice: 0,
    sellingPrice,
  });
  return {
    name: draft.name.trim(),
    categoryId: draft.categoryId,
    defaultGstRate: 0,
    defaultUom: "PCS",
    variants: sizes.length ? sizes.map(variant) : [variant(null)],
  };
}
