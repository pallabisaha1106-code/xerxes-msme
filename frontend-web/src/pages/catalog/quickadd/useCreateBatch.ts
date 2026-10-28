import { useRef, useState } from "react";
import { extractErrorMessage } from "../../../api/client";
import { createCategory, listCategories } from "../../../api/categoryApi";
import { createProduct, uploadVariantImage } from "../../../api/productApi";
import { copyVariantImages } from "../../../api/quickAddApi";
import { recordAdjustment } from "../../../api/stockApi";
import type { CardProgress } from "./createOne";
import { usableDrafts, type Draft } from "./draft";
import type { CategoryOption } from "./fileNameParser";
import { runBatch, type BatchApi, type CardStatus } from "./runBatch";

export interface CardState {
  status: CardStatus;
  message?: string;
}

const batchApi: BatchApi = {
  listCategories: async () => (await listCategories()).map(({ id, name }) => ({ id, name })),
  createCategory: async (name) => {
    const { id } = await createCategory({ name });
    return { id, name };
  },
  createProduct,
  uploadVariantImage,
  copyVariantImages,
  recordAdjustment,
};

interface Hooks {
  /** A card was fully created; the page drops it from the list. */
  onCardDone: (draftId: string) => void;
  onCategoriesCreated: (created: CategoryOption[]) => void;
  /** The run ended, whatever the outcome. */
  onSettled: () => void;
}

/**
 * Runs "Create N products" for the page: tracks each card's state (waiting, creating, failed), how many products
 * have been created so far, and the saved progress that lets a retry carry on instead of creating anything twice.
 */
export function useCreateBatch(hooks: Hooks) {
  const [states, setStates] = useState<Record<string, CardState>>({});
  const [creating, setCreating] = useState(false);
  const [createdCount, setCreatedCount] = useState(0);
  const progress = useRef(new Map<string, CardProgress>());
  const latest = useRef(hooks);
  latest.current = hooks;

  const start = async (cards: Draft[]) => {
    setCreating(true);
    setStates((current) => ({
      ...current,
      ...Object.fromEntries(usableDrafts(cards).map((card) => [card.id, { status: "waiting" } as CardState])),
    }));
    try {
      const { createdCategories } = await runBatch(cards, {
        api: batchApi,
        progress: progress.current,
        describe: extractErrorMessage,
        onStatus: (id, status, message) => {
          setStates((current) => ({ ...current, [id]: { status, message } }));
          if (status === "done") {
            setCreatedCount((count) => count + 1);
            latest.current.onCardDone(id);
          }
        },
      });
      latest.current.onCategoriesCreated(createdCategories);
    } finally {
      setCreating(false);
      latest.current.onSettled();
    }
  };

  /** Whether a card already has its product saved (a retry of it needs no new product slot). */
  const hasProduct = (draftId: string) => progress.current.get(draftId)?.product != null;

  return { states, creating, createdCount, start, hasProduct, resetCount: () => setCreatedCount(0) };
}
