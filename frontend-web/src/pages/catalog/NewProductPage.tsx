import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Icon, Input, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as categoryApi from "../../api/categoryApi";
import * as productApi from "../../api/productApi";
import type { CategoryResponse } from "../../api/categoryApi";
import "./new-product.css";

interface AttributeRow {
  key: string;
  value: string;
}

interface VariantForm {
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

function blankVariant(): VariantForm {
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

const UOM_OPTIONS = [
  { label: "Pieces (PCS)", value: "PCS" },
  { label: "Set", value: "SET" },
  { label: "Pair", value: "PAIR" },
  { label: "Box", value: "BOX" },
  { label: "Kilogram (KG)", value: "KG" },
  { label: "Metre (MTR)", value: "MTR" },
];

export function NewProductPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const [name, setName] = useState("");
  const [brand, setBrand] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [defaultHsnCode, setDefaultHsnCode] = useState("");
  const [defaultGstRate, setDefaultGstRate] = useState("");
  const [defaultUom, setDefaultUom] = useState("PCS");
  const [fabric, setFabric] = useState("");
  const [careInstructions, setCareInstructions] = useState("");
  const [includedItems, setIncludedItems] = useState("");
  const [variants, setVariants] = useState<VariantForm[]>([blankVariant()]);

  useEffect(() => {
    categoryApi
      .listCategories()
      .then(setCategories)
      .catch((err) => toast.show({ severity: "error", summary: "Could not load categories", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function updateVariant(index: number, patch: Partial<VariantForm>) {
    setVariants((current) => current.map((v, i) => (i === index ? { ...v, ...patch } : v)));
  }

  function addVariant() {
    setVariants((current) => [...current, blankVariant()]);
  }

  function removeVariant(index: number) {
    setVariants((current) => current.filter((_, i) => i !== index));
  }

  function updateAttribute(variantIndex: number, attrIndex: number, patch: Partial<AttributeRow>) {
    setVariants((current) =>
      current.map((v, i) =>
        i === variantIndex
          ? { ...v, attributes: v.attributes.map((a, j) => (j === attrIndex ? { ...a, ...patch } : a)) }
          : v,
      ),
    );
  }

  function addAttribute(variantIndex: number) {
    setVariants((current) =>
      current.map((v, i) => (i === variantIndex ? { ...v, attributes: [...v.attributes, { key: "", value: "" }] } : v)),
    );
  }

  function removeAttribute(variantIndex: number, attrIndex: number) {
    setVariants((current) =>
      current.map((v, i) =>
        i === variantIndex ? { ...v, attributes: v.attributes.filter((_, j) => j !== attrIndex) } : v,
      ),
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const request: productApi.ProductRequest = {
        name,
        brand: brand || undefined,
        description: description || undefined,
        categoryId: categoryId || undefined,
        defaultHsnCode: defaultHsnCode || undefined,
        defaultGstRate: Number(defaultGstRate || 0),
        defaultUom,
        fabric: fabric || undefined,
        careInstructions: careInstructions || undefined,
        includedItems: includedItems || undefined,
        variants: variants.map((v) => ({
          sku: v.sku,
          barcode: v.barcode || undefined,
          attributes: Object.fromEntries(
            v.attributes.filter((a) => a.key.trim() !== "").map((a) => [a.key.trim(), a.value]),
          ),
          purchasePrice: Number(v.purchasePrice || 0),
          sellingPrice: Number(v.sellingPrice || 0),
          reorderThreshold: Number(v.reorderThreshold || 0),
          hsnCode: v.hsnCode || undefined,
          gstRate: v.gstRate ? Number(v.gstRate) : undefined,
          uom: v.uom || undefined,
        })),
      };

      const created = await productApi.createProduct(request);
      toast.show({ severity: "success", summary: "Product created", detail: created.name });
      navigate("/products");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create product", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <Card title="Product details">
        <div className="form-row">
          <Input id="name" label="Product name" value={name} onChange={(e) => setName(e.target.value)} required />
          <Input id="brand" label="Brand (optional)" value={brand} onChange={(e) => setBrand(e.target.value)} />
        </div>

        <Input
          id="description"
          label="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <div className="form-row">
          <Select
            id="category"
            label="Category (optional)"
            placeholder="Uncategorized"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            options={categories.map((c) => ({ label: c.name, value: c.id }))}
          />
          <Select
            id="defaultUom"
            label="Default unit of measure"
            value={defaultUom}
            onChange={(e) => setDefaultUom(e.target.value)}
            options={UOM_OPTIONS}
          />
        </div>

        <div className="form-row">
          <Input
            id="defaultHsnCode"
            label="Default HSN code (optional)"
            value={defaultHsnCode}
            onChange={(e) => setDefaultHsnCode(e.target.value)}
          />
          <Input
            id="defaultGstRate"
            label="Default GST rate (%)"
            type="number"
            step="0.01"
            min={0}
            max={100}
            value={defaultGstRate}
            onChange={(e) => setDefaultGstRate(e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <Input
            id="fabric"
            label="Fabric (optional)"
            value={fabric}
            onChange={(e) => setFabric(e.target.value)}
          />
          <Input
            id="careInstructions"
            label="Care instructions (optional)"
            value={careInstructions}
            onChange={(e) => setCareInstructions(e.target.value)}
          />
        </div>

        <Input
          id="includedItems"
          label="Included items (optional)"
          hint="e.g. 1 top, 1 dupatta, 1 bottom"
          value={includedItems}
          onChange={(e) => setIncludedItems(e.target.value)}
        />
      </Card>

      <Card
        title="Variants"
        className="variants-card"
        actions={
          <Button type="button" icon="plus" size="sm" variant="outlined" onClick={addVariant}>
            Add Variant
          </Button>
        }
      >
        {variants.map((variant, index) => (
          <div key={index} className="variant-block">
            <div className="variant-block-header">
              <h3>Variant {index + 1}</h3>
              {variants.length > 1 && (
                <button
                  type="button"
                  className="variant-remove"
                  onClick={() => removeVariant(index)}
                  aria-label="Remove variant"
                >
                  <Icon name="trash" size={15} />
                </button>
              )}
            </div>

            <div className="form-row">
              <Input
                id={`sku-${index}`}
                label="SKU"
                value={variant.sku}
                onChange={(e) => updateVariant(index, { sku: e.target.value })}
                required
              />
              <Input
                id={`barcode-${index}`}
                label="Barcode (optional)"
                value={variant.barcode}
                onChange={(e) => updateVariant(index, { barcode: e.target.value })}
              />
            </div>

            <div className="field">
              <label className="field-label">Attributes (e.g. Size, Color)</label>
              {variant.attributes.map((attr, attrIndex) => (
                <div key={attrIndex} className="attribute-row">
                  <input
                    className="input"
                    placeholder="Attribute (e.g. Size)"
                    value={attr.key}
                    onChange={(e) => updateAttribute(index, attrIndex, { key: e.target.value })}
                  />
                  <input
                    className="input"
                    placeholder="Value (e.g. M)"
                    value={attr.value}
                    onChange={(e) => updateAttribute(index, attrIndex, { value: e.target.value })}
                  />
                  <button
                    type="button"
                    className="attribute-remove"
                    onClick={() => removeAttribute(index, attrIndex)}
                    aria-label="Remove attribute"
                    disabled={variant.attributes.length <= 1}
                  >
                    <Icon name="x" size={14} />
                  </button>
                </div>
              ))}
              <button type="button" className="attribute-add" onClick={() => addAttribute(index)}>
                <Icon name="plus" size={13} /> Add attribute
              </button>
            </div>

            <div className="form-row">
              <Input
                id={`purchasePrice-${index}`}
                label="Purchase price"
                type="number"
                step="0.01"
                min={0}
                value={variant.purchasePrice}
                onChange={(e) => updateVariant(index, { purchasePrice: e.target.value })}
                required
              />
              <Input
                id={`sellingPrice-${index}`}
                label="Selling price"
                type="number"
                step="0.01"
                min={0}
                value={variant.sellingPrice}
                onChange={(e) => updateVariant(index, { sellingPrice: e.target.value })}
                required
              />
              <Input
                id={`reorderThreshold-${index}`}
                label="Reorder threshold"
                type="number"
                step="1"
                min={0}
                value={variant.reorderThreshold}
                onChange={(e) => updateVariant(index, { reorderThreshold: e.target.value })}
              />
            </div>

            <details className="variant-overrides">
              <summary>Override HSN / GST / UOM for this variant (optional)</summary>
              <div className="form-row">
                <Input
                  id={`hsnCode-${index}`}
                  label="HSN code"
                  value={variant.hsnCode}
                  onChange={(e) => updateVariant(index, { hsnCode: e.target.value })}
                />
                <Input
                  id={`gstRate-${index}`}
                  label="GST rate (%)"
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  value={variant.gstRate}
                  onChange={(e) => updateVariant(index, { gstRate: e.target.value })}
                />
                <Select
                  id={`uom-${index}`}
                  label="Unit of measure"
                  placeholder="Use product default"
                  value={variant.uom}
                  onChange={(e) => updateVariant(index, { uom: e.target.value })}
                  options={UOM_OPTIONS}
                />
              </div>
            </details>
          </div>
        ))}
      </Card>

      <div className="form-actions">
        <Button type="button" variant="outlined" severity="secondary" onClick={() => navigate("/products")}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Create Product
        </Button>
      </div>
    </form>
  );
}
