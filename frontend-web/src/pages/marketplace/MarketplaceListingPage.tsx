import { useEffect, useState } from "react";
import { Button, Card, Icon, ImagePicker, Input, Select, Tag, useToast } from "../../ui";
import type { ImageRules } from "../../lib/imageChecks";
import { extractErrorMessage } from "../../api/client";
import * as marketplaceApi from "../../api/marketplaceListingApi";
import type {
  MarketplaceCategory,
  MarketplaceListing,
  MarketplaceOrderMode,
  UpdateMarketplaceListingRequest,
} from "../../api/marketplaceListingApi";
import { resolveImagePath } from "../../lib/mediaUrl";
import "./marketplace-listing.css";

const SHOP_PHOTO_GUIDELINES = [
  "Landscape framing, roughly 4:3 to 2:1 (wider than tall)",
  "At least 960×480 pixels",
  "A real photo of your storefront, not a supplier/stock image",
];

// Mirrors MarketplaceListingService's shop photo limits (960x480, 1.3-2.2 aspect); the server stays the source of truth.
const SHOP_PHOTO_RULES: ImageRules = {
  minWidth: 960,
  minHeight: 480,
  minAspect: 1.3,
  maxAspect: 2.2,
  aspectHint: "a landscape photo, roughly 4:3 to 2:1 (wider than tall)",
  maxBytes: 10 * 1024 * 1024,
};

const ORDER_MODE_OPTIONS = [
  { label: "WhatsApp enquiry", value: "WHATSAPP" },
  { label: "Real checkout (customer places a real order)", value: "CHECKOUT" },
];

function statusTag(status: MarketplaceListing["status"]) {
  if (status === "LIVE") return <Tag severity="success">LIVE</Tag>;
  if (status === "PENDING") return <Tag severity="warning">PENDING REVIEW</Tag>;
  return <Tag severity="secondary">DRAFT</Tag>;
}

interface FormState {
  categoryId: string;
  area: string;
  shopPhotoAlt: string;
  instagramUrl: string;
  facebookUrl: string;
  youtubeUrl: string;
  googleMapsUrl: string;
  whatsappUrl: string;
  websiteUrl: string;
  enquiryWhatsapp: string;
  orderMode: MarketplaceOrderMode;
}

function formFromListing(listing: MarketplaceListing): FormState {
  return {
    categoryId: listing.categoryId ?? "",
    area: listing.area ?? "",
    shopPhotoAlt: listing.shopPhotoAlt ?? "",
    instagramUrl: listing.instagramUrl ?? "",
    facebookUrl: listing.facebookUrl ?? "",
    youtubeUrl: listing.youtubeUrl ?? "",
    googleMapsUrl: listing.googleMapsUrl ?? "",
    whatsappUrl: listing.whatsappUrl ?? "",
    websiteUrl: listing.websiteUrl ?? "",
    enquiryWhatsapp: listing.enquiryWhatsapp ?? "",
    orderMode: listing.orderMode,
  };
}

export function MarketplaceListingPage() {
  const toast = useToast();

  const [categories, setCategories] = useState<MarketplaceCategory[]>([]);
  const [listing, setListing] = useState<MarketplaceListing | null>(null);
  const [form, setForm] = useState<FormState | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [pendingPhoto, setPendingPhoto] = useState<File | null>(null);
  const [pickerKey, setPickerKey] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [categoryList, currentListing] = await Promise.all([
          marketplaceApi.listMarketplaceCategories(),
          marketplaceApi.getMarketplaceListing(),
        ]);
        setCategories(categoryList);
        setListing(currentListing);
        setForm(formFromListing(currentListing));
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load marketplace listing", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function update(patch: Partial<FormState>) {
    setForm((current) => (current ? { ...current, ...patch } : current));
  }

  async function handleSave() {
    if (!form) return;
    setSaving(true);
    try {
      const request: UpdateMarketplaceListingRequest = {
        categoryId: form.categoryId || null,
        area: form.area || null,
        shopPhotoAlt: form.shopPhotoAlt || null,
        instagramUrl: form.instagramUrl || null,
        facebookUrl: form.facebookUrl || null,
        youtubeUrl: form.youtubeUrl || null,
        googleMapsUrl: form.googleMapsUrl || null,
        whatsappUrl: form.whatsappUrl || null,
        websiteUrl: form.websiteUrl || null,
        enquiryWhatsapp: form.enquiryWhatsapp || null,
        orderMode: form.orderMode,
      };
      const updated = await marketplaceApi.updateMarketplaceListing(request);
      setListing(updated);
      setForm(formFromListing(updated));
      toast.show({ severity: "success", summary: "Marketplace listing saved" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function handleUploadPhoto() {
    if (!pendingPhoto) return;

    setUploading(true);
    try {
      const updated = await marketplaceApi.uploadMarketplacePhoto(pendingPhoto);
      setListing(updated);
      setForm(formFromListing(updated));
      setPendingPhoto(null);
      setPickerKey((k) => k + 1);
      toast.show({ severity: "success", summary: "Shop photo uploaded" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not upload photo", detail: extractErrorMessage(err) });
    } finally {
      setUploading(false);
    }
  }

  async function handleRemovePhoto() {
    setUploading(true);
    try {
      const updated = await marketplaceApi.deleteMarketplacePhoto();
      setListing(updated);
      setForm(formFromListing(updated));
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not remove photo", detail: extractErrorMessage(err) });
    } finally {
      setUploading(false);
    }
  }

  async function handleSubmit() {
    setSubmitting(true);
    try {
      const updated = await marketplaceApi.submitMarketplaceListing();
      setListing(updated);
      setForm(formFromListing(updated));
      toast.show({ severity: "success", summary: "Submitted for review" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not submit", detail: extractErrorMessage(err) });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading || !form || !listing) {
    return (
      <Card title="Marketplace Listing">
        <p>Loading&hellip;</p>
      </Card>
    );
  }

  return (
    <Card
      title="Marketplace Listing"
      actions={
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          {statusTag(listing.status)}
          <Button variant="outlined" severity="secondary" loading={saving} onClick={handleSave}>
            Save
          </Button>
          {listing.status === "DRAFT" && (
            <Button severity="success" loading={submitting} onClick={handleSubmit}>
              Submit for Review
            </Button>
          )}
        </div>
      }
    >
      {listing.status === "PENDING" && (
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Your listing is awaiting Super Admin review. You can keep editing it in the meantime.
        </p>
      )}
      {listing.status === "LIVE" && (
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Your listing is live on the marketplace. Edits here are saved immediately.
        </p>
      )}
      {listing.rejectionReason && (
        <div className="marketplace-rejection-note">
          <strong>Changes requested:</strong> {listing.rejectionReason}
        </div>
      )}

      <div className="field">
        <label className="field-label">Shop Photo</label>
        <ul className="shop-photo-guidelines">
          {SHOP_PHOTO_GUIDELINES.map((rule) => (
            <li key={rule}>{rule}</li>
          ))}
        </ul>
        {listing.shopPhotoUrl && (
          <div className="shop-photo-preview">
            <img src={resolveImagePath(listing.shopPhotoUrl)} alt={listing.shopPhotoAlt ?? ""} />
            <button type="button" className="shop-photo-remove" onClick={handleRemovePhoto} aria-label="Remove shop photo">
              <Icon name="x" size={14} />
            </button>
          </div>
        )}
        <ImagePicker
          key={pickerKey}
          id="shop-photo-file"
          rules={SHOP_PHOTO_RULES}
          cropAspect={3 / 2}
          disabled={uploading}
          onChange={setPendingPhoto}
        />
        <Button type="button" variant="outlined" size="sm" icon="plus" loading={uploading} disabled={!pendingPhoto} onClick={handleUploadPhoto}>
          {listing.shopPhotoUrl ? "Replace Photo" : "Upload Photo"}
        </Button>
      </div>

      <Input
        id="shop-photo-alt"
        label="Photo description (optional)"
        hint="A short description of the photo, for accessibility"
        value={form.shopPhotoAlt}
        onChange={(e) => update({ shopPhotoAlt: e.target.value })}
      />

      <div className="form-row">
        <Select
          id="category"
          label="Category"
          placeholder="Choose a category"
          value={form.categoryId}
          onChange={(e) => update({ categoryId: e.target.value })}
          options={categories.map((c) => ({ label: `${c.icon} ${c.name}`, value: c.id }))}
        />
        <Input id="area" label="Area / Neighbourhood" value={form.area} onChange={(e) => update({ area: e.target.value })} />
      </div>

      <div className="form-row">
        <Input
          id="instagram"
          label="Instagram URL (optional)"
          value={form.instagramUrl}
          onChange={(e) => update({ instagramUrl: e.target.value })}
        />
        <Input
          id="facebook"
          label="Facebook URL (optional)"
          value={form.facebookUrl}
          onChange={(e) => update({ facebookUrl: e.target.value })}
        />
      </div>

      <div className="form-row">
        <Input
          id="youtube"
          label="YouTube URL (optional)"
          value={form.youtubeUrl}
          onChange={(e) => update({ youtubeUrl: e.target.value })}
        />
        <Input
          id="google-maps"
          label="Google Maps link (optional)"
          value={form.googleMapsUrl}
          onChange={(e) => update({ googleMapsUrl: e.target.value })}
        />
      </div>

      <div className="form-row">
        <Input
          id="whatsapp-url"
          label="WhatsApp chat/group link (optional)"
          hint="Shown as a contact icon - separate from the order number below"
          value={form.whatsappUrl}
          onChange={(e) => update({ whatsappUrl: e.target.value })}
        />
        <Input id="website" label="Website URL (optional)" value={form.websiteUrl} onChange={(e) => update({ websiteUrl: e.target.value })} />
      </div>

      <div className="form-row">
        <Select
          id="order-mode"
          label="How should customers order?"
          value={form.orderMode}
          onChange={(e) => update({ orderMode: e.target.value as MarketplaceOrderMode })}
          options={ORDER_MODE_OPTIONS}
        />
        <Input
          id="enquiry-whatsapp"
          label={form.orderMode === "WHATSAPP" ? "WhatsApp number for orders" : "WhatsApp number for enquiries (optional)"}
          hint="Country code + number, digits only"
          value={form.enquiryWhatsapp}
          onChange={(e) => update({ enquiryWhatsapp: e.target.value })}
        />
      </div>
    </Card>
  );
}
