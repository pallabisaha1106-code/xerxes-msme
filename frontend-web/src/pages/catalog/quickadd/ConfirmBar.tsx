import { Button } from "../../../ui";
import type { ProductCapacity } from "../../../api/quickAddApi";

interface ConfirmBarProps {
  ready: number;
  /** Cards that failed in the last run: the button then reads "Retry". */
  failed: number;
  /** Cards that still need a new product slot (a retried card that already has its product needs none). */
  slotsNeeded: number;
  skipped: number;
  withoutCategory: number;
  withoutPrice: number;
  capacity: ProductCapacity | null;
  creating: boolean;
  onDiscard: () => void;
  onCreate: () => void;
}

const plural = (count: number, word: string) => `${count} ${word}${count === 1 ? "" : "s"}`;

/** The sticky footer: what will be created, anything worth a second look, and the confirm button. */
export function ConfirmBar({ ready, failed, slotsNeeded, skipped, withoutCategory, withoutPrice, capacity, creating, onDiscard, onCreate }: ConfirmBarProps) {
  const overLimit = capacity !== null && slotsNeeded > capacity.remaining;
  const notes: string[] = [];
  if (skipped > 0) notes.push(`${plural(skipped, "photo")} will be skipped`);
  if (withoutCategory > 0) notes.push(`${withoutCategory} without a category`);
  if (withoutPrice > 0) notes.push(`${withoutPrice} without a price`);

  return (
    <div className="qa-confirm">
      <div className="qa-confirm-text">
        <strong>{ready === 0 ? "No products yet" : `${plural(ready, "product")} ready to create`}</strong>
        {capacity !== null && (
          <span className={overLimit ? "qa-over" : "qa-muted"}>
            {overLimit
              ? `Your plan has room for ${capacity.remaining} more; remove ${slotsNeeded - capacity.remaining} to continue.`
              : `${capacity.remaining} of ${capacity.limit} product slots left`}
          </span>
        )}
        {notes.length > 0 && <span className="qa-muted">{notes.join(" · ")}. You can fill these in later.</span>}
      </div>
      <div className="qa-confirm-actions">
        <Button variant="text" severity="secondary" disabled={creating} onClick={onDiscard}>
          Discard all
        </Button>
        <Button icon="check" loading={creating} disabled={ready === 0 || overLimit} onClick={onCreate}>
          {ready === 0 ? "Create products" : `${failed > 0 ? "Retry" : "Create"} ${plural(ready, "product")}`}
        </Button>
      </div>
    </div>
  );
}
