import { isNewCategory, newCategoryName, usableDrafts, type Draft } from "./draft";
import type { CategoryOption } from "./fileNameParser";

const keyOf = (name: string) => name.trim().toLowerCase();

export interface CategoryOutcome {
  /** Real category id for every new category name that now exists, keyed by lower-cased name. */
  ids: Map<string, string>;
  /** Categories this call actually created. */
  created: CategoryOption[];
  failed: { name: string; error: unknown }[];
}

/**
 * Makes the categories the vendor chose to create ("Create 'Saree' and use it") real, once each, before any product
 * is created. Only names a photo that will really be created uses are made, so removing or skipping a photo never
 * leaves an empty category behind. A name that already exists (including from an earlier attempt that got partway)
 * is reused, not created again. One failure doesn't stop the others.
 */
export async function resolveNewCategories(
  drafts: Draft[],
  existing: CategoryOption[],
  create: (name: string) => Promise<CategoryOption>,
): Promise<CategoryOutcome> {
  const ids = new Map<string, string>();
  const created: CategoryOption[] = [];
  const failed: CategoryOutcome["failed"] = [];

  const wanted = new Map<string, string>();
  for (const draft of usableDrafts(drafts)) {
    if (isNewCategory(draft.categoryId)) {
      const name = newCategoryName(draft.categoryId);
      if (!wanted.has(keyOf(name))) wanted.set(keyOf(name), name);
    }
  }

  for (const [key, name] of wanted) {
    const found = existing.find((category) => keyOf(category.name) === key);
    if (found) {
      ids.set(key, found.id);
      continue;
    }
    try {
      const made = await create(name);
      ids.set(key, made.id);
      created.push(made);
    } catch (error) {
      failed.push({ name, error });
    }
  }
  return { ids, created, failed };
}

/**
 * The real category id to send for a draft: null for "no category", the id itself for an existing category, the
 * created id for a new one, and undefined when it was a new category that could not be created.
 */
export function categoryIdFor(draft: Draft, ids: Map<string, string>): string | null | undefined {
  if (draft.categoryId === "") return null;
  if (!isNewCategory(draft.categoryId)) return draft.categoryId;
  return ids.get(keyOf(newCategoryName(draft.categoryId)));
}
