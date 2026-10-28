import { useEffect, useRef, useState } from "react";
import { Button, Card, Dialog, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminMarketplaceListingApi from "../../api/adminMarketplaceListingApi";
import type { AdminMarketplaceListing } from "../../api/adminMarketplaceListingApi";
import type { MarketplaceListingStatus } from "../../api/marketplaceListingApi";

function statusTag(status: MarketplaceListingStatus) {
  if (status === "LIVE") return <Tag severity="success">LIVE</Tag>;
  if (status === "PENDING") return <Tag severity="warning">PENDING</Tag>;
  return <Tag severity="secondary">DRAFT</Tag>;
}

const STATUS_FILTER_OPTIONS = [
  { label: "All listings", value: "" },
  { label: "Pending", value: "PENDING" },
  { label: "Live", value: "LIVE" },
  { label: "Draft", value: "DRAFT" },
];

type ReviewAction = "reject" | "unpublish";

export function AdminMarketplaceListingsPage() {
  const toast = useToast();
  const [listings, setListings] = useState<AdminMarketplaceListing[]>([]);
  const [statusFilter, setStatusFilter] = useState<MarketplaceListingStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [actioningTenantId, setActioningTenantId] = useState<string | null>(null);
  const [reviewTarget, setReviewTarget] = useState<AdminMarketplaceListing | null>(null);
  const [reviewAction, setReviewAction] = useState<ReviewAction | null>(null);
  const [reviewReason, setReviewReason] = useState("");

  // Only the newest load may update the table: a slower, older response must not overwrite a newer one (or a row
  // Approve just changed).
  const latestLoad = useRef(0);

  async function loadListings() {
    const thisLoad = ++latestLoad.current;
    setLoading(true);
    try {
      const list = await adminMarketplaceListingApi.listMarketplaceListings(statusFilter || undefined);
      if (thisLoad === latestLoad.current) setListings(list);
    } catch (err) {
      if (thisLoad === latestLoad.current) {
        toast.show({ severity: "error", summary: "Failed to load listings", detail: extractErrorMessage(err) });
      }
    } finally {
      if (thisLoad === latestLoad.current) setLoading(false);
    }
  }

  useEffect(() => {
    loadListings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  function applyUpdatedListing(listing: AdminMarketplaceListing) {
    setListings((current) => current.map((l) => (l.tenantId === listing.tenantId ? listing : l)));
  }

  async function handleApprove(listing: AdminMarketplaceListing) {
    setActioningTenantId(listing.tenantId);
    try {
      const updated = await adminMarketplaceListingApi.approveMarketplaceListing(listing.tenantId);
      applyUpdatedListing(updated);
      toast.show({ severity: "success", summary: "Listing approved", detail: listing.businessName ?? undefined });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not approve listing", detail: extractErrorMessage(err) });
    } finally {
      setActioningTenantId(null);
    }
  }

  function openReviewDialog(listing: AdminMarketplaceListing, action: ReviewAction) {
    setReviewTarget(listing);
    setReviewAction(action);
    setReviewReason("");
  }

  async function handleConfirmReview() {
    if (!reviewTarget || !reviewAction) return;
    setActioningTenantId(reviewTarget.tenantId);
    try {
      const updated =
        reviewAction === "reject"
          ? await adminMarketplaceListingApi.rejectMarketplaceListing(reviewTarget.tenantId, reviewReason)
          : await adminMarketplaceListingApi.unpublishMarketplaceListing(reviewTarget.tenantId, reviewReason);
      applyUpdatedListing(updated);
      toast.show({
        severity: "success",
        summary: reviewAction === "reject" ? "Listing rejected" : "Listing unpublished",
        detail: reviewTarget.businessName ?? undefined,
      });
      setReviewTarget(null);
      setReviewAction(null);
      setReviewReason("");
    } catch (err) {
      toast.show({
        severity: "error",
        summary: reviewAction === "reject" ? "Could not reject listing" : "Could not unpublish listing",
        detail: extractErrorMessage(err),
      });
    } finally {
      setActioningTenantId(null);
    }
  }

  const columns: Column<AdminMarketplaceListing>[] = [
    { field: "businessName", header: "Business", body: (l) => l.businessName ?? "-" },
    { field: "categoryName", header: "Category", body: (l) => l.categoryName ?? "-" },
    { field: "area", header: "Area", body: (l) => l.area ?? "-" },
    { field: "status", header: "Status", body: (l) => statusTag(l.status) },
    {
      field: "submittedAt",
      header: "Submitted",
      body: (l) => (l.submittedAt ? new Date(l.submittedAt).toLocaleString() : "-"),
    },
    {
      field: "actions",
      header: "",
      body: (l) => (
        <div style={{ display: "flex", gap: "0.5rem" }}>
          {l.status === "PENDING" && (
            <>
              <Button
                size="sm"
                severity="success"
                loading={actioningTenantId === l.tenantId}
                onClick={() => handleApprove(l)}
              >
                Approve
              </Button>
              <Button
                size="sm"
                variant="outlined"
                severity="danger"
                disabled={actioningTenantId === l.tenantId}
                onClick={() => openReviewDialog(l, "reject")}
              >
                Reject
              </Button>
            </>
          )}
          {l.status === "LIVE" && (
            <Button
              size="sm"
              variant="outlined"
              severity="danger"
              disabled={actioningTenantId === l.tenantId}
              onClick={() => openReviewDialog(l, "unpublish")}
            >
              Unpublish
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <Card
      title="Marketplace Listings"
      actions={
        <Select
          id="status-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as MarketplaceListingStatus | "")}
          options={STATUS_FILTER_OPTIONS}
        />
      }
    >
      <Table
        data={listings}
        columns={columns}
        rowKey={(row) => row.tenantId}
        loading={loading}
        emptyMessage="No marketplace listings yet."
      />

      <Dialog
        visible={reviewTarget != null}
        onHide={() => {
          setReviewTarget(null);
          setReviewAction(null);
        }}
        title={reviewAction === "unpublish" ? "Unpublish Listing" : "Reject Listing"}
        width={420}
        footer={
          <>
            <Button
              variant="outlined"
              severity="secondary"
              onClick={() => {
                setReviewTarget(null);
                setReviewAction(null);
              }}
            >
              Cancel
            </Button>
            <Button
              severity="danger"
              loading={actioningTenantId === reviewTarget?.tenantId}
              onClick={handleConfirmReview}
            >
              {reviewAction === "unpublish" ? "Unpublish" : "Reject"}
            </Button>
          </>
        }
      >
        <p>
          {reviewAction === "unpublish" ? "Unpublish the listing from" : "Reject the listing from"}{" "}
          <strong>{reviewTarget?.businessName}</strong>?
        </p>
        <Input
          id="review-reason"
          label="Reason (optional)"
          value={reviewReason}
          onChange={(e) => setReviewReason(e.target.value)}
        />
      </Dialog>
    </Card>
  );
}
