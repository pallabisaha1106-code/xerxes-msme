import { describe, expect, it } from "vitest";
import { buildProductRequest, skuFor } from "./sizeVariants";

describe("skuFor", () => {
  it("builds an upper-case dashed SKU from the name", () => {
    expect(skuFor("Kurti Blue Cotton", null)).toBe("KURTI-BLUE-COTTON");
  });
  it("adds the size", () => {
    expect(skuFor("Kurti Blue Cotton", "M")).toBe("KURTI-BLUE-COTTON-M");
    expect(skuFor("Kurti Blue Cotton", "Free size")).toBe("KURTI-BLUE-COTTON-FREE-SIZE");
  });
  it("adds the attempt number before the size, from the second attempt on", () => {
    expect(skuFor("Kurti", null, 1)).toBe("KURTI");
    expect(skuFor("Kurti", null, 2)).toBe("KURTI-2");
    expect(skuFor("Kurti", "M", 3)).toBe("KURTI-3-M");
  });
  it("strips punctuation and edge dashes", () => {
    expect(skuFor("  Men's T-Shirt (XL)! ", null)).toBe("MEN-S-T-SHIRT-XL");
  });
  it("shortens long names without leaving a trailing dash", () => {
    const sku = skuFor("A".repeat(39) + " " + "B".repeat(30), "L");
    expect(sku.length).toBeLessThanOrEqual(40 + "-L".length);
    expect(sku).not.toContain("--");
    expect(sku.endsWith("-L")).toBe(true);
  });
  it("falls back to ITEM when the name has no Latin letters or digits", () => {
    expect(skuFor("कुर्ता नीला", null)).toBe("ITEM");
    expect(skuFor("कुर्ता", "M")).toBe("ITEM-M");
  });
});

describe("buildProductRequest", () => {
  const base = { name: "Kurti Blue Cotton", categoryId: "cat-1", price: "", sizes: [] as string[] };

  it("makes one default variant when no sizes are picked", () => {
    const request = buildProductRequest(base);
    expect(request.variants).toEqual([{ sku: "KURTI-BLUE-COTTON", attributes: {}, purchasePrice: 0, sellingPrice: 0 }]);
  });
  it("makes one variant per size, in the order given, with the Size attribute", () => {
    const request = buildProductRequest({ ...base, sizes: ["M", "L", "XL"], price: "499" });
    expect(request.variants.map((v) => [v.sku, v.attributes, v.sellingPrice])).toEqual([
      ["KURTI-BLUE-COTTON-M", { Size: "M" }, 499],
      ["KURTI-BLUE-COTTON-L", { Size: "L" }, 499],
      ["KURTI-BLUE-COTTON-XL", { Size: "XL" }, 499],
    ]);
  });
  it("uses the safe starter defaults for everything else", () => {
    const request = buildProductRequest({ ...base, sizes: ["S"] });
    expect(request).toMatchObject({ name: "Kurti Blue Cotton", categoryId: "cat-1", defaultGstRate: 0, defaultUom: "PCS" });
    expect(request.variants[0].purchasePrice).toBe(0);
  });
  it("keeps a blank, zero, negative or non-numeric price as 0 (not priced yet)", () => {
    for (const price of ["", "  ", "0", "-5", "abc"]) {
      expect(buildProductRequest({ ...base, price }).variants[0].sellingPrice).toBe(0);
    }
  });
  it("rounds the price to paise", () => {
    expect(buildProductRequest({ ...base, price: "499.999" }).variants[0].sellingPrice).toBe(500);
    expect(buildProductRequest({ ...base, price: "12.345" }).variants[0].sellingPrice).toBeCloseTo(12.35, 2);
  });
  it("drops blank and repeated sizes", () => {
    const request = buildProductRequest({ ...base, sizes: ["M", " M ", "", "L"] });
    expect(request.variants.map((v) => v.attributes)).toEqual([{ Size: "M" }, { Size: "L" }]);
  });
  it("trims the name and passes a missing category through", () => {
    const request = buildProductRequest({ ...base, name: "  Kurti  ", categoryId: null });
    expect(request.name).toBe("Kurti");
    expect(request.categoryId).toBeNull();
  });
  it("shifts every SKU together when retrying after a clash", () => {
    const request = buildProductRequest({ ...base, sizes: ["M", "L"] }, 2);
    expect(request.variants.map((v) => v.sku)).toEqual(["KURTI-BLUE-COTTON-2-M", "KURTI-BLUE-COTTON-2-L"]);
  });
});
