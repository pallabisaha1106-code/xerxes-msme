import { useEffect, useState } from "react";
import { Button, Card, Dialog, ImagePicker, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import type { ImageRules } from "../../lib/imageChecks";
import { extractErrorMessage } from "../../api/client";
import { resolveImagePath } from "../../lib/mediaUrl";
import * as adminAdvertisementApi from "../../api/adminAdvertisementApi";
import type { AdminAdvertisement, AdvertisementSlot } from "../../api/adminAdvertisementApi";

const SLOT_OPTIONS: { label: string; value: AdvertisementSlot }[] = [
  { label: "Top Carousel", value: "TOP_CAROUSEL" },
  { label: "Left Rail", value: "LEFT_RAIL" },
  { label: "Right Rail", value: "RIGHT_RAIL" },
];

// Suggested sizes match how the marketplace lays out each slot (top banner ~1376px wide, side rails 160px wide, shown at 2x for sharp screens).
const SLOT_IMAGE_GUIDE: Record<AdvertisementSlot, { hint: string; rules: ImageRules }> = {
  TOP_CAROUSEL: {
    hint: "Wide banner - about 1600×340px (roughly 4.7:1). Minimum 1200×250px.",
    rules: { minWidth: 1200, minHeight: 250, maxBytes: 10 * 1024 * 1024 },
  },
  LEFT_RAIL: {
    hint: "Tall side banner - about 320×800px (2:5). Minimum 300×600px.",
    rules: { minWidth: 300, minHeight: 600, maxBytes: 10 * 1024 * 1024 },
  },
  RIGHT_RAIL: {
    hint: "Tall side banner - about 320×800px (2:5). Minimum 300×600px.",
    rules: { minWidth: 300, minHeight: 600, maxBytes: 10 * 1024 * 1024 },
  },
};

interface FormState {
  linkUrl: string;
  altText: string;
  headline: string;
  description: string;
  startsAt: string;
  endsAt: string;
}

const EMPTY_FORM: FormState = { linkUrl: "", altText: "", headline: "", description: "", startsAt: "", endsAt: "" };

/** datetime-local inputs work in the browser's local time with no timezone - convert to/from the backend's ISO instants at the edges. */
function toDatetimeLocalValue(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formFromAdvertisement(ad: AdminAdvertisement): FormState {
  return {
    linkUrl: ad.linkUrl ?? "",
    altText: ad.altText,
    headline: ad.headline ?? "",
    description: ad.description ?? "",
    startsAt: toDatetimeLocalValue(ad.startsAt),
    endsAt: toDatetimeLocalValue(ad.endsAt),
  };
}

export function AdminAdvertisementsPage() {
  const toast = useToast();
  const [slot, setSlot] = useState<AdvertisementSlot>("TOP_CAROUSEL");
  const [ads, setAds] = useState<AdminAdvertisement[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AdminAdvertisement | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [pickerKey, setPickerKey] = useState(0);
  const [saving, setSaving] = useState(false);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminAdvertisement | null>(null);
  const [deleting, setDeleting] = useState(false);

  async function loadAds(forSlot: AdvertisementSlot) {
    setLoading(true);
    try {
      setAds(await adminAdvertisementApi.listAdvertisements(forSlot));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load advertisements", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAds(slot);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slot]);

  function openCreateDialog() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setPhotoFile(null);
    setPickerKey((k) => k + 1);
    setDialogOpen(true);
  }

  function openEditDialog(ad: AdminAdvertisement) {
    setEditing(ad);
    setForm(formFromAdvertisement(ad));
    setPhotoFile(null);
    setPickerKey((k) => k + 1);
    setDialogOpen(true);
  }

  function applyUpdatedAd(updated: AdminAdvertisement) {
    setEditing(updated);
    setAds((current) => {
      const exists = current.some((a) => a.id === updated.id);
      return exists ? current.map((a) => (a.id === updated.id ? updated : a)) : [updated, ...current];
    });
  }

  async function handleSave() {
    if (!editing && !photoFile) {
      toast.show({ severity: "error", summary: "Please choose an image to upload" });
      return;
    }
    setSaving(true);
    try {
      if (editing) {
        let updated = await adminAdvertisementApi.updateAdvertisement(editing.id, {
          linkUrl: form.linkUrl || undefined,
          altText: form.altText,
          headline: form.headline || undefined,
          description: form.description || undefined,
          startsAt: new Date(form.startsAt).toISOString(),
          endsAt: new Date(form.endsAt).toISOString(),
        });
        if (photoFile) {
          updated = await adminAdvertisementApi.uploadAdvertisementPhoto(editing.id, photoFile);
        }
        applyUpdatedAd(updated);
        setPhotoFile(null);
        toast.show({ severity: "success", summary: "Advertisement updated" });
      } else {
        const created = await adminAdvertisementApi.createAdvertisement({
          slot,
          linkUrl: form.linkUrl || undefined,
          altText: form.altText,
          headline: form.headline || undefined,
          description: form.description || undefined,
          startsAt: new Date(form.startsAt).toISOString(),
          endsAt: new Date(form.endsAt).toISOString(),
        });
        try {
          const withImage = await adminAdvertisementApi.uploadAdvertisementPhoto(created.id, photoFile as File);
          applyUpdatedAd(withImage);
          setPhotoFile(null);
          setDialogOpen(false);
          toast.show({ severity: "success", summary: "Advertisement created" });
        } catch (uploadErr) {
          applyUpdatedAd(created);
          toast.show({ severity: "error", summary: "Advertisement created but image upload failed - try again below", detail: extractErrorMessage(uploadErr) });
        }
      }
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save advertisement", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(ad: AdminAdvertisement) {
    setTogglingId(ad.id);
    try {
      const updated = ad.published
        ? await adminAdvertisementApi.unpublishAdvertisement(ad.id)
        : await adminAdvertisementApi.publishAdvertisement(ad.id);
      setAds((current) => current.map((a) => (a.id === updated.id ? updated : a)));
      toast.show({ severity: "success", summary: updated.published ? "Advertisement published" : "Advertisement unpublished" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update advertisement", detail: extractErrorMessage(err) });
    } finally {
      setTogglingId(null);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await adminAdvertisementApi.deleteAdvertisement(deleteTarget.id);
      setAds((current) => current.filter((a) => a.id !== deleteTarget.id));
      toast.show({ severity: "success", summary: "Advertisement deleted" });
      setDeleteTarget(null);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not delete advertisement", detail: extractErrorMessage(err) });
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<AdminAdvertisement>[] = [
    {
      field: "imageUrl",
      header: "",
      width: "4rem",
      body: (a) =>
        a.imageUrl ? (
          <img src={resolveImagePath(a.imageUrl)} alt="" width={48} height={48} style={{ objectFit: "cover", borderRadius: 4 }} />
        ) : (
          <span className="field-hint">No image</span>
        ),
    },
    {
      field: "headline",
      header: "Advertisement",
      body: (a) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => openEditDialog(a)}>
          {a.headline || a.altText}
        </Button>
      ),
    },
    {
      field: "startsAt",
      header: "Duration",
      body: (a) => `${new Date(a.startsAt).toLocaleDateString()} – ${new Date(a.endsAt).toLocaleDateString()}`,
    },
    {
      field: "published",
      header: "Status",
      body: (a) =>
        a.currentlyLive ? (
          <Tag severity="success">LIVE</Tag>
        ) : a.published ? (
          <Tag severity="warning">SCHEDULED</Tag>
        ) : (
          <Tag severity="secondary">DRAFT</Tag>
        ),
    },
    {
      field: "actions",
      header: "",
      body: (a) => (
        <div style={{ display: "flex", gap: "0.35rem" }}>
          <Button
            size="sm"
            variant="outlined"
            severity={a.published ? "secondary" : "success"}
            loading={togglingId === a.id}
            disabled={!a.imageUrl}
            onClick={() => handleToggle(a)}
          >
            {a.published ? "Unpublish" : "Publish"}
          </Button>
          <Button size="sm" variant="outlined" severity="danger" onClick={() => setDeleteTarget(a)}>
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <Card
        title="Advertisements"
        actions={
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Select id="ad-slot" value={slot} onChange={(e) => setSlot(e.target.value as AdvertisementSlot)} options={SLOT_OPTIONS} />
            <Button size="sm" onClick={openCreateDialog}>
              New Advertisement
            </Button>
          </div>
        }
      >
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Only one advertisement is shown live per slot at a time - publishing a new one takes over immediately,
          regardless of an older one's end date. Once an advertisement's end date passes (or none is published),
          that slot falls back to a default placeholder on the marketplace.
        </p>
        <Table data={ads} columns={columns} rowKey={(row) => row.id} loading={loading} emptyMessage="No advertisements yet for this slot." />
      </Card>

      <Dialog
        visible={dialogOpen}
        onHide={() => setDialogOpen(false)}
        title={editing ? "Edit Advertisement" : "New Advertisement"}
        width={560}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setDialogOpen(false)}>
              Close
            </Button>
            <Button loading={saving} onClick={handleSave}>
              {editing ? "Save Changes" : "Create"}
            </Button>
          </>
        }
      >
        <div>
          <label className="field-label" htmlFor="ad-image">
            Image {!editing?.imageUrl && "(required)"}
          </label>
          {editing?.imageUrl && !photoFile && (
            <img
              src={resolveImagePath(editing.imageUrl)}
              alt=""
              width={200}
              height={120}
              style={{ objectFit: "cover", borderRadius: 6, display: "block", marginBottom: "0.5rem" }}
            />
          )}
          <ImagePicker
            key={pickerKey}
            id="ad-image"
            rules={SLOT_IMAGE_GUIDE[editing?.slot ?? slot].rules}
            onChange={setPhotoFile}
          />
          <p className="field-hint">{SLOT_IMAGE_GUIDE[editing?.slot ?? slot].hint}</p>
          <p className="field-hint">Uploaded to cloud storage - shown on the marketplace once published.</p>
        </div>
        <Input
          id="ad-alt-text"
          label="Alt text (for accessibility)"
          value={form.altText}
          onChange={(e) => setForm((f) => ({ ...f, altText: e.target.value }))}
          required
        />
        <Input
          id="ad-headline"
          label="Headline (optional)"
          value={form.headline}
          onChange={(e) => setForm((f) => ({ ...f, headline: e.target.value }))}
        />
        <Input
          id="ad-description"
          label="Description (optional)"
          value={form.description}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
        />
        <Input
          id="ad-link"
          label="Link URL (optional)"
          placeholder="https://…"
          value={form.linkUrl}
          onChange={(e) => setForm((f) => ({ ...f, linkUrl: e.target.value }))}
        />
        <div className="form-row">
          <Input
            id="ad-starts-at"
            label="Starts"
            type="datetime-local"
            value={form.startsAt}
            onChange={(e) => setForm((f) => ({ ...f, startsAt: e.target.value }))}
            required
          />
          <Input
            id="ad-ends-at"
            label="Ends"
            type="datetime-local"
            value={form.endsAt}
            onChange={(e) => setForm((f) => ({ ...f, endsAt: e.target.value }))}
            required
          />
        </div>
      </Dialog>

      <Dialog
        visible={deleteTarget != null}
        onHide={() => setDeleteTarget(null)}
        title="Delete Advertisement"
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button severity="danger" loading={deleting} onClick={handleDelete}>
              Delete
            </Button>
          </>
        }
      >
        <p>
          Delete <strong>{deleteTarget?.headline || deleteTarget?.altText}</strong>? This can&rsquo;t be undone.
        </p>
      </Dialog>
    </>
  );
}
