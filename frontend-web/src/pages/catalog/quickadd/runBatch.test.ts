import { describe, expect, it, vi } from "vitest";
import type { ProductRequest, ProductResponse, VariantResponse } from "../../../api/productApi";
import type { Draft } from "./draft";
import type { CardProgress } from "./createOne";
import { CONCURRENCY, runBatch, type BatchApi, type CardStatus } from "./runBatch";

const draft = (name: string, overrides: Partial<Draft> = {}): Draft => ({
  id: `id-${name}`,
  fileName: `${name}.jpg`,
  previewUrl: "blob:x",
  file: new File(["x"], `${name}.jpg`),
  problem: null,
  cropped: false,
  name,
  categoryId: "",
  categoryNote: null,
  missingCategory: null,
  price: "",
  stock: "0",
  sizes: [],
  sizeStock: {},
  ...overrides,
});

const tick = () => new Promise((resolve) => setTimeout(resolve, 5));

function setup(overrides: Partial<BatchApi> = {}) {
  const events: { id: string; status: CardStatus; message?: string }[] = [];
  const progress = new Map<string, CardProgress>();
  let inFlight = 0;
  let peak = 0;
  const api: BatchApi = {
    listCategories: vi.fn(async () => []),
    createCategory: vi.fn(async (name: string) => ({ id: `cat-${name}`, name })),
    createProduct: vi.fn(async (request: ProductRequest) => {
      inFlight++;
      peak = Math.max(peak, inFlight);
      await tick();
      inFlight--;
      return {
        id: `prod-${request.name}`,
        name: request.name,
        variants: request.variants.map((v, i) => ({ id: `${request.name}-v${i}`, attributes: v.attributes ?? {} }) as VariantResponse),
      } as ProductResponse;
    }),
    uploadVariantImage: vi.fn(async () => ({})),
    copyVariantImages: vi.fn(async () => ({})),
    recordAdjustment: vi.fn(async () => ({})),
    ...overrides,
  };
  const options = {
    api,
    progress,
    describe: (error: unknown) => (error instanceof Error ? error.message : "error"),
    onStatus: (id: string, status: CardStatus, message?: string) => events.push({ id, status, message }),
  };
  return { api, options, events, progress, peak: () => peak };
}

const statusOf = (events: { id: string; status: CardStatus }[], id: string) => events.filter((e) => e.id === id).map((e) => e.status);

describe("runBatch", () => {
  it("creates every usable card, working then done", async () => {
    const { api, options, events } = setup();

    await runBatch([draft("A"), draft("B")], options);

    expect(api.createProduct).toHaveBeenCalledTimes(2);
    expect(statusOf(events, "id-A")).toEqual(["working", "done"]);
    expect(statusOf(events, "id-B")).toEqual(["working", "done"]);
  });

  it("never runs more than three cards at once", async () => {
    const { options, peak } = setup();

    await runBatch(Array.from({ length: 10 }, (_, i) => draft(`P${i}`)), options);

    expect(peak()).toBe(CONCURRENCY);
  });

  it("skips cards with no usable photo, without reporting them", async () => {
    const { api, options, events } = setup();

    await runBatch([draft("A"), draft("Tiny", { file: null, problem: "Too small" })], options);

    expect(api.createProduct).toHaveBeenCalledTimes(1);
    expect(events.some((e) => e.id === "id-Tiny")).toBe(false);
  });

  it("reports a failed card and carries on with the rest", async () => {
    const { options, events } = setup({
      uploadVariantImage: vi.fn(async (_p: string, variantId: string) => {
        if (variantId.startsWith("Bad")) throw new Error("photo rejected");
        return {};
      }),
    });

    await runBatch([draft("Bad"), draft("Good")], options);

    expect(events.find((e) => e.id === "id-Bad" && e.status === "failed")?.message).toBe("photo rejected");
    expect(statusOf(events, "id-Good")).toEqual(["working", "done"]);
  });

  it("creates a new category once, and uses its real id for every card that chose it", async () => {
    const { api, options } = setup();

    await runBatch([draft("A", { categoryId: "new:Saree" }), draft("B", { categoryId: "new:Saree" })], options);

    expect(api.createCategory).toHaveBeenCalledTimes(1);
    const requests = (api.createProduct as ReturnType<typeof vi.fn>).mock.calls.map((c) => c[0] as ProductRequest);
    expect(requests.map((r) => r.categoryId)).toEqual(["cat-Saree", "cat-Saree"]);
  });

  it("returns the categories it created, and makes no category calls when none were chosen", async () => {
    const withNew = setup();
    const result = await runBatch([draft("A", { categoryId: "new:Saree" })], withNew.options);
    expect(result.createdCategories).toEqual([{ id: "cat-Saree", name: "Saree" }]);

    const without = setup();
    await runBatch([draft("A", { categoryId: "existing" })], without.options);
    expect(without.api.listCategories).not.toHaveBeenCalled();
  });

  it("fails only the cards whose new category could not be created", async () => {
    const { api, options, events } = setup({
      createCategory: vi.fn(async (name: string) => {
        if (name === "Saree") throw new Error("not allowed");
        return { id: `cat-${name}`, name };
      }),
    });

    await runBatch([draft("A", { categoryId: "new:Saree" }), draft("B", { categoryId: "new:Kurti" }), draft("C")], options);

    expect(events.find((e) => e.id === "id-A" && e.status === "failed")?.message).toContain('"Saree" category: not allowed');
    expect(statusOf(events, "id-B")).toEqual(["working", "done"]);
    expect(statusOf(events, "id-C")).toEqual(["working", "done"]);
    expect(api.createProduct).toHaveBeenCalledTimes(2);
  });

  it("fails the cards that need a new category when the category list can't be read", async () => {
    const { api, options, events } = setup({ listCategories: vi.fn(async () => Promise.reject(new Error("offline"))) });

    await runBatch([draft("A", { categoryId: "new:Saree" }), draft("B")], options);

    expect(events.find((e) => e.id === "id-A" && e.status === "failed")?.message).toContain("offline");
    expect(api.createCategory).not.toHaveBeenCalled();
    expect(statusOf(events, "id-B")).toEqual(["working", "done"]);
  });

  it("retrying a failed card carries on without creating the product again", async () => {
    let failOnce = true;
    const { api, options, events } = setup({
      uploadVariantImage: vi.fn(async () => {
        if (failOnce) {
          failOnce = false;
          throw new Error("network");
        }
        return {};
      }),
    });
    const card = draft("A");

    await runBatch([card], options);
    expect(statusOf(events, "id-A")).toEqual(["working", "failed"]);

    await runBatch([card], options);

    expect(api.createProduct).toHaveBeenCalledTimes(1);
    expect(api.uploadVariantImage).toHaveBeenCalledTimes(2);
    expect(statusOf(events, "id-A")).toEqual(["working", "failed", "working", "done"]);
  });
});
