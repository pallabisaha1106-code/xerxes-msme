import { describe, expect, it } from "vitest";
import { matchCategory, parseFileName, type CategoryOption } from "./fileNameParser";

const KURTI: CategoryOption = { id: "c-kurti", name: "Kurti" };
const SAREE: CategoryOption = { id: "c-saree", name: "Saree" };
const LADIES: CategoryOption = { id: "c-ladies", name: "Ladies" };
const LADIES_WEAR: CategoryOption = { id: "c-ladies-wear", name: "Ladies Wear" };
const MENS_WEAR: CategoryOption = { id: "c-mens", name: "Men's Wear" };
const DRESS: CategoryOption = { id: "c-dress", name: "Dress" };
const ALL = [KURTI, SAREE, LADIES, LADIES_WEAR, MENS_WEAR, DRESS];

describe("parseFileName - the name", () => {
  it("drops the IMG_ prefix and the extension and turns underscores into spaces", () => {
    expect(parseFileName("IMG_Kurti_Blue Cotton.jpg", ALL).name).toBe("Kurti Blue Cotton");
  });
  it("works without the IMG_ prefix", () => {
    expect(parseFileName("Kurti_Blue Cotton.jpg", ALL).name).toBe("Kurti Blue Cotton");
  });
  it("is case-insensitive about the prefix and accepts other extensions", () => {
    expect(parseFileName("img_Saree_Red Silk.JPEG", ALL).name).toBe("Saree Red Silk");
    expect(parseFileName("IMG_Saree.png", ALL).name).toBe("Saree");
  });
  it("keeps hyphens inside a name", () => {
    expect(parseFileName("IMG_T-Shirt_Blue.jpg", ALL).name).toBe("T-Shirt Blue");
  });
  it("collapses stray spaces and empty segments", () => {
    expect(parseFileName("IMG__Kurti___Blue   Cotton .jpg", ALL).name).toBe("Kurti Blue Cotton");
  });
  it("treats camera and messenger names as having no name", () => {
    for (const file of ["IMG_2231.jpg", "IMG_20260115_103045.jpg", "DSC_0042.JPG", "PXL_20260115_103045123.jpg", "WhatsApp Image 2026-01-15 at 10.30.45 AM.jpeg", "2231.jpg", ".jpg"]) {
      expect(parseFileName(file, ALL)).toEqual({ name: null, category: null, missingCategory: null });
    }
  });
  it("keeps non-Latin names", () => {
    expect(parseFileName("IMG_कुर्ता_नीला.jpg", ALL).name).toBe("कुर्ता नीला");
  });
  it("caps very long names", () => {
    expect(parseFileName(`IMG_${"a".repeat(300)}.jpg`, ALL).name).toHaveLength(120);
  });
});

describe("matchCategory", () => {
  it("matches the category the name starts with", () => {
    expect(matchCategory("Kurti Blue Cotton", ALL)).toBe(KURTI);
    expect(matchCategory("Saree Red Silk", ALL)).toBe(SAREE);
  });
  it("ignores case, spaces, underscores and hyphens", () => {
    expect(matchCategory("kurti blue", ALL)).toBe(KURTI);
    expect(matchCategory("LADIES-WEAR Kurta", ALL)).toBe(LADIES_WEAR);
    expect(matchCategory("Ladies_Wear Top", ALL)).toBe(LADIES_WEAR);
  });
  it("ignores apostrophes", () => {
    expect(matchCategory("Mens Wear Shirt", ALL)).toBe(MENS_WEAR);
    expect(matchCategory("Men’s Wear Shirt", ALL)).toBe(MENS_WEAR);
  });
  it("treats a trailing s or es as the same word", () => {
    expect(matchCategory("Kurtis Blue", ALL)).toBe(KURTI);
    expect(matchCategory("Dresses Floral", ALL)).toBe(DRESS);
    expect(matchCategory("Sarees Red", ALL)).toBe(SAREE);
  });
  it("prefers the longest match", () => {
    expect(matchCategory("Ladies Wear Kurta", ALL)).toBe(LADIES_WEAR);
    expect(matchCategory("Ladies Top", ALL)).toBe(LADIES);
  });
  it("only matches whole words at the start", () => {
    expect(matchCategory("Kurtina Blue", ALL)).toBeNull();
    expect(matchCategory("Blue Kurti", ALL)).toBeNull();
  });
  it("returns null when there are no categories", () => {
    expect(matchCategory("Kurti Blue", [])).toBeNull();
  });
});

describe("parseFileName - category and the missing-category guess", () => {
  it("finds the category from the file name", () => {
    expect(parseFileName("IMG_Kurti_Blue Cotton.jpg", ALL)).toEqual({ name: "Kurti Blue Cotton", category: KURTI, missingCategory: null });
  });
  it("guesses a missing category from the Category_Rest structure", () => {
    expect(parseFileName("IMG_Handbag_Black.jpg", ALL)).toEqual({ name: "Handbag Black", category: null, missingCategory: "Handbag" });
    expect(parseFileName("IMG_Home Decor_Vase.jpg", ALL).missingCategory).toBe("Home Decor");
  });
  it("matches by the name's start, so a longer category word still lands in the shorter existing category", () => {
    // "Ladies" exists, so "Ladies Footwear Sandal" goes there (visible on the review card, and editable) rather than guessing "Create Ladies Footwear".
    const result = parseFileName("IMG_Ladies Footwear_Sandal.jpg", ALL);
    expect(result.category).toBe(LADIES);
    expect(result.missingCategory).toBeNull();
  });
  it("guesses a missing category from a single word", () => {
    expect(parseFileName("IMG_Handbag.jpg", ALL)).toEqual({ name: "Handbag", category: null, missingCategory: "Handbag" });
  });
  it("makes no guess from a plain multi-word name", () => {
    expect(parseFileName("IMG_Blue Cotton Kurta.jpg", ALL)).toEqual({ name: "Blue Cotton Kurta", category: null, missingCategory: null });
  });
  it("matches when the whole name has no underscores", () => {
    expect(parseFileName("IMG_Ladies Wear Kurta.jpg", ALL).category).toBe(LADIES_WEAR);
  });
});
