import { apiClient } from "./client";

export type FeedPostStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface FeedPost {
  id: string;
  content: string;
  /** Short badge text ("20% off"), if the vendor set one. */
  discountLabel: string | null;
  /** Last day (YYYY-MM-DD, inclusive) the offer is shown; null means the default 14-day window. */
  validUntil: string | null;
  status: FeedPostStatus;
  rejectionReason: string | null;
  createdAt: string;
}

export interface NewFeedPost {
  content: string;
  discountLabel?: string;
  validUntil?: string;
}

export async function createFeedPost(post: NewFeedPost): Promise<FeedPost> {
  const { data } = await apiClient.post<FeedPost>("/feed-posts", post);
  return data;
}

export async function listMyFeedPosts(): Promise<FeedPost[]> {
  const { data } = await apiClient.get<FeedPost[]>("/feed-posts/mine");
  return data;
}
