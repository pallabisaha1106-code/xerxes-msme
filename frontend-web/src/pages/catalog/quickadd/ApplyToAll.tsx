import { useState } from "react";
import { Button, Input, Select, type SelectOption } from "../../../ui";
import type { ApplyValues } from "./draft";
import { SizeChips } from "./SizeChips";

interface ApplyToAllProps {
  categoryOptions: SelectOption[];
  /** Receives only the fields the vendor filled in, plus the stock to give every size. */
  onApply: (values: ApplyValues) => void;
  disabled: boolean;
}

/** Sets the same category, price, stock and sizes on every photo at once, so a batch of similar items needs one pass instead of thirty. */
export function ApplyToAll({ categoryOptions, onApply, disabled }: ApplyToAllProps) {
  const [categoryId, setCategoryId] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [sizes, setSizes] = useState<string[]>([]);

  const empty = !categoryId && !price && !stock && sizes.length === 0;

  const apply = () => {
    const values: ApplyValues = {};
    if (categoryId) values.categoryId = categoryId;
    if (price) values.price = price;
    if (stock) values.stock = stock;
    if (sizes.length > 0) values.sizes = sizes;
    onApply(values);
    setCategoryId("");
    setPrice("");
    setStock("");
    setSizes([]);
  };

  return (
    <section className="qa-apply" aria-label="Apply to all">
      <h2>Same for all photos</h2>
      <p className="qa-muted">Fill in only what is the same for every product, then apply. You can still change any card afterwards.</p>
      <div className="qa-row">
        <div className="qa-grow">
          <Select id="qa-all-category" label="Category" value={categoryId} options={categoryOptions} placeholder="Leave as is" disabled={disabled} onChange={(e) => setCategoryId(e.target.value)} />
        </div>
        <Input id="qa-all-price" label="Price (₹)" type="number" inputMode="decimal" min="0" step="0.01" value={price} disabled={disabled} onChange={(e) => setPrice(e.target.value)} />
        <Input id="qa-all-stock" label={sizes.length > 0 ? "Stock per size" : "Stock"} type="number" inputMode="numeric" min="0" step="1" value={stock} disabled={disabled} onChange={(e) => setStock(e.target.value)} />
      </div>
      <div className="qa-sizes">
        <span className="field-label">Sizes</span>
        <SizeChips sizes={sizes} onChange={setSizes} />
      </div>
      <Button variant="outlined" disabled={disabled || empty} onClick={apply}>
        Apply to all
      </Button>
    </section>
  );
}
