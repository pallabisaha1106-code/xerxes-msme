import { describe, expect, it } from "vitest";
import type { StarterCatalogGroup } from "../../../api/starterCatalogApi";
import { atCapacity, filterGroups, nameKey, ownedProductIds, selectedCountByGroup, selectedProducts } from "./starterCatalog";

const catalog: StarterCatalogGroup[] = [
  { id: "g1", name: "Rice", products: [{ id: "p1", name: "Basmati Rice", imageUrl: null }] },
  {
    id: "g2",
    name: "Milk & Dairy Products",
    products: [
      { id: "p2", name: "Amul Milk", imageUrl: null },
      { id: "p3", name: "Amul Butter", imageUrl: null },
    ],
  },
];

describe("starterCatalog", () => {
  it("compares names ignoring capitals and stray spaces", () => {
    expect(nameKey("  Amul MILK ")).toBe("amul milk");
  });

  it("finds a group by its own name or by a product inside it", () => {
    expect(filterGroups(catalog, "").map((g) => g.id)).toEqual(["g1", "g2"]);
    expect(filterGroups(catalog, "dairy").map((g) => g.id)).toEqual(["g2"]);
    expect(filterGroups(catalog, "BUTTER").map((g) => g.id)).toEqual(["g2"]);
    expect(filterGroups(catalog, "zzz")).toEqual([]);
  });

  it("counts and lists what is picked", () => {
    const picked = new Set(["p2", "p3"]);
    expect(selectedCountByGroup(catalog, picked).get("g1")).toBe(0);
    expect(selectedCountByGroup(catalog, picked).get("g2")).toBe(2);
    expect(selectedProducts(catalog, picked).map((p) => p.name)).toEqual(["Amul Milk", "Amul Butter"]);
  });

  it("recognises products the vendor already has, whatever the capitals", () => {
    expect([...ownedProductIds(catalog, ["AMUL milk", "Something else"])]).toEqual(["p2"]);
    expect(ownedProductIds(catalog, []).size).toBe(0);
  });

  it("is at capacity only when a limit is known and reached", () => {
    expect(atCapacity(3, 3)).toBe(true);
    expect(atCapacity(2, 3)).toBe(false);
    expect(atCapacity(50, null)).toBe(false);
    expect(atCapacity(0, 0)).toBe(true);
  });
});
