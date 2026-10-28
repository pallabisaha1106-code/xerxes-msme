import { useEffect, useState, type FormEvent } from "react";
import { Button, Dialog, Input, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import type { ProductResponse } from "../../api/productApi";
import type { CategoryResponse } from "../../api/categoryApi";

interface ProductFormState {
  name: string;
  brand: string;
  description: string;
  categoryId: string;
  defaultHsnCode: string;
  defaultGstRate: string;
  defaultUom: string;
  fabric: string;
  careInstructions: string;
  includedItems: string;
}

function formFromProduct(product: ProductResponse): ProductFormState {
  return {
    name: product.name,
    brand: product.brand ?? "",
    description: product.description ?? "",
    categoryId: product.categoryId ?? "",
    defaultHsnCode: product.defaultHsnCode ?? "",
    defaultGstRate: String(product.defaultGstRate),
    defaultUom: product.defaultUom,
    fabric: product.fabric ?? "",
    careInstructions: product.careInstructions ?? "",
    includedItems: product.includedItems ?? "",
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

export function EditProductDialog({
  visible,
  onHide,
  product,
  categories,
  onSaved,
}: {
  visible: boolean;
  onHide: () => void;
  product: ProductResponse | null;
  categories: CategoryResponse[];
  onSaved: (product: ProductResponse) => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState<ProductFormState | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible && product) {
      setForm(formFromProduct(product));
    }
  }, [visible, product]);

  function update(patch: Partial<ProductFormState>) {
    setForm((current) => (current ? { ...current, ...patch } : current));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!product || !form) return;
    setSubmitting(true);
    try {
      const request: productApi.ProductUpdateRequest = {
        name: form.name,
        brand: form.brand || undefined,
        description: form.description || undefined,
        categoryId: form.categoryId || undefined,
        defaultHsnCode: form.defaultHsnCode || undefined,
        defaultGstRate: Number(form.defaultGstRate || 0),
        defaultUom: form.defaultUom,
        fabric: form.fabric || undefined,
        careInstructions: form.careInstructions || undefined,
        includedItems: form.includedItems || undefined,
      };

      const updated = await productApi.updateProduct(product.id, request);
      toast.show({ severity: "success", summary: "Product updated", detail: updated.name });
      onSaved(updated);
      onHide();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update product", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  if (!form) return null;

  return (
    <Dialog visible={visible} onHide={onHide} title="Edit Product" width={640}>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <Input id="edit-name" label="Product name" value={form.name} onChange={(e) => update({ name: e.target.value })} required />
          <Input id="edit-brand" label="Brand (optional)" value={form.brand} onChange={(e) => update({ brand: e.target.value })} />
        </div>

        <Input
          id="edit-description"
          label="Description (optional)"
          value={form.description}
          onChange={(e) => update({ description: e.target.value })}
        />

        <div className="form-row">
          <Select
            id="edit-category"
            label="Category (optional)"
            placeholder="Uncategorized"
            value={form.categoryId}
            onChange={(e) => update({ categoryId: e.target.value })}
            options={categories.map((c) => ({ label: c.name, value: c.id }))}
          />
          <Select
            id="edit-defaultUom"
            label="Default unit of measure"
            value={form.defaultUom}
            onChange={(e) => update({ defaultUom: e.target.value })}
            options={UOM_OPTIONS}
          />
        </div>

        <div className="form-row">
          <Input
            id="edit-defaultHsnCode"
            label="Default HSN code (optional)"
            value={form.defaultHsnCode}
            onChange={(e) => update({ defaultHsnCode: e.target.value })}
          />
          <Input
            id="edit-defaultGstRate"
            label="Default GST rate (%)"
            type="number"
            step="0.01"
            min={0}
            max={100}
            value={form.defaultGstRate}
            onChange={(e) => update({ defaultGstRate: e.target.value })}
            required
          />
        </div>

        <div className="form-row">
          <Input id="edit-fabric" label="Fabric (optional)" value={form.fabric} onChange={(e) => update({ fabric: e.target.value })} />
          <Input
            id="edit-careInstructions"
            label="Care instructions (optional)"
            value={form.careInstructions}
            onChange={(e) => update({ careInstructions: e.target.value })}
          />
        </div>

        <Input
          id="edit-includedItems"
          label="Included items (optional)"
          hint="e.g. 1 top, 1 dupatta, 1 bottom"
          value={form.includedItems}
          onChange={(e) => update({ includedItems: e.target.value })}
        />

        <div className="form-actions">
          <Button type="button" variant="outlined" severity="secondary" onClick={onHide}>
            Cancel
          </Button>
          <Button type="submit" loading={submitting}>
            Save Changes
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
