import { Icon, Input, Select, Tag, type SelectOption } from "../../../ui";
import type { Draft } from "./draft";
import type { CardState } from "./useCreateBatch";
import { SizeChips } from "./SizeChips";

interface DraftCardProps {
  draft: Draft;
  categoryOptions: SelectOption[];
  onChange: (patch: Partial<Draft>) => void;
  onRemove: () => void;
  onCreateCategory: (name: string) => void;
  /** Lock the fields while the batch is being created. */
  disabled: boolean;
  /** Where this card is in "Create": waiting, creating, or failed with a reason. */
  state?: CardState;
}

/** One photo turned into a product the vendor can still edit before confirming. */
export function DraftCard({ draft, categoryOptions, onChange, onRemove, onCreateCategory, disabled, state }: DraftCardProps) {
  const id = `qa-${draft.id}`;
  const hasSizes = draft.sizes.length > 0;

  return (
    <article className={["qa-card", draft.problem ? "qa-card-problem" : ""].filter(Boolean).join(" ")}>
      <div className="qa-photo">
        <img src={draft.previewUrl} alt={draft.fileName} />
        {draft.cropped && <Tag severity="info">Cropped to fit</Tag>}
      </div>

      <div className="qa-fields">
        <div className="qa-card-head">
          <span className="qa-file" title={draft.fileName}>
            {draft.fileName}
          </span>
          <button type="button" className="qa-remove" aria-label={`Remove ${draft.fileName}`} disabled={disabled} onClick={onRemove}>
            <Icon name="x" size={16} />
          </button>
        </div>

        {state?.status === "waiting" && <Tag severity="secondary">Waiting</Tag>}
        {state?.status === "working" && <Tag severity="info">Creating...</Tag>}
        {state?.status === "failed" && <p className="qa-problem">{state.message ?? "This product could not be created."}</p>}

        {draft.problem ? (
          <p className="qa-problem">{draft.problem}. This photo will be skipped; remove it or drop a larger one.</p>
        ) : (
          <>
            <Input
              id={`${id}-name`}
              label="Name"
              value={draft.name}
              maxLength={120}
              disabled={disabled}
              onChange={(event) => onChange({ name: event.target.value })}
            />

            <div className="qa-row">
              <div className="qa-grow">
                <Select
                  id={`${id}-category`}
                  label="Category"
                  value={draft.categoryId}
                  options={categoryOptions}
                  placeholder="No category"
                  disabled={disabled}
                  onChange={(event) => onChange({ categoryId: event.target.value, categoryNote: null })}
                />
                {draft.missingCategory && draft.categoryId === "" && (
                  <button type="button" className="qa-link" disabled={disabled} onClick={() => onCreateCategory(draft.missingCategory!)}>
                    Create "{draft.missingCategory}" and use it
                  </button>
                )}
              </div>
              <Input
                id={`${id}-price`}
                label="Price (₹)"
                type="number"
                inputMode="decimal"
                min="0"
                step="0.01"
                value={draft.price}
                placeholder="Not set"
                disabled={disabled}
                onChange={(event) => onChange({ price: event.target.value })}
              />
              {!hasSizes && (
                <Input
                  id={`${id}-stock`}
                  label="Stock"
                  type="number"
                  inputMode="numeric"
                  min="0"
                  step="1"
                  value={draft.stock}
                  disabled={disabled}
                  onChange={(event) => onChange({ stock: event.target.value })}
                />
              )}
            </div>

            <div className="qa-sizes">
              <span className="field-label">Sizes (optional)</span>
              <SizeChips
                sizes={draft.sizes}
                onChange={(sizes) => (disabled ? undefined : onChange({ sizes }))}
              />
              {hasSizes && (
                <div className="qa-size-stock">
                  {draft.sizes.map((size) => (
                    <label key={size} className="qa-size-stock-item">
                      <span>{size}</span>
                      <input
                        className="input"
                        type="number"
                        inputMode="numeric"
                        min="0"
                        step="1"
                        aria-label={`Stock for size ${size}`}
                        value={draft.sizeStock[size] ?? ""}
                        placeholder="0"
                        disabled={disabled}
                        onChange={(event) => onChange({ sizeStock: { ...draft.sizeStock, [size]: event.target.value } })}
                      />
                    </label>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </article>
  );
}
