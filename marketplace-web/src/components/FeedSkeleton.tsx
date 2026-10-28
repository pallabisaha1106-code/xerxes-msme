/**
 * Placeholder for the offers feed while it loads. Reuses .feed-card/.feed-card-head/.feed-card-actions so the
 * real cards land in about the same boxes; the shapes are decorative (aria-hidden) and the status line is what
 * assistive tech announces.
 */
export function FeedSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="feed-list" aria-busy="true">
      <p className="sr-only" role="status">
        Loading offers…
      </p>
      {Array.from({ length: count }, (_, i) => (
        <article className="feed-card" key={i} aria-hidden="true">
          <div className="feed-card-head">
            <div className="skeleton" style={{ width: 36, height: 36, borderRadius: "50%", flexShrink: 0 }} />
            <div style={{ flex: 1 }}>
              <div className="skeleton skeleton-line" style={{ width: "45%", height: 14 }} />
              <div className="skeleton skeleton-line" style={{ width: "30%", height: 12, marginBottom: 0 }} />
            </div>
          </div>
          <div>
            <div className="skeleton skeleton-line" style={{ width: "92%", height: 18 }} />
            <div className="skeleton skeleton-line" style={{ width: "60%", height: 18, marginBottom: 0 }} />
          </div>
          <div className="skeleton" style={{ width: 90, height: 24, borderRadius: 999 }} />
          <div className="feed-card-actions">
            <div className="skeleton feed-skeleton-btn" />
            <div className="skeleton feed-skeleton-btn" />
          </div>
        </article>
      ))}
    </div>
  );
}
