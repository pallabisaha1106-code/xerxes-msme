import { Button, Dialog } from "../../../ui";
import { resolveImagePath } from "../../../lib/mediaUrl";
import type { StarterCatalogGroup } from "../../../api/starterCatalogApi";

/**
 * The dialog that opens from a category tile: every product in the group as a tile with a checkbox, thumbnail and name.
 * A product the vendor already has can't be picked again; once the plan's limit is reached, the unpicked ones can't be
 * picked either.
 */
export function CategoryProductsDialog({
  group,
  selectedIds,
  ownedIds,
  atCapacity,
  onToggle,
  onClose,
}: {
  group: StarterCatalogGroup | null;
  selectedIds: Set<string>;
  ownedIds: Set<string>;
  atCapacity: boolean;
  onToggle: (id: string) => void;
  onClose: () => void;
}) {
  return (
    <Dialog
      visible={group !== null}
      onHide={onClose}
      title={group?.name ?? ""}
      width={760}
      footer={<Button onClick={onClose}>Done</Button>}
    >
      <p className="qa-muted">Select any products you stock.</p>
      <div className="sc-products">
        {group?.products.map((product) => {
          const owned = ownedIds.has(product.id);
          const checked = selectedIds.has(product.id);
          return (
            <label key={product.id} className={`sc-product${checked ? " selected" : ""}${owned ? " owned" : ""}`}>
              <input
                type="checkbox"
                checked={checked}
                disabled={owned || (!checked && atCapacity)}
                onChange={() => onToggle(product.id)}
              />
              {product.imageUrl && <img className="sc-thumb" src={resolveImagePath(product.imageUrl)} alt="" />}
              <span className="sc-product-name">{product.name}</span>
              {owned && <span className="sc-owned">Already in your catalogue</span>}
            </label>
          );
        })}
      </div>
    </Dialog>
  );
}
