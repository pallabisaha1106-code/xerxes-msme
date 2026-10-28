import { useEffect, useState, type FormEvent } from "react";
import { Button, Dialog, Icon, ImagePicker, Input, Select, useToast } from "../../ui";
import type { ImageRules } from "../../lib/imageChecks";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import type { ProductImageRules, ProductResponse, ProductVariantImageResponse, VariantResponse } from "../../api/productApi";
import { resolveImagePath } from "../../lib/mediaUrl";
import "./new-product.css";
import "./variant-images.css";

// Used until the shop's real limits arrive from the server (or if that call fails) - the same values the
// server applied to every shop before photo profiles existed.
const FALLBACK_IMAGE_RULES: ProductImageRules = {
  profile: "DETAIL",
  minWidth: 720,
  minHeight: 900,
  minAspectRatio: 0.55,
  maxAspectRatio: 0.9,
};

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

/** The server stays the source of truth; this just lets the vendor hear about a bad photo before uploading it. */
function toImageRules(rules: ProductImageRules): ImageRules {
  return {
    minWidth: rules.minWidth,
    minHeight: rules.minHeight,
    minAspect: rules.minAspectRatio,
    maxAspect: rules.maxAspectRatio,
    aspectHint:
      rules.profile === "COMPACT"
        ? "a photo that is no more than twice as tall as it is wide, or the reverse"
        : "a portrait photo, roughly 3:4 to 4:5 (taller than wide)",
    maxBytes: MAX_UPLOAD_BYTES,
  };
}

function imageGuidelines(rules: ProductImageRules): string[] {
  const size = `At least ${rules.minWidth}×${rules.minHeight} pixels`;
  return rules.profile === "COMPACT"
    ? [
        "A clear photo of the product with the front of the pack facing the camera",
        size,
        "Square or portrait works best; nothing extremely tall or wide",
        "A photo of your own stock, not a supplier/stock image",
      ]
    : [
        "Portrait framing, roughly 3:4 to 4:5 (taller than wide)",
        size,
        "The full garment visible, not cropped",
        "A photo of your own stock, not a supplier/stock image",
      ];
}

function VariantImages({ productId, variantId }: { productId: string; variantId: string }) {
  const toast = useToast();
  const [rules, setRules] = useState<ProductImageRules>(FALLBACK_IMAGE_RULES);
  const [images, setImages] = useState<ProductVariantImageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [pickerKey, setPickerKey] = useState(0);

  async function load() {
    setLoading(true);
    try {
      setImages(await productApi.listVariantImages(productId, variantId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load photos", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productId, variantId]);

  useEffect(() => {
    productApi
      .getProductImageRules()
      .then(setRules)
      .catch(() => setRules(FALLBACK_IMAGE_RULES));
  }, []);

  async function handleUpload() {
    if (!pendingFile) return;

    setUploading(true);
    try {
      const image = await productApi.uploadVariantImage(productId, variantId, pendingFile);
      setImages((current) => [...current, image]);
      setPendingFile(null);
      setPickerKey((k) => k + 1);
      toast.show({ severity: "success", summary: "Photo uploaded" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not upload photo", detail: extractErrorMessage(err) });
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(imageId: string) {
    try {
      await productApi.deleteVariantImage(productId, variantId, imageId);
      setImages((current) => current.filter((i) => i.id !== imageId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not delete photo", detail: extractErrorMessage(err) });
    }
  }

  return (
    <div className="field">
      <label className="field-label">Photos</label>
      <ul className="variant-image-guidelines">
        {imageGuidelines(rules).map((rule) => (
          <li key={rule}>{rule}</li>
        ))}
      </ul>

      {!loading && (
        <div className="variant-image-grid">
          {images.map((image) => (
            <div key={image.id} className="variant-image-thumb">
              <img src={resolveImagePath(image.thumbnailUrl)} alt="" width={image.width} height={image.height} />
              <button
                type="button"
                className="variant-image-remove"
                onClick={() => handleDelete(image.id)}
                aria-label="Remove photo"
              >
                <Icon name="x" size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      <ImagePicker
        key={pickerKey}
        id={`variant-photo-${variantId}`}
        rules={toImageRules(rules)}
        cropAspect={rules.profile === "COMPACT" ? 1 : 3 / 4}
        disabled={uploading}
        onChange={setPendingFile}
      />
      <Button
        type="button"
        variant="outlined"
        size="sm"
        icon="plus"
        loading={uploading}
        disabled={!pendingFile}
        onClick={handleUpload}
      >
        Upload Photo
      </Button>
    </div>
  );
}

interface AttributeRow {
  key: string;
  value: string;
}

interface VariantFormState {
  sku: string;
  barcode: string;
  attributes: AttributeRow[];
  purchasePrice: string;
  sellingPrice: string;
  reorderThreshold: string;
  hsnCode: string;
  gstRate: string;
  uom: string;
}

function blankForm(): VariantFormState {
  return {
    sku: "",
    barcode: "",
    attributes: [{ key: "", value: "" }],
    purchasePrice: "",
    sellingPrice: "",
    reorderThreshold: "0",
    hsnCode: "",
    gstRate: "",
    uom: "",
  };
}

function formFromVariant(variant: VariantResponse): VariantFormState {
  const attributes = Object.entries(variant.attributes).map(([key, value]) => ({ key, value }));
  return {
    sku: variant.sku,
    barcode: variant.barcode ?? "",
    attributes: attributes.length > 0 ? attributes : [{ key: "", value: "" }],
    purchasePrice: String(variant.purchasePrice),
    sellingPrice: String(variant.sellingPrice),
    reorderThreshold: String(variant.reorderThreshold),
    hsnCode: variant.hsnCode ?? "",
    gstRate: variant.gstRate != null ? String(variant.gstRate) : "",
    uom: variant.uom ?? "",
  };
}

const UOM_OPTIONS = [
  { label: "Pieces (PCS)", value: "PCS" },
  { label: "Set", value: "SET" },
  { label: "Pair", value: "PAIR" },
  { label: "Box", value: "BOX" },
  { label: "Kilogram (KG)", value: "KG" },
  { label: "Metre (MTR)", value: "MTR" },
];

export function VariantFormDialog({
  visible,
  onHide,
  productId,
  variant,
  onSaved,
}: {
  visible: boolean;
  onHide: () => void;
  productId: string;
  variant: VariantResponse | null;
  onSaved: (product: ProductResponse) => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState<VariantFormState>(blankForm());
  const [submitting, setSubmitting] = useState(false);
  const isEdit = variant != null;

  useEffect(() => {
    if (visible) {
      setForm(variant ? formFromVariant(variant) : blankForm());
    }
  }, [visible, variant]);

  function update(patch: Partial<VariantFormState>) {
    setForm((current) => ({ ...current, ...patch }));
  }

  function updateAttribute(index: number, patch: Partial<AttributeRow>) {
    setForm((current) => ({
      ...current,
      attributes: current.attributes.map((a, i) => (i === index ? { ...a, ...patch } : a)),
    }));
  }

  function addAttribute() {
    setForm((current) => ({ ...current, attributes: [...current.attributes, { key: "", value: "" }] }));
  }

  function removeAttribute(index: number) {
    setForm((current) => ({ ...current, attributes: current.attributes.filter((_, i) => i !== index) }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const request: productApi.VariantRequest = {
        sku: form.sku,
        barcode: form.barcode || undefined,
        attributes: Object.fromEntries(
          form.attributes.filter((a) => a.key.trim() !== "").map((a) => [a.key.trim(), a.value]),
        ),
        purchasePrice: Number(form.purchasePrice || 0),
        sellingPrice: Number(form.sellingPrice || 0),
        reorderThreshold: Number(form.reorderThreshold || 0),
        hsnCode: form.hsnCode || undefined,
        gstRate: form.gstRate ? Number(form.gstRate) : undefined,
        uom: form.uom || undefined,
      };

      const product =
        isEdit && variant
          ? await productApi.updateVariant(productId, variant.id, request)
          : await productApi.addVariant(productId, request);

      toast.show({
        severity: "success",
        summary: isEdit ? "Variant updated" : "Variant added",
        detail: request.sku,
      });
      onSaved(product);
      onHide();
    } catch (err) {
      toast.show({
        severity: "error",
        summary: isEdit ? "Could not update variant" : "Could not add variant",
        detail: extractErrorMessage(err),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog visible={visible} onHide={onHide} title={isEdit ? "Edit Variant" : "Add Variant"} width={640}>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <Input id="variant-sku" label="SKU" value={form.sku} onChange={(e) => update({ sku: e.target.value })} required />
          <Input
            id="variant-barcode"
            label="Barcode (optional)"
            value={form.barcode}
            onChange={(e) => update({ barcode: e.target.value })}
          />
        </div>

        <div className="field">
          <label className="field-label">Attributes (e.g. Size, Color)</label>
          {form.attributes.map((attr, index) => (
            <div key={index} className="attribute-row">
              <input
                className="input"
                placeholder="Attribute (e.g. Size)"
                value={attr.key}
                onChange={(e) => updateAttribute(index, { key: e.target.value })}
              />
              <input
                className="input"
                placeholder="Value (e.g. M)"
                value={attr.value}
                onChange={(e) => updateAttribute(index, { value: e.target.value })}
              />
              <button
                type="button"
                className="attribute-remove"
                onClick={() => removeAttribute(index)}
                aria-label="Remove attribute"
                disabled={form.attributes.length <= 1}
              >
                <Icon name="x" size={14} />
              </button>
            </div>
          ))}
          <button type="button" className="attribute-add" onClick={addAttribute}>
            <Icon name="plus" size={13} /> Add attribute
          </button>
        </div>

        <div className="form-row">
          <Input
            id="variant-purchasePrice"
            label="Purchase price"
            type="number"
            step="0.01"
            min={0}
            value={form.purchasePrice}
            onChange={(e) => update({ purchasePrice: e.target.value })}
            required
          />
          <Input
            id="variant-sellingPrice"
            label="Selling price"
            type="number"
            step="0.01"
            min={0}
            value={form.sellingPrice}
            onChange={(e) => update({ sellingPrice: e.target.value })}
            required
          />
          <Input
            id="variant-reorderThreshold"
            label="Reorder threshold"
            type="number"
            step="1"
            min={0}
            value={form.reorderThreshold}
            onChange={(e) => update({ reorderThreshold: e.target.value })}
          />
        </div>

        <details className="variant-overrides">
          <summary>Override HSN / GST / UOM for this variant (optional)</summary>
          <div className="form-row">
            <Input
              id="variant-hsnCode"
              label="HSN code"
              value={form.hsnCode}
              onChange={(e) => update({ hsnCode: e.target.value })}
            />
            <Input
              id="variant-gstRate"
              label="GST rate (%)"
              type="number"
              step="0.01"
              min={0}
              max={100}
              value={form.gstRate}
              onChange={(e) => update({ gstRate: e.target.value })}
            />
            <Select
              id="variant-uom"
              label="Unit of measure"
              placeholder="Use product default"
              value={form.uom}
              onChange={(e) => update({ uom: e.target.value })}
              options={UOM_OPTIONS}
            />
          </div>
        </details>

        {isEdit && variant && <VariantImages productId={productId} variantId={variant.id} />}

        <div className="form-actions">
          <Button type="button" variant="outlined" severity="secondary" onClick={onHide}>
            Cancel
          </Button>
          <Button type="submit" loading={submitting}>
            {isEdit ? "Save Changes" : "Add Variant"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
