import { useEffect, useState } from "react";
import { Button, Card, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminMarketplaceCategoryApi from "../../api/adminMarketplaceCategoryApi";
import type { AdminMarketplaceCategory, ImageProfile } from "../../api/adminMarketplaceCategoryApi";

const IMAGE_PROFILE_OPTIONS: { label: string; value: ImageProfile }[] = [
  { label: "Compact (small pack shots)", value: "COMPACT" },
  { label: "Detail (zoom, sharper photos)", value: "DETAIL" },
];

const BOOKING_OPTIONS = [
  { label: "Orders", value: "false" },
  { label: "Appointments", value: "true" },
];

export function AdminMarketplaceCategoriesPage() {
  const toast = useToast();
  const [categories, setCategories] = useState<AdminMarketplaceCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [profileSavingId, setProfileSavingId] = useState<string | null>(null);
  const [bookableSavingId, setBookableSavingId] = useState<string | null>(null);

  async function loadCategories() {
    setLoading(true);
    try {
      setCategories(await adminMarketplaceCategoryApi.listMarketplaceCategories());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load categories", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleToggle(category: AdminMarketplaceCategory) {
    setTogglingId(category.id);
    try {
      const updated = await adminMarketplaceCategoryApi.updateMarketplaceCategoryStatus(category.id, !category.active);
      setCategories((current) => current.map((c) => (c.id === updated.id ? updated : c)));
      toast.show({
        severity: "success",
        summary: updated.active ? "Category enabled" : "Category disabled",
        detail: updated.name,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update category", detail: extractErrorMessage(err) });
    } finally {
      setTogglingId(null);
    }
  }

  async function handleImageProfileChange(category: AdminMarketplaceCategory, imageProfile: ImageProfile) {
    setProfileSavingId(category.id);
    try {
      const updated = await adminMarketplaceCategoryApi.updateMarketplaceCategoryImageProfile(category.id, imageProfile);
      setCategories((current) => current.map((c) => (c.id === updated.id ? updated : c)));
      toast.show({ severity: "success", summary: "Photo style updated", detail: updated.name });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update photo style", detail: extractErrorMessage(err) });
    } finally {
      setProfileSavingId(null);
    }
  }

  async function handleBookableChange(category: AdminMarketplaceCategory, bookable: boolean) {
    setBookableSavingId(category.id);
    try {
      const updated = await adminMarketplaceCategoryApi.updateMarketplaceCategoryBookable(category.id, bookable);
      setCategories((current) => current.map((c) => (c.id === updated.id ? updated : c)));
      toast.show({ severity: "success", summary: "Booking mode updated", detail: updated.name });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update booking mode", detail: extractErrorMessage(err) });
    } finally {
      setBookableSavingId(null);
    }
  }

  const columns: Column<AdminMarketplaceCategory>[] = [
    { field: "icon", header: "", body: (c) => <span style={{ fontSize: "1.2rem" }}>{c.icon}</span> },
    { field: "name", header: "Category" },
    { field: "displayOrder", header: "Order" },
    {
      field: "active",
      header: "Status",
      body: (c) => (c.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">DISABLED</Tag>),
    },
    {
      field: "imageProfile",
      header: "Photo style",
      body: (c) => (
        <Select
          id={`image-profile-${c.id}`}
          value={c.imageProfile}
          disabled={profileSavingId === c.id}
          onChange={(e) => handleImageProfileChange(c, e.target.value as ImageProfile)}
          options={IMAGE_PROFILE_OPTIONS}
        />
      ),
    },
    {
      field: "bookable",
      header: "Shops take",
      body: (c) => (
        <Select
          id={`bookable-${c.id}`}
          aria-label={`Shops in ${c.name} take`}
          value={String(c.bookable)}
          disabled={bookableSavingId === c.id}
          onChange={(e) => handleBookableChange(c, e.target.value === "true")}
          options={BOOKING_OPTIONS}
        />
      ),
    },
    {
      field: "actions",
      header: "",
      body: (c) => (
        <Button
          size="sm"
          variant="outlined"
          severity={c.active ? "danger" : "success"}
          loading={togglingId === c.id}
          onClick={() => handleToggle(c)}
        >
          {c.active ? "Disable" : "Enable"}
        </Button>
      ),
    },
  ];

  return (
    <Card title="Marketplace Categories">
      <p className="field-hint" style={{ marginBottom: "1rem" }}>
        Disabling a category hides it from the public marketplace&rsquo;s category list and filters immediately.
        Existing listings and starter-catalog entries under it are left alone.
      </p>
      <Table
        data={categories}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No marketplace categories yet."
      />
    </Card>
  );
}
