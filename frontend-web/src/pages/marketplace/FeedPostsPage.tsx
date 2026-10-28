import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Button, Card, Input, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as feedPostApi from "../../api/feedPostApi";
import type { FeedPost } from "../../api/feedPostApi";

const MAX_LENGTH = 140;
const MAX_LABEL_LENGTH = 30;
const COOLDOWN_DAYS = 7;

/** A date-only string (YYYY-MM-DD) shown in the viewer's locale, without a timezone shift. */
function formatDay(isoDay: string): string {
  return new Date(`${isoDay}T00:00:00`).toLocaleDateString();
}

function statusTag(status: FeedPost["status"]) {
  if (status === "APPROVED") return <Tag severity="success">APPROVED</Tag>;
  if (status === "REJECTED") return <Tag severity="danger">REJECTED</Tag>;
  return <Tag severity="warning">PENDING REVIEW</Tag>;
}

export function FeedPostsPage() {
  const toast = useToast();
  const [posts, setPosts] = useState<FeedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState("");
  const [discountLabel, setDiscountLabel] = useState("");
  const [validUntil, setValidUntil] = useState("");
  const [posting, setPosting] = useState(false);

  async function loadPosts() {
    setLoading(true);
    try {
      setPosts(await feedPostApi.listMyFeedPosts());
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not load your posts", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPosts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // posts is newest-first (see FeedPostRepository.findAllByTenantIdOrderByCreatedAtDesc), so
  // posts[0] is the one the 7-day cooldown is measured from - same rule FeedPostService enforces
  // server-side; this is just so the vendor sees it coming instead of hitting a 422.
  const cooldownEndsAt = useMemo(() => {
    if (posts.length === 0) return null;
    return new Date(posts[0].createdAt).getTime() + COOLDOWN_DAYS * 24 * 60 * 60 * 1000;
  }, [posts]);
  const onCooldown = cooldownEndsAt !== null && Date.now() < cooldownEndsAt;
  const cooldownDaysLeft = onCooldown ? Math.ceil((cooldownEndsAt! - Date.now()) / (24 * 60 * 60 * 1000)) : 0;

  const charsLeft = MAX_LENGTH - content.length;
  // en-CA formats as YYYY-MM-DD, which is what <input type="date"> and the API both use (local day, not UTC).
  const today = new Date().toLocaleDateString("en-CA");
  const endDateInPast = validUntil !== "" && validUntil < today;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPosting(true);
    try {
      await feedPostApi.createFeedPost({
        content: content.trim(),
        discountLabel: discountLabel.trim() || undefined,
        validUntil: validUntil || undefined,
      });
      toast.show({ severity: "success", summary: "Post submitted for review" });
      setContent("");
      setDiscountLabel("");
      setValidUntil("");
      await loadPosts();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not submit your post", detail: extractErrorMessage(err) });
    } finally {
      setPosting(false);
    }
  }

  const columns: Column<FeedPost>[] = [
    { field: "content", header: "Post" },
    { field: "discountLabel", header: "Discount", body: (row) => row.discountLabel ?? "-" },
    { field: "validUntil", header: "Ends on", body: (row) => (row.validUntil ? formatDay(row.validUntil) : "-") },
    { field: "status", header: "Status", body: (row) => statusTag(row.status) },
    { field: "createdAt", header: "Posted", body: (row) => new Date(row.createdAt).toLocaleString() },
    {
      field: "rejectionReason",
      header: "Reason",
      body: (row) => (row.status === "REJECTED" ? row.rejectionReason ?? "-" : "-"),
    },
  ];

  return (
    <Card title="Feed Posts">
      <p className="join-section-note">
        Share a current offer or sale ({MAX_LENGTH} characters, once every {COOLDOWN_DAYS} days) - a Super Admin
        reviews it before it appears in the public feed. It stays visible for 14 days, or until the end date you set.
      </p>
      <form onSubmit={handleSubmit} className="field">
        <label htmlFor="feed-post-content" className="field-label">
          What&rsquo;s new?
        </label>
        <textarea
          id="feed-post-content"
          className="input"
          rows={3}
          maxLength={MAX_LENGTH}
          value={content}
          disabled={onCooldown}
          onChange={(e) => setContent(e.target.value)}
          placeholder="e.g. 20% off all winter wear this weekend!"
        />
        <span className="field-hint">{charsLeft} characters left</span>
        <Input
          id="feed-post-discount"
          label="Discount label (optional)"
          maxLength={MAX_LABEL_LENGTH}
          value={discountLabel}
          disabled={onCooldown}
          onChange={(e) => setDiscountLabel(e.target.value)}
          placeholder="e.g. 20% off, Buy 1 Get 1"
          hint={`Shown as a badge on your offer. ${MAX_LABEL_LENGTH - discountLabel.length} characters left`}
        />
        <Input
          id="feed-post-valid-until"
          type="date"
          label="Offer ends on (optional)"
          min={today}
          value={validUntil}
          disabled={onCooldown}
          onChange={(e) => setValidUntil(e.target.value)}
          error={endDateInPast ? "The end date can't be in the past." : undefined}
          hint="Leave empty to show it for 14 days."
        />
        {onCooldown ? (
          <p className="field-hint">You can post again in {cooldownDaysLeft} day(s).</p>
        ) : (
          <Button type="submit" loading={posting} disabled={!content.trim() || endDateInPast}>
            Post Update
          </Button>
        )}
      </form>

      <Table
        data={posts}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No posts yet - share your first offer above."
      />
    </Card>
  );
}
