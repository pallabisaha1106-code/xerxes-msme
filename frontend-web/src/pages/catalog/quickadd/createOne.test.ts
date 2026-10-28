import { AxiosError } from "axios";
import { describe, expect, it, vi } from "vitest";
import type { ProductRequest, ProductResponse, VariantResponse } from "../../../api/productApi";
import type { Draft } from "./draft";
import { createProductWithPhoto, newProgress, type CreateApi } from "./createOne";

const draft = (overrides: Partial<Draft> = {}): Draft => ({
  id: "d1",
  fileName: "IMG_Kurti_Blue.jpg",
  previewUrl: "blob:x",
  file: new File(["x"], "x.jpg"),
  problem: null,
  cropped: false,
  name: "Kurti Blue",
  categoryId: "new:Kurti",
  categoryNote: null,
  missingCategory: null,
  price: "499",
  stock: "0",
  sizes: [],
  sizeStock: {},
  ...overrides,
});

const responseFor = (request: ProductRequest): ProductResponse =>
  ({
    id: "p1",
    name: request.name,
    categoryId: request.categoryId ?? null,
    variants: request.variants.map((v, i) => ({ id: `v${i + 1}`, sku: v.sku, attributes: v.attributes ?? {} }) as VariantResponse),
  }) as ProductResponse;

const clash = () => new AxiosError("conflict", "ERR_BAD_REQUEST", undefined, undefined, { status: 409 } as never);

const makeApi = (overrides: Partial<CreateApi> = {}): CreateApi => ({
  createProduct: vi.fn(async (request: ProductRequest) => responseFor(request)),
  uploadVariantImage: vi.fn(async () => ({})),
  ...overrides,
});

describe("createProductWithPhoto", () => {
  it("creates the product with the real category id and uploads the photo to its only variant", async () => {
    const api = makeApi();
    const card = draft();

    const product = await createProductWithPhoto(card, "cat-1", newProgress(), api);

    expect(api.createProduct).toHaveBeenCalledTimes(1);
    const request = (api.createProduct as ReturnType<typeof vi.fn>).mock.calls[0][0] as ProductRequest;
    expect(request.categoryId).toBe("cat-1");
    expect(request.name).toBe("Kurti Blue");
    expect(request.variants).toHaveLength(1);
    expect(api.uploadVariantImage).toHaveBeenCalledWith("p1", "v1", card.file);
    expect(product.id).toBe("p1");
  });

  it("makes one variant per size and uploads the photo to the first", async () => {
    const api = makeApi();

    await createProductWithPhoto(draft({ sizes: ["M", "L"] }), null, newProgress(), api);

    const request = (api.createProduct as ReturnType<typeof vi.fn>).mock.calls[0][0] as ProductRequest;
    expect(request.categoryId).toBeNull();
    expect(request.variants.map((v) => v.attributes)).toEqual([{ Size: "M" }, { Size: "L" }]);
    expect(api.uploadVariantImage).toHaveBeenCalledWith("p1", "v1", expect.any(File));
  });

  it("tries a new SKU when the server says it already exists", async () => {
    const createProduct = vi
      .fn<(request: ProductRequest) => Promise<ProductResponse>>()
      .mockRejectedValueOnce(clash())
      .mockImplementation(async (request) => responseFor(request));
    const api = makeApi({ createProduct });

    await createProductWithPhoto(draft(), null, newProgress(), api);

    expect(createProduct).toHaveBeenCalledTimes(2);
    expect(createProduct.mock.calls[0][0].variants[0].sku).toBe("KURTI-BLUE");
    expect(createProduct.mock.calls[1][0].variants[0].sku).toBe("KURTI-BLUE-2");
  });

  it("gives up after ten SKU clashes", async () => {
    const createProduct = vi.fn(async () => {
      throw clash();
    });

    await expect(createProductWithPhoto(draft(), null, newProgress(), makeApi({ createProduct }))).rejects.toBeInstanceOf(AxiosError);
    expect(createProduct).toHaveBeenCalledTimes(10);
  });

  it("does not retry other errors", async () => {
    const createProduct = vi.fn(async () => {
      throw new AxiosError("limit", "ERR_BAD_REQUEST", undefined, undefined, { status: 422 } as never);
    });
    const api = makeApi({ createProduct });

    await expect(createProductWithPhoto(draft(), null, newProgress(), api)).rejects.toBeDefined();
    expect(createProduct).toHaveBeenCalledTimes(1);
    expect(api.uploadVariantImage).not.toHaveBeenCalled();
  });

  it("after a failed photo upload, trying again only uploads the photo", async () => {
    const uploadVariantImage = vi
      .fn<CreateApi["uploadVariantImage"]>()
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValue({});
    const api = makeApi({ uploadVariantImage });
    const progress = newProgress();

    await expect(createProductWithPhoto(draft(), null, progress, api)).rejects.toThrow("network");
    expect(progress.product?.id).toBe("p1");
    expect(progress.photoUploaded).toBe(false);

    await createProductWithPhoto(draft(), null, progress, api);

    expect(api.createProduct).toHaveBeenCalledTimes(1);
    expect(uploadVariantImage).toHaveBeenCalledTimes(2);
    expect(progress.photoUploaded).toBe(true);
  });

  it("refuses a card with no usable photo", async () => {
    const api = makeApi();

    await expect(createProductWithPhoto(draft({ file: null }), null, newProgress(), api)).rejects.toThrow();
    expect(api.createProduct).not.toHaveBeenCalled();
  });
});
