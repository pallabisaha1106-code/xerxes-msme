import { apiClient } from "./client";
import type { FeedPostStatus } from "./feedPostApi";

export interface AdminFeedPost {
  id: string;
  tenantId: string;
  businessName: string | null;
  content: string;
  discountLabel: string | null;
  /** Last day (YYYY-MM-DD, inclusive) the offer is shown; null means the default 14-day window. */
  validUntil: string | null;
  status: FeedPostStatus;
  rejectionReason: string | null;
  createdAt: string;
  reviewedAt: string | null;
}

export async function listFeedPosts(status?: FeedPostStatus): Promise<AdminFeedPost[]> {
  const { data } = await apiClient.get<AdminFeedPost[]>("/admin/feed-posts", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function approveFeedPost(id: string): Promise<AdminFeedPost> {
  const { data } = await apiClient.post<AdminFeedPost>(`/admin/feed-posts/${id}/approve`);
  return data;
}

export async function rejectFeedPost(id: string, reason?: string): Promise<AdminFeedPost> {
  const { data } = await apiClient.post<AdminFeedPost>(`/admin/feed-posts/${id}/reject`, { reason });
  return data;
}
