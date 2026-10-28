import axios from "axios";
import type { ProductRequest, ProductResponse } from "../../../api/productApi";
import type { Draft } from "./draft";
import { buildProductRequest } from "./sizeVariants";

/** What has already been saved for one card, so trying again after a failure carries on instead of starting over (and never creates a second product). */
export interface CardProgress {
  product: ProductResponse | null;
  photoUploaded: boolean;
  /** Variants that already received the shared photo / their opening stock (see finishProduct.ts). */
  copiedVariantIds: string[];
  stockedVariantIds: string[];
}

export const newProgress = (): CardProgress => ({
  product: null,
  photoUploaded: false,
  copiedVariantIds: [],
  stockedVariantIds: [],
});

/** The two server calls this step makes, passed in so they can be replaced in tests. */
export interface CreateApi {
  createProduct: (request: ProductRequest) => Promise<ProductResponse>;
  uploadVariantImage: (productId: string, variantId: string, file: File) => Promise<unknown>;
}

/** Cards in one batch can share a name (and so a SKU), and a SKU can exist already; each clash bumps the suffix. */
const MAX_SKU_ATTEMPTS = 10;

const isSkuClash = (error: unknown) => axios.isAxiosError(error) && error.response?.status === 409;

/**
 * Creates the product for one card (with a variant per size) and uploads its photo to the first variant. Steps
 * already recorded in `progress` are skipped, and each step is recorded as soon as it succeeds. The photo goes to
 * one variant here; copying it to the other sizes and the opening stock come after, in later steps.
 * categoryId is the real id to save (see categoryIdFor), not the draft's own "new:..." choice.
 */
export async function createProductWithPhoto(
  draft: Draft,
  categoryId: string | null,
  progress: CardProgress,
  api: CreateApi,
): Promise<ProductResponse> {
  if (!draft.file) throw new Error("This photo can't be used, so no product was created.");

  for (let attempt = 1; !progress.product; attempt++) {
    try {
      progress.product = await api.createProduct(buildProductRequest({ ...draft, categoryId }, attempt));
    } catch (error) {
      if (!isSkuClash(error) || attempt >= MAX_SKU_ATTEMPTS) throw error;
    }
  }

  if (!progress.photoUploaded) {
    await api.uploadVariantImage(progress.product.id, progress.product.variants[0].id, draft.file);
    progress.photoUploaded = true;
  }
  return progress.product;
}
