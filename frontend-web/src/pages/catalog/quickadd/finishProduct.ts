import type { VariantResponse } from "../../../api/productApi";
import type { AdjustmentRequest } from "../../../api/stockApi";
import type { Draft } from "./draft";
import type { CardProgress } from "./createOne";

/** The two server calls this step makes, passed in so they can be replaced in tests. */
export interface FinishApi {
  copyVariantImages: (productId: string, variantId: string, sourceVariantId: string) => Promise<unknown>;
  recordAdjustment: (variantId: string, request: AdjustmentRequest) => Promise<unknown>;
}

/** Stock typed in a box: whole pieces, and blank, negative or unreadable means none. */
export function openingStock(raw: string | undefined): number {
  const value = Math.floor(Number(raw));
  return Number.isFinite(value) && value > 0 ? value : 0;
}

/** Opening stock for one variant: its size's own box when the card has sizes, otherwise the card's single stock box. */
function stockFor(draft: Draft, variant: VariantResponse): number {
  if (draft.sizes.length === 0) return openingStock(draft.stock);
  const size = variant.attributes.Size;
  const key = Object.keys(draft.sizeStock).find((candidate) => candidate.trim() === size);
  return openingStock(key === undefined ? undefined : draft.sizeStock[key]);
}

/**
 * Second half of creating a card, after createProductWithPhoto: shares the photo with the other size variants, then
 * records each variant's opening stock (nothing is recorded for zero - the server refuses a zero movement, and a new
 * variant already starts at zero). Each variant is noted in `progress` as it is done, so a retry skips finished ones.
 */
export async function finishProduct(draft: Draft, progress: CardProgress, api: FinishApi): Promise<void> {
  if (!progress.product || !progress.photoUploaded) {
    throw new Error("The product and its photo must be saved before sizes and stock.");
  }
  const { id: productId, variants } = progress.product;
  const first = variants[0];

  for (const variant of variants.slice(1)) {
    if (progress.copiedVariantIds.includes(variant.id)) continue;
    await api.copyVariantImages(productId, variant.id, first.id);
    progress.copiedVariantIds.push(variant.id);
  }

  for (const variant of variants) {
    const quantity = stockFor(draft, variant);
    if (quantity === 0 || progress.stockedVariantIds.includes(variant.id)) continue;
    await api.recordAdjustment(variant.id, { signedDelta: quantity, remarks: "Opening stock" });
    progress.stockedVariantIds.push(variant.id);
  }
}
