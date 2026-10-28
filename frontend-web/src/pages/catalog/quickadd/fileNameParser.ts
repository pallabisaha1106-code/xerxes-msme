/**
 * Turns a dropped photo's file name into a draft product's name and category, for the "add products from
 * photos" flow. The convention is IMG_<Category>_<anything else>.jpg - e.g. "IMG_Kurti_Blue Cotton.jpg" becomes
 * the product "Kurti Blue Cotton" in the category "Kurti". Pure functions, no browser APIs, so they're unit-tested.
 */

export interface CategoryOption {
  id: string;
  name: string;
}

export interface ParsedFileName {
  /** null when the file name carries no usable name (a camera name like IMG_2231.jpg) - the caller picks a placeholder. */
  name: string | null;
  /** The vendor's existing category the name starts with, if any. */
  category: CategoryOption | null;
  /** A category the file name seems to name but the vendor doesn't have yet, offered as "Create ...". */
  missingCategory: string | null;
}

const MAX_NAME_LENGTH = 120;
// Phone and camera defaults (IMG_2231, DSC_0042, PXL_2026..., MVIMG_...); IMG_ doubles as our own convention's prefix.
const CAMERA_PREFIX = /^(img|dsc|dscn|pxl|mvimg)[\s_-]+/i;

/** The file name without its extension and prefix, split on underscores: "IMG_Kurti_Blue Cotton.jpg" -> ["Kurti", "Blue Cotton"]. */
function segmentsOf(fileName: string): string[] {
  if (/^whatsapp image/i.test(fileName)) return [];
  return fileName
    .replace(/\.[A-Za-z0-9]{2,5}$/, "")
    .replace(CAMERA_PREFIX, "")
    .split("_")
    .map((segment) => segment.replace(/\s+/g, " ").trim())
    .filter(Boolean);
}

/** Case, apostrophes, spaces, underscores and hyphens don't matter: "Men's Wear", "mens_wear" and "MENS-WEAR" are the same. */
function wordsOf(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/['’]/g, "")
    .split(/[\s_-]+/)
    .filter(Boolean);
}

/** A trailing "s" or "es" doesn't matter either: "kurtis" matches "kurti", "dresses" matches "dress". */
function sameWord(a: string, b: string): boolean {
  return a === b || a + "s" === b || b + "s" === a || a + "es" === b || b + "es" === a;
}

/** The category whose words the name starts with; when several do ("Ladies" and "Ladies Wear"), the longest wins. */
export function matchCategory(name: string, categories: CategoryOption[]): CategoryOption | null {
  const nameWords = wordsOf(name);
  let best: CategoryOption | null = null;
  let bestLength = 0;
  for (const category of categories) {
    const categoryWords = wordsOf(category.name);
    if (categoryWords.length === 0 || categoryWords.length > nameWords.length || categoryWords.length <= bestLength) continue;
    if (categoryWords.every((word, i) => sameWord(word, nameWords[i]))) {
      best = category;
      bestLength = categoryWords.length;
    }
  }
  return best;
}

export function parseFileName(fileName: string, categories: CategoryOption[]): ParsedFileName {
  const segments = segmentsOf(fileName);
  const name = segments.join(" ").slice(0, MAX_NAME_LENGTH).trim();
  // No letters at all (digits only, e.g. IMG_20260115_103045) means a camera name, not a product name.
  if (!/\p{L}/u.test(name)) return { name: null, category: null, missingCategory: null };

  const category = matchCategory(name, categories);
  // Only guess a missing category from the Category_Rest structure or a single word. A plain multi-word name
  // ("Blue Cotton Kurti") could put the category anywhere, so no guess is better than a wrong "Create" button.
  const canGuess = segments.length > 1 || wordsOf(segments[0]).length === 1;
  return { name, category, missingCategory: !category && canGuess ? segments[0] : null };
}
