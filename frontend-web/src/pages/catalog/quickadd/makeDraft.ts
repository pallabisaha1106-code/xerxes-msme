import { parseFileName, type CategoryOption } from "./fileNameParser";
import { prepareImage } from "./imageFile";
import type { FitRules } from "./imageFit";
import type { Draft } from "./draft";

/**
 * Turns one dropped file into a draft: reads the name and category from the file name, then fits the photo to the
 * shop's image rules. An unnamed photo (a camera name) gets an empty name here; the page numbers those "Product 1",
 * "Product 2", ... so the numbering continues across several drops.
 */
export async function makeDraft(file: File, rules: FitRules, categories: CategoryOption[]): Promise<Draft> {
  const parsed = parseFileName(file.name, categories);
  const prepared = await prepareImage(file, rules);
  const upload = "file" in prepared ? prepared.file : null;
  const problem =
    prepared.status === "tooSmall"
      ? `Too small (${prepared.width} x ${prepared.height})`
      : prepared.status === "unreadable"
        ? "Couldn't read this photo"
        : null;

  return {
    id: crypto.randomUUID(),
    fileName: file.name,
    previewUrl: URL.createObjectURL(upload ?? file),
    file: upload,
    problem,
    cropped: prepared.status === "cropped",
    name: parsed.name ?? "",
    categoryId: parsed.category?.id ?? "",
    categoryNote: parsed.category ? "file" : parsed.missingCategory ? "missing" : "none",
    missingCategory: parsed.missingCategory,
    price: "",
    stock: "0",
    sizes: [],
    sizeStock: {},
  };
}
