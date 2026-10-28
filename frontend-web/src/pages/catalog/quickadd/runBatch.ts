import { categoryIdFor, resolveNewCategories } from "./resolveCategories";
import { createProductWithPhoto, newProgress, type CardProgress, type CreateApi } from "./createOne";
import { finishProduct, type FinishApi } from "./finishProduct";
import { isNewCategory, newCategoryName, usableDrafts, type Draft } from "./draft";
import type { CategoryOption } from "./fileNameParser";

export type CardStatus = "waiting" | "working" | "done" | "failed";

export interface BatchApi extends CreateApi, FinishApi {
  listCategories: () => Promise<CategoryOption[]>;
  createCategory: (name: string) => Promise<CategoryOption>;
}

export interface BatchOptions {
  api: BatchApi;
  /** What has already been saved for each card; kept by the caller across runs so a retry carries on where a card stopped. */
  progress: Map<string, CardProgress>;
  onStatus: (draftId: string, status: CardStatus, message?: string) => void;
  /** Turns a thrown error into text for the vendor. */
  describe: (error: unknown) => string;
}

/** Products are created this many at a time: quick, without flooding the server or the shop's photo storage. */
export const CONCURRENCY = 3;

/**
 * Creates the products for these cards. New categories come first (once each), then the cards run a few at a time:
 * product and photo, then shared photo and stock. A card that fails is reported and the rest carry on. Run it again
 * with just the failed cards to retry them; their saved progress means nothing is created twice. Returns the
 * categories this run created, so the page can add them to its list.
 */
export async function runBatch(drafts: Draft[], options: BatchOptions): Promise<{ createdCategories: CategoryOption[] }> {
  const { api, progress, onStatus, describe } = options;
  const cards = usableDrafts(drafts);

  const ids = new Map<string, string>();
  let createdCategories: CategoryOption[] = [];
  let blocked: ((draft: Draft) => string | null) | null = null;

  if (cards.some((card) => isNewCategory(card.categoryId))) {
    try {
      const outcome = await resolveNewCategories(cards, await api.listCategories(), api.createCategory);
      outcome.ids.forEach((id, key) => ids.set(key, id));
      createdCategories = outcome.created;
      const failedNames = new Map(outcome.failed.map((f) => [f.name.toLowerCase(), describe(f.error)]));
      blocked = (draft) =>
        isNewCategory(draft.categoryId) && failedNames.has(newCategoryName(draft.categoryId).toLowerCase())
          ? `Couldn't create the "${newCategoryName(draft.categoryId)}" category: ${failedNames.get(newCategoryName(draft.categoryId).toLowerCase())}`
          : null;
    } catch (error) {
      blocked = (draft) => (isNewCategory(draft.categoryId) ? `Couldn't check your categories: ${describe(error)}` : null);
    }
  }

  const queue = [...cards];
  const worker = async () => {
    for (let card = queue.shift(); card; card = queue.shift()) {
      const reason = blocked?.(card) ?? null;
      if (reason) {
        onStatus(card.id, "failed", reason);
        continue;
      }
      onStatus(card.id, "working");
      try {
        const saved = progress.get(card.id) ?? newProgress();
        progress.set(card.id, saved);
        await createProductWithPhoto(card, categoryIdFor(card, ids) ?? null, saved, api);
        await finishProduct(card, saved, api);
        onStatus(card.id, "done");
      } catch (error) {
        onStatus(card.id, "failed", describe(error));
      }
    }
  };
  await Promise.all(Array.from({ length: Math.min(CONCURRENCY, cards.length) }, worker));

  return { createdCategories };
}
