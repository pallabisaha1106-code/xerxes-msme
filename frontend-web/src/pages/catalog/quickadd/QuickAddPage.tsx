import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, useToast, type SelectOption } from "../../../ui";
import { extractErrorMessage } from "../../../api/client";
import { listCategories } from "../../../api/categoryApi";
import { getProductImageRules, type ProductImageRules } from "../../../api/productApi";
import { getProductCapacity, type ProductCapacity } from "../../../api/quickAddApi";
import type { CategoryOption } from "./fileNameParser";
import { NEW_CATEGORY_PREFIX, applyValues, usableDrafts, type ApplyValues, type Draft } from "./draft";
import { makeDraft } from "./makeDraft";
import { DropZone } from "./DropZone";
import { DraftCard } from "./DraftCard";
import { ApplyToAll } from "./ApplyToAll";
import { ConfirmBar } from "./ConfirmBar";
import { useCreateBatch } from "./useCreateBatch";
import "./quickadd.css";

/** Most photos in one batch: keeps the page quick and the review manageable. */
const MAX_BATCH = 30;

/**
 * "Add products from photos": drop a set of photos, check the draft cards, confirm. Nothing is saved until the
 * vendor confirms; drafts live in this page's memory only.
 */
export function QuickAddPage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [rules, setRules] = useState<ProductImageRules | null>(null);
  const [capacity, setCapacity] = useState<ProductCapacity | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Draft[]>([]);
  /** Categories the vendor chose to create from file names; they are created at confirm time, not before. */
  const [newCategories, setNewCategories] = useState<string[]>([]);
  const [reading, setReading] = useState(false);
  const unnamedCount = useRef(0);
  const draftsRef = useRef<Draft[]>([]);
  draftsRef.current = drafts;

  const batch = useCreateBatch({
    onCardDone: (id) => {
      const done = draftsRef.current.find((draft) => draft.id === id);
      if (done) URL.revokeObjectURL(done.previewUrl);
      setDrafts((current) => current.filter((draft) => draft.id !== id));
    },
    // The new categories are real now: list them, and point the cards that chose them at the real ids.
    onCategoriesCreated: (created) => {
      setCategories((current) => [...current, ...created]);
      setNewCategories((current) => current.filter((name) => !created.some((c) => c.name.toLowerCase() === name.toLowerCase())));
      setDrafts((current) =>
        current.map((draft) => {
          const made = created.find((c) => draft.categoryId.toLowerCase() === (NEW_CATEGORY_PREFIX + c.name).toLowerCase());
          return made ? { ...draft, categoryId: made.id } : draft;
        }),
      );
    },
    onSettled: () => void getProductCapacity().then(setCapacity),
  });
  const { creating } = batch;

  useEffect(() => {
    Promise.all([listCategories(), getProductImageRules()])
      .then(([categoryList, imageRules]) => {
        setCategories(categoryList.map(({ id, name }) => ({ id, name })));
        setRules(imageRules);
      })
      .catch((error) => setLoadError(extractErrorMessage(error)));
    // Optional extra: the page works without it, so it never holds the page up.
    getProductCapacity().then(setCapacity);
  }, []);

  // Free the photo previews when leaving the page.
  useEffect(() => () => draftsRef.current.forEach((draft) => URL.revokeObjectURL(draft.previewUrl)), []);

  const categoryOptions: SelectOption[] = useMemo(
    () => [
      ...[...categories].sort((a, b) => a.name.localeCompare(b.name)).map((c) => ({ value: c.id, label: c.name })),
      ...newCategories.map((name) => ({ value: NEW_CATEGORY_PREFIX + name, label: `${name} (new)` })),
    ],
    [categories, newCategories],
  );

  const addFiles = async (files: File[]) => {
    if (!rules) return;
    const room = MAX_BATCH - drafts.length;
    if (files.length > room) toast.show({ severity: "warning", summary: `Only ${MAX_BATCH} photos at a time`, detail: `${Math.max(room, 0)} added, the rest were left out.` });
    setReading(true);
    for (const file of files.slice(0, Math.max(room, 0))) {
      const draft = await makeDraft(file, rules, categories);
      if (draft.file && draft.name === "") draft.name = `Product ${++unnamedCount.current}`;
      const pending = newCategories.find((name) => name.toLowerCase() === draft.missingCategory?.toLowerCase());
      if (pending && draft.categoryId === "") draft.categoryId = NEW_CATEGORY_PREFIX + pending;
      setDrafts((current) => [...current, draft]);
    }
    setReading(false);
  };

  const changeDraft = (id: string, patch: Partial<Draft>) =>
    setDrafts((current) => current.map((draft) => (draft.id === id ? { ...draft, ...patch } : draft)));

  const removeDraft = (id: string) => {
    const gone = drafts.find((draft) => draft.id === id);
    if (gone) URL.revokeObjectURL(gone.previewUrl);
    setDrafts((current) => current.filter((draft) => draft.id !== id));
  };

  const discardAll = () => {
    drafts.forEach((draft) => URL.revokeObjectURL(draft.previewUrl));
    setDrafts([]);
    setNewCategories([]);
  };

  /** Picks "create this category" for every draft whose file name named it. */
  const useMissingCategory = (name: string) => {
    const existing = newCategories.find((n) => n.toLowerCase() === name.toLowerCase()) ?? name;
    if (!newCategories.includes(existing)) setNewCategories((current) => [...current, existing]);
    setDrafts((current) =>
      current.map((draft) =>
        draft.categoryId === "" && draft.missingCategory?.toLowerCase() === name.toLowerCase()
          ? { ...draft, categoryId: NEW_CATEGORY_PREFIX + existing, categoryNote: "created" }
          : draft,
      ),
    );
  };

  const applyToAll = (values: ApplyValues) => setDrafts((current) => current.map((draft) => applyValues(draft, values)));

  const usable = usableDrafts(drafts);
  const failed = usable.filter((draft) => batch.states[draft.id]?.status === "failed").length;
  // A card that failed after its product was saved needs no new slot when retried.
  const slotsNeeded = usable.filter((draft) => !batch.hasProduct(draft.id)).length;

  if (loadError) {
    return (
      <div className="qa-page">
        <Card>
          <p className="qa-problem">Couldn't open this page: {loadError}</p>
          <Button variant="outlined" onClick={() => navigate("/products")}>Back to products</Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="qa-page">
      <header className="qa-header">
        <div>
          <h1>Add products from photos</h1>
          <p className="qa-muted">Drop photos, check each product, and confirm. Prices can stay blank and stock starts at zero.</p>
        </div>
        <Button variant="text" severity="secondary" onClick={() => navigate("/products")}>
          Back to products
        </Button>
      </header>

      {usable.length === 0 && batch.createdCount > 0 ? (
        <Card>
          <h2 className="qa-done-title">
            {batch.createdCount} {batch.createdCount === 1 ? "product" : "products"} created
          </h2>
          <p className="qa-muted">They are in your catalogue now. Add prices and stock any time from the products page.</p>
          {drafts.length > 0 && (
            <p className="qa-muted">
              {drafts.length} {drafts.length === 1 ? "photo was" : "photos were"} skipped because {drafts.length === 1 ? "it" : "they"} couldn't be used.
            </p>
          )}
          <div className="qa-confirm-actions">
            <Button onClick={() => navigate("/products")}>View products</Button>
            <Button
              variant="outlined"
              onClick={() => {
                discardAll();
                batch.resetCount();
              }}
            >
              Add more photos
            </Button>
          </div>
        </Card>
      ) : drafts.length === 0 ? (
        <DropZone onFiles={addFiles} busy={reading || rules === null} busyText={rules === null ? "Getting ready..." : undefined} />
      ) : (
        <>
          {batch.createdCount > 0 && !creating && (
            <p className="qa-banner">
              {batch.createdCount} created. {failed} could not be finished: see the messages below, then try again.
            </p>
          )}
          <ApplyToAll categoryOptions={categoryOptions} onApply={applyToAll} disabled={creating} />
          <div className="qa-list">
            {drafts.map((draft) => (
              <DraftCard
                key={draft.id}
                draft={draft}
                categoryOptions={categoryOptions}
                disabled={creating}
                state={batch.states[draft.id]}
                onChange={(patch) => changeDraft(draft.id, patch)}
                onRemove={() => removeDraft(draft.id)}
                onCreateCategory={useMissingCategory}
              />
            ))}
          </div>
          {drafts.length < MAX_BATCH && <DropZone onFiles={addFiles} busy={reading || creating} compact />}
          <ConfirmBar
            ready={usable.length}
            failed={failed}
            slotsNeeded={slotsNeeded}
            skipped={drafts.length - usable.length}
            withoutCategory={usable.filter((draft) => draft.categoryId === "").length}
            withoutPrice={usable.filter((draft) => !(Number(draft.price) > 0)).length}
            capacity={capacity}
            creating={creating}
            onDiscard={discardAll}
            onCreate={() => void batch.start(drafts)}
          />
        </>
      )}
    </div>
  );
}
