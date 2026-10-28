import { describe, expect, it, vi } from "vitest";
import type { Draft } from "./draft";
import { categoryIdFor, resolveNewCategories } from "./resolveCategories";

const draft = (categoryId: string, overrides: Partial<Draft> = {}): Draft => ({
  id: crypto.randomUUID(),
  fileName: "x.jpg",
  previewUrl: "blob:x",
  file: new File(["x"], "x.jpg"),
  problem: null,
  cropped: false,
  name: "Item",
  categoryId,
  categoryNote: null,
  missingCategory: null,
  price: "",
  stock: "0",
  sizes: [],
  sizeStock: {},
  ...overrides,
});

const make = () => vi.fn(async (name: string) => ({ id: `id-${name}`, name }));

describe("resolveNewCategories", () => {
  it("creates each wanted category once, however many photos use it", async () => {
    const create = make();
    const outcome = await resolveNewCategories([draft("new:Saree"), draft("new:Saree"), draft("new:saree")], [], create);

    expect(create).toHaveBeenCalledTimes(1);
    expect(outcome.ids.get("saree")).toBe("id-Saree");
    expect(outcome.created).toEqual([{ id: "id-Saree", name: "Saree" }]);
  });

  it("ignores a new category only a skipped photo wanted", async () => {
    const create = make();
    const skipped = draft("new:Saree", { file: null, problem: "Too small" });

    const outcome = await resolveNewCategories([skipped, draft("new:Kurti")], [], create);

    expect(create).toHaveBeenCalledTimes(1);
    expect(create).toHaveBeenCalledWith("Kurti");
    expect(outcome.ids.has("saree")).toBe(false);
  });

  it("reuses a category that already exists, whatever its letter case", async () => {
    const create = make();
    const outcome = await resolveNewCategories([draft("new:Saree")], [{ id: "c1", name: "SAREE" }], create);

    expect(create).not.toHaveBeenCalled();
    expect(outcome.ids.get("saree")).toBe("c1");
    expect(outcome.created).toEqual([]);
  });

  it("reports a failure and still creates the others", async () => {
    const create = vi.fn(async (name: string) => {
      if (name === "Saree") throw new Error("boom");
      return { id: `id-${name}`, name };
    });

    const outcome = await resolveNewCategories([draft("new:Saree"), draft("new:Kurti")], [], create);

    expect(outcome.failed).toHaveLength(1);
    expect(outcome.failed[0].name).toBe("Saree");
    expect(outcome.ids.get("kurti")).toBe("id-Kurti");
  });

  it("does nothing when no draft wants a new category", async () => {
    const create = make();
    const outcome = await resolveNewCategories([draft(""), draft("existing-id")], [], create);

    expect(create).not.toHaveBeenCalled();
    expect(outcome.ids.size).toBe(0);
  });
});

describe("categoryIdFor", () => {
  const ids = new Map([["saree", "real-1"]]);

  it("maps each kind of category choice", () => {
    expect(categoryIdFor(draft(""), ids)).toBeNull();
    expect(categoryIdFor(draft("c9"), ids)).toBe("c9");
    expect(categoryIdFor(draft("new:Saree"), ids)).toBe("real-1");
    expect(categoryIdFor(draft("new:Kurti"), ids)).toBeUndefined();
  });
});
