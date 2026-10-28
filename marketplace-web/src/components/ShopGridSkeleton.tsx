/**
 * Placeholder for the shop grid while listings load. Reuses .business-card/.business-grid so the
 * real cards land in exactly the same boxes; the shapes are decorative (aria-hidden) and the
 * status line is what assistive tech announces.
 */
export function ShopGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <section className="listings" aria-busy="true">
      <p className="sr-only" role="status">
        Loading shops…
      </p>
      <div className="business-grid" aria-hidden="true">
        {Array.from({ length: count }, (_, i) => (
          <article className="business-card skeleton-card" key={i}>
            <div className="skeleton skeleton-img" />
            <div className="card-content">
              <div className="skeleton skeleton-line" style={{ width: "30%", height: 10 }} />
              <div className="skeleton skeleton-line" style={{ width: "85%", height: 16 }} />
              <div className="skeleton skeleton-line" style={{ width: "55%", height: 16, marginBottom: 14 }} />
              <div className="skeleton skeleton-line" style={{ width: "35%" }} />
              <div className="skeleton skeleton-line" style={{ width: "50%" }} />
              <div className="skeleton skeleton-btn" />
              <div className="skeleton skeleton-btn" />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
