import { describe, expect, it, vi } from "vitest";
import type { ProductResponse, VariantResponse } from "../../../api/productApi";
import type { Draft } from "./draft";
import { newProgress, type CardProgress } from "./createOne";
import { finishProduct, openingStock, type FinishApi } from "./finishProduct";

const draft = (overrides: Partial<Draft> = {}): Draft => ({
  id: "d1",
  fileName: "x.jpg",
  previewUrl: "blob:x",
  file: new File(["x"], "x.jpg"),
  problem: null,
  cropped: false,
  name: "Kurti",
  categoryId: "",
  categoryNote: null,
  missingCategory: null,
  price: "",
  stock: "0",
  sizes: [],
  sizeStock: {},
  ...overrides,
});

const saved = (sizes: (string | null)[]): CardProgress => {
  const variants = sizes.map(
    (size, i) => ({ id: `v${i + 1}`, attributes: size ? { Size: size } : {} }) as unknown as VariantResponse,
  );
  return { ...newProgress(), product: { id: "p1", variants } as ProductResponse, photoUploaded: true };
};

const makeApi = (overrides: Partial<FinishApi> = {}): FinishApi => ({
  copyVariantImages: vi.fn(async () => ({})),
  recordAdjustment: vi.fn(async () => ({})),
  ...overrides,
});

describe("openingStock", () => {
  it("reads whole pieces and treats anything else as none", () => {
    expect(openingStock("12")).toBe(12);
    expect(openingStock("7.9")).toBe(7);
    expect(openingStock("")).toBe(0);
    expect(openingStock(undefined)).toBe(0);
    expect(openingStock("-3")).toBe(0);
    expect(openingStock("abc")).toBe(0);
  });
});

describe("finishProduct", () => {
  it("with no sizes: records the single stock figure and copies nothing", async () => {
    const api = makeApi();

    await finishProduct(draft({ stock: "20" }), saved([null]), api);

    expect(api.copyVariantImages).not.toHaveBeenCalled();
    expect(api.recordAdjustment).toHaveBeenCalledWith("v1", { signedDelta: 20, remarks: "Opening stock" });
  });

  it("with zero stock: records nothing", async () => {
    const api = makeApi();

    await finishProduct(draft({ stock: "0" }), saved([null]), api);

    expect(api.recordAdjustment).not.toHaveBeenCalled();
  });

  it("with sizes: shares the first variant's photo with the others and stocks each size from its own box", async () => {
    const api = makeApi();
    const card = draft({ sizes: ["M", "L", "XL"], sizeStock: { M: "5", L: "", XL: "2" }, stock: "99" });

    await finishProduct(card, saved(["M", "L", "XL"]), api);

    expect(api.copyVariantImages).toHaveBeenCalledTimes(2);
    expect(api.copyVariantImages).toHaveBeenCalledWith("p1", "v2", "v1");
    expect(api.copyVariantImages).toHaveBeenCalledWith("p1", "v3", "v1");
    expect(api.recordAdjustment).toHaveBeenCalledTimes(2);
    expect(api.recordAdjustment).toHaveBeenCalledWith("v1", expect.objectContaining({ signedDelta: 5 }));
    expect(api.recordAdjustment).toHaveBeenCalledWith("v3", expect.objectContaining({ signedDelta: 2 }));
  });

  it("matches a size typed with stray spaces", async () => {
    const api = makeApi();

    await finishProduct(draft({ sizes: [" Free "], sizeStock: { " Free ": "4" } }), saved(["Free"]), api);

    expect(api.recordAdjustment).toHaveBeenCalledWith("v1", expect.objectContaining({ signedDelta: 4 }));
  });

  it("after a failure, trying again skips the variants already done", async () => {
    const recordAdjustment = vi
      .fn<FinishApi["recordAdjustment"]>()
      .mockResolvedValueOnce({})
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValue({});
    const api = makeApi({ recordAdjustment });
    const card = draft({ sizes: ["M", "L"], sizeStock: { M: "5", L: "6" } });
    const progress = saved(["M", "L"]);

    await expect(finishProduct(card, progress, api)).rejects.toThrow("network");
    expect(progress.copiedVariantIds).toEqual(["v2"]);
    expect(progress.stockedVariantIds).toEqual(["v1"]);

    await finishProduct(card, progress, api);

    expect(api.copyVariantImages).toHaveBeenCalledTimes(1);
    expect(recordAdjustment).toHaveBeenCalledTimes(3);
    expect(recordAdjustment).toHaveBeenLastCalledWith("v2", expect.objectContaining({ signedDelta: 6 }));
  });

  it("refuses to run before the product and photo are saved", async () => {
    await expect(finishProduct(draft(), newProgress(), makeApi())).rejects.toThrow();
  });
});
