import { useEffect, useState } from "react";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { listMarketplaceCategories } from "../api/marketplaceApi";
import { categoryToCategory } from "../data/liveMarketplace";
import { MarketplacePage, type MarketplacePageCopy } from "./MarketplacePage";
import type { Category, MarketplaceConfig } from "../types";

/**
 * Shared by Explore Shops and Explore Wholesaler: loads the category list, then hands over to MarketplacePage, which
 * fetches the listings itself (paginated, and scoped to the city, category and business type).
 */
export function ExploreListingsPage({
  kind,
  config,
  copy,
  loadingText,
}: {
  kind: string;
  config: MarketplaceConfig;
  copy: MarketplacePageCopy;
  loadingText: string;
}) {
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    document.title = copy.pageTitle;
    document.querySelector('meta[name="description"]')?.setAttribute("content", copy.metaDescription);
  }, [copy]);

  useEffect(() => {
    let cancelled = false;
    listMarketplaceCategories()
      .then((result) => {
        if (!cancelled) setCategories(result.map(categoryToCategory));
      })
      .catch(() => {
        if (!cancelled) setError(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="marketplace-status">
          <p>We couldn’t load the marketplace right now. Please try again shortly.</p>
        </main>
        <SiteFooter listBusinessLabel={copy.onboarding.footerLinkLabel} />
      </>
    );
  }

  if (!categories) {
    return (
      <>
        <TopStrip />
        <SiteHeader />
        <main className="marketplace-status">
          <p>{loadingText}</p>
        </main>
        <SiteFooter listBusinessLabel={copy.onboarding.footerLinkLabel} />
      </>
    );
  }

  return <MarketplacePage categories={categories} config={config} kind={kind} copy={copy} />;
}
