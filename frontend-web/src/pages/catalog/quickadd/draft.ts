/** One dropped photo on its way to becoming a product: everything the review card shows and the confirm step needs. */
export interface Draft {
  id: string;
  fileName: string;
  /** Object URL of the photo shown on the card (the cropped version when it was cropped). Revoked when the draft goes away. */
  previewUrl: string;
  /** The upright, rule-conforming JPEG to upload. null when the photo can't be used (see problem). */
  file: File | null;
  /** Why the photo can't be used ("Too small (400 x 500)"); a draft with a problem is left out of the count and never created. */
  problem: string | null;
  cropped: boolean;
  name: string;
  /** "" = no category; "new:<Name>" = a category that doesn't exist yet and is created when the vendor confirms. */
  categoryId: string;
  /** Where the current category came from, for the note on the card: found in the file name, guessed-but-missing, none, or just created. */
  categoryNote: "file" | "missing" | "none" | "created" | null;
  /** The category the file name seemed to name but the vendor doesn't have, offered as "Create ...". */
  missingCategory: string | null;
  /** Raw input text; blank means "not priced yet". */
  price: string;
  /** Opening stock when no sizes are picked. */
  stock: string;
  sizes: string[];
  /** Opening stock per size, keyed by size; blank or missing means 0. */
  sizeStock: Record<string, string>;
}

export const NEW_CATEGORY_PREFIX = "new:";

export const isNewCategory = (categoryId: string) => categoryId.startsWith(NEW_CATEGORY_PREFIX);
export const newCategoryName = (categoryId: string) => categoryId.slice(NEW_CATEGORY_PREFIX.length);

/** The drafts that will become products. */
export const usableDrafts = (drafts: Draft[]) => drafts.filter((draft) => draft.file !== null);

/** The fields "same for all photos" can set; a missing field means leave that field alone. */
export interface ApplyValues {
  categoryId?: string;
  price?: string;
  stock?: string;
  sizes?: string[];
}

/** What "apply to all" does to one draft: only the filled fields change; a stock figure goes to each size when the draft has sizes. */
export function applyValues(draft: Draft, values: ApplyValues): Draft {
  if (draft.file === null) return draft;
  const next: Draft = { ...draft };
  if (values.categoryId !== undefined) {
    next.categoryId = values.categoryId;
    next.categoryNote = null;
  }
  if (values.price !== undefined) next.price = values.price;
  if (values.sizes !== undefined) next.sizes = values.sizes;
  if (values.stock !== undefined) {
    next.stock = values.stock;
    next.sizeStock = Object.fromEntries(next.sizes.map((size) => [size, values.stock as string]));
  }
  return next;
}
