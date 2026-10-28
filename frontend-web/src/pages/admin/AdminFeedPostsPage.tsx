import { useEffect, useRef, useState } from "react";
import { Button, Card, Dialog, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminFeedPostApi from "../../api/adminFeedPostApi";
import type { AdminFeedPost } from "../../api/adminFeedPostApi";
import type { FeedPostStatus } from "../../api/feedPostApi";

function statusTag(status: FeedPostStatus) {
  if (status === "APPROVED") return <Tag severity="success">APPROVED</Tag>;
  if (status === "REJECTED") return <Tag severity="danger">REJECTED</Tag>;
  return <Tag severity="warning">PENDING</Tag>;
}

/** A date-only string (YYYY-MM-DD) shown in the viewer's locale, without a timezone shift. */
function formatDay(isoDay: string): string {
  return new Date(`${isoDay}T00:00:00`).toLocaleDateString();
}

const STATUS_FILTER_OPTIONS = [
  { label: "All posts", value: "" },
  { label: "Pending", value: "PENDING" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
];

export function AdminFeedPostsPage() {
  const toast = useToast();
  const [posts, setPosts] = useState<AdminFeedPost[]>([]);
  const [statusFilter, setStatusFilter] = useState<FeedPostStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [reviewTarget, setReviewTarget] = useState<AdminFeedPost | null>(null);
  const [reviewReason, setReviewReason] = useState("");

  // Only the newest load may update the table: a slower, older response must not overwrite a newer one (or a row
  // Approve just changed).
  const latestLoad = useRef(0);

  async function loadPosts() {
    const thisLoad = ++latestLoad.current;
    setLoading(true);
    try {
      const list = await adminFeedPostApi.listFeedPosts(statusFilter || undefined);
      if (thisLoad === latestLoad.current) setPosts(list);
    } catch (err) {
      if (thisLoad === latestLoad.current) {
        toast.show({ severity: "error", summary: "Failed to load feed posts", detail: extractErrorMessage(err) });
      }
    } finally {
      if (thisLoad === latestLoad.current) setLoading(false);
    }
  }

  useEffect(() => {
    loadPosts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  function applyUpdatedPost(post: AdminFeedPost) {
    setPosts((current) => current.map((p) => (p.id === post.id ? post : p)));
  }

  async function handleApprove(post: AdminFeedPost) {
    setActioningId(post.id);
    try {
      const updated = await adminFeedPostApi.approveFeedPost(post.id);
      applyUpdatedPost(updated);
      toast.show({ severity: "success", summary: "Post approved", detail: post.businessName ?? undefined });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not approve post", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  function openReviewDialog(post: AdminFeedPost) {
    setReviewTarget(post);
    setReviewReason("");
  }

  async function handleConfirmReject() {
    if (!reviewTarget) return;
    setActioningId(reviewTarget.id);
    try {
      const updated = await adminFeedPostApi.rejectFeedPost(reviewTarget.id, reviewReason);
      applyUpdatedPost(updated);
      toast.show({ severity: "success", summary: "Post rejected", detail: reviewTarget.businessName ?? undefined });
      setReviewTarget(null);
      setReviewReason("");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not reject post", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  // en-CA formats as YYYY-MM-DD, so it compares directly with validUntil.
  const today = new Date().toLocaleDateString("en-CA");

  const columns: Column<AdminFeedPost>[] = [
    { field: "businessName", header: "Business", body: (p) => p.businessName ?? "-" },
    { field: "content", header: "Content" },
    { field: "discountLabel", header: "Discount", body: (p) => p.discountLabel ?? "-" },
    {
      field: "validUntil",
      header: "Ends on",
      // An end date already in the past is flagged so a reviewer doesn't approve an offer that can never show.
      body: (p) => (p.validUntil ? `${formatDay(p.validUntil)}${p.validUntil < today ? " (ended)" : ""}` : "-"),
    },
    { field: "status", header: "Status", body: (p) => statusTag(p.status) },
    {
      field: "createdAt",
      header: "Posted",
      body: (p) => new Date(p.createdAt).toLocaleString(),
    },
    {
      field: "actions",
      header: "",
      body: (p) =>
        p.status === "PENDING" && (
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button size="sm" severity="success" loading={actioningId === p.id} onClick={() => handleApprove(p)}>
              Approve
            </Button>
            <Button
              size="sm"
              variant="outlined"
              severity="danger"
              disabled={actioningId === p.id}
              onClick={() => openReviewDialog(p)}
            >
              Reject
            </Button>
          </div>
        ),
    },
  ];

  return (
    <Card
      title="Feed Posts"
      actions={
        <Select
          id="status-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as FeedPostStatus | "")}
          options={STATUS_FILTER_OPTIONS}
        />
      }
    >
      <Table data={posts} columns={columns} rowKey={(row) => row.id} loading={loading} emptyMessage="No feed posts yet." />

      <Dialog
        visible={reviewTarget != null}
        onHide={() => setReviewTarget(null)}
        title="Reject Post"
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setReviewTarget(null)}>
              Cancel
            </Button>
            <Button severity="danger" loading={actioningId === reviewTarget?.id} onClick={handleConfirmReject}>
              Reject
            </Button>
          </>
        }
      >
        <p>
          Reject the post from <strong>{reviewTarget?.businessName}</strong>?
        </p>
        <Input id="review-reason" label="Reason (optional)" value={reviewReason} onChange={(e) => setReviewReason(e.target.value)} />
      </Dialog>
    </Card>
  );
}
