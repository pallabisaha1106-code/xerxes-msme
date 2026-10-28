import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronDown, MapPin } from "lucide-react";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { CategoryList } from "../components/CategoryList";
import { DiscoveryWelcomeDialog } from "../components/DiscoveryWelcomeDialog";
import { FeedSkeleton } from "../components/FeedSkeleton";
import { AdCarousel } from "../components/AdCarousel";
import { SideRailAd } from "../components/SideRailAd";
import {
  listMarketplaceCategories,
  listPublicFeed,
  listServiceAreas,
  type FeedSortOption,
  type PublicFeedPostDto,
} from "../api/marketplaceApi";
import { extractErrorMessage } from "../api/apiError";
import { useToast } from "../components/Toast";
import { categoryToCategory } from "../data/liveMarketplace";
import { useAdvertisements } from "../lib/useAdvertisements";
import type { BusinessType } from "../api/vendorOnboardingApi";
import { clearDiscoveryPreference, saveDiscoveryPreference, type DiscoverySelection } from "../lib/discoveryStorage";
import { normalizeEnquiryNumber, whatsappURL } from "../lib/urlSafety";
import type { Category } from "../types";

const PAGE_SIZE = 20;

/**
 * "Ends today" / "Ends tomorrow" / "Ends in 5 days" for the next two weeks, then a plain date ("Ends 2 Nov") -
 * "Ends in 40 days" reads oddly. null when there is no end date (or it has somehow already passed). Both dates
 * are local calendar days, so there's no timezone drift around midnight.
 */
function endsLabel(validUntil: string | null): string | null {
  if (!validUntil) return null;
  const [year, month, day] = validUntil.split("-").map(Number);
  const end = new Date(year, month - 1, day);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const days = Math.round((end.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));
  if (days < 0) return null;
  if (days === 0) return "Ends today";
  if (days === 1) return "Ends tomorrow";
  if (days <= 14) return `Ends in ${days} days`;
  return `Ends ${end.toLocaleDateString("en-IN", { day: "numeric", month: "short" })}`;
}

/** "2 hours ago" / "3 days ago" - posts only ever live here for 14 days, so this never needs anything coarser. */
function timeAgo(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const hours = Math.floor(ms / (60 * 60 * 1000));
  if (hours < 1) return "Just now";
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}

const TYPE_LABEL: Record<BusinessType, string> = { RETAILER: "Retail", WHOLESALER: "Wholesale" };

export function FeedPage() {
  const toast = useToast();
  const { topAd, leftAd, rightAd } = useAdvertisements();
  const [categories, setCategories] = useState<Category[] | null>(null);
  // The city/area from the same first-visit picker the home page uses ("shops" kind, so the choice is shared).
  // null = the picker hasn't resolved yet; a selection with no cityId means it was dismissed without a city.
  const [selection, setSelection] = useState<DiscoverySelection | null>(null);
  const [pickerKey, setPickerKey] = useState(0);
  // Starts at the category chosen in the picker (it only ever offers real ones); the chips below change it
  // without touching the saved preference. Names map to ids for the server-side filter.
  const [activeCategory, setActiveCategory] = useState("All");
  // The saved area is a name - its id is resolved separately (null = still resolving, "" = all areas).
  const [areaId, setAreaId] = useState<string | null>("");
  // null = not loaded yet, so the skeleton shows instead of a flash of "No offers".
  const [posts, setPosts] = useState<PublicFeedPostDto[] | null>(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [retryKey, setRetryKey] = useState(0);
  const [sort, setSort] = useState<FeedSortOption>("NEWEST");
  // "" = both retail and wholesale offers.
  const [typeFilter, setTypeFilter] = useState<BusinessType | "">("");
  const cityId = selection?.cityId ?? "";
  const categoryId = categories?.find((c) => c.name === activeCategory)?.id;
  // Identifies the current city/area/category, so a slow "Load more" response for an earlier one can be dropped.
  const filterKey = `${cityId}|${areaId}|${categoryId ?? ""}|${typeFilter}|${sort}`;
  const filterKeyRef = useRef(filterKey);

  useEffect(() => {
    document.title = "Latest Offers | Neighbourly";
  }, []);

  // The picker validates the saved category against this list when it mounts, so it only mounts once this settles.
  useEffect(() => {
    let cancelled = false;
    listMarketplaceCategories()
      .then((result) => {
        if (!cancelled) setCategories(result.map(categoryToCategory));
      })
      .catch(() => {
        if (!cancelled) setCategories([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selection?.cityId || !selection.area) {
      setAreaId("");
      return;
    }
    let cancelled = false;
    setAreaId(null);
    const wanted = selection.area.trim().toLowerCase();
    listServiceAreas(selection.cityId)
      .then((list) => {
        if (!cancelled) setAreaId(list.find((a) => a.name.trim().toLowerCase() === wanted)?.id ?? "");
      })
      .catch(() => {
        if (!cancelled) setAreaId("");
      });
    return () => {
      cancelled = true;
    };
  }, [selection]);

  useEffect(() => {
    if (!cityId || areaId === null) return;
    let cancelled = false;
    filterKeyRef.current = filterKey;
    setPosts(null);
    setPage(0);
    setHasMore(false);
    setLoadingMore(false);
    setLoadError(false);
    // `cancelled` stops an older, slower response from overwriting a newer one.
    listPublicFeed({ cityId, areaId: areaId || undefined, categoryId, businessType: typeFilter || undefined, sort, page: 0, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return;
        setPosts(result.items);
        setHasMore(result.hasMore);
      })
      .catch(() => {
        if (cancelled) return;
        setPosts([]);
        setLoadError(true);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cityId, areaId, categoryId, typeFilter, sort, retryKey]);

  function loadMore() {
    const requestedKey = filterKey;
    const nextPage = page + 1;
    setLoadingMore(true);
    listPublicFeed({ cityId, areaId: areaId || undefined, categoryId, businessType: typeFilter || undefined, sort, page: nextPage, size: PAGE_SIZE })
      .then((result) => {
        if (filterKeyRef.current !== requestedKey) return;
        setPosts((current) => [...(current ?? []), ...result.items]);
        setPage(nextPage);
        setHasMore(result.hasMore);
      })
      .catch((err) => {
        if (filterKeyRef.current !== requestedKey) return;
        toast.show({ severity: "error", summary: "Could not load more offers", detail: extractErrorMessage(err) });
      })
      .finally(() => {
        if (filterKeyRef.current === requestedKey) setLoadingMore(false);
      });
  }

  // Set together (one render) so the first fetch already has the right category.
  function applySelection(next: DiscoverySelection) {
    setSelection(next);
    setActiveCategory(next.category || "All");
  }

  // Same approach as the header's city pill: forget the saved choice, then bring the picker back up (here by
  // remounting it, rather than reloading the page).
  function reopenPicker() {
    clearDiscoveryPreference("shops");
    setPickerKey((key) => key + 1);
  }

  // The empty-state "see the whole city" action: drop the area, and remember that like the picker would.
  function showWholeCity() {
    if (!selection) return;
    saveDiscoveryPreference("shops", { cityId: selection.cityId, cityName: selection.cityName, area: "", category: activeCategory });
    applySelection({ ...selection, area: "", category: activeCategory });
  }

  const locationLabel = [selection?.cityName, selection?.area].filter(Boolean).join(" · ");
  const loaded = Boolean(cityId) && areaId !== null && posts !== null;
  // The server pages without a total, so "20+" while more remain and the exact number once everything is loaded.
  const countLabel =
    loaded && posts.length > 0
      ? `${posts.length}${hasMore ? "+" : ""} offer${posts.length === 1 && !hasMore ? "" : "s"}${categoryId ? ` · ${activeCategory}` : ""}${typeFilter ? ` · ${TYPE_LABEL[typeFilter]}` : ""}`
      : null;

  return (
    <>
      <TopStrip />
      <SiteHeader />
      <AdCarousel ad={topAd} />
      <div className="market-layout">
      <SideRailAd side="left" ad={leftAd} />
      <main className="feed-page" id="feed">
        <h1>Latest offers near you</h1>
        <p className="join-subheading">
          Shop owners post a quick update when they have something new - sales, fresh stock, limited-time offers.
          Posts disappear after 14 days, so what you see here is always current.
        </p>

        {cityId && (
          <div className="feed-controls">
            <button
              type="button"
              className="feed-location-chip"
              aria-label={`Change location, currently ${locationLabel}`}
              onClick={reopenPicker}
            >
              <MapPin size={16} strokeWidth={2} aria-hidden="true" />
              {locationLabel}
              <ChevronDown size={16} strokeWidth={2} aria-hidden="true" />
            </button>
            <div className="feed-type-filter" role="group" aria-label="Show">
              {(["", "RETAILER", "WHOLESALER"] as const).map((value) => (
                <button
                  key={value || "ALL"}
                  type="button"
                  aria-pressed={typeFilter === value}
                  className={typeFilter === value ? "active" : ""}
                  onClick={() => setTypeFilter(value)}
                >
                  {value ? TYPE_LABEL[value] : "All"}
                </button>
              ))}
            </div>
          </div>
        )}

        {cityId && categories && categories.length > 0 && (
          <div className="feed-categories">
            <CategoryList categories={categories} activeCategory={activeCategory} listingPlural="offers" onSelect={setActiveCategory} />
          </div>
        )}

        {/* Stays mounted while a new sort loads (posts === null), so the select keeps keyboard focus. */}
        {cityId && !loadError && (posts === null || posts.length > 0) && (
          <div className="feed-toolbar">
            <p className="feed-count" role="status" aria-live="polite">
              {countLabel}
            </p>
            <label className="feed-sort">
              Sort
              <select value={sort} onChange={(e) => setSort(e.target.value as FeedSortOption)}>
                <option value="NEWEST">Newest</option>
                <option value="ENDING_SOON">Ending soon</option>
              </select>
            </label>
          </div>
        )}

        {(selection === null || (cityId && (posts === null || areaId === null))) && <FeedSkeleton />}

        {selection && !cityId && (
          <div className="feed-empty-state">
            <p className="feed-empty-title">See what your neighbours are offering</p>
            <p className="feed-empty-text">Choose your city and we'll show the latest offers from shops near you.</p>
            <div className="feed-empty-actions">
              <button type="button" className="primary-button" onClick={reopenPicker}>
                Choose your city
              </button>
            </div>
          </div>
        )}

        {loaded && loadError && (
          <div className="feed-empty-state">
            <p className="feed-empty-title">Couldn't load the offers</p>
            <p className="feed-empty-text">Check your connection and try again.</p>
            <div className="feed-empty-actions">
              <button type="button" className="primary-button" onClick={() => setRetryKey((key) => key + 1)}>
                Try again
              </button>
            </div>
          </div>
        )}

        {loaded && !loadError && posts.length === 0 && (
          <div className="feed-empty-state">
            {typeFilter ? (
              <>
                <p className="feed-empty-title">No {TYPE_LABEL[typeFilter].toLowerCase()} offers right now</p>
                <p className="feed-empty-text">Nothing posted in the last 14 days. Try showing both.</p>
                <div className="feed-empty-actions">
                  <button type="button" className="primary-button" onClick={() => setTypeFilter("")}>
                    Show all offers
                  </button>
                </div>
              </>
            ) : categoryId ? (
              <>
                <p className="feed-empty-title">No {activeCategory} offers right now</p>
                <p className="feed-empty-text">Nothing posted in the last 14 days. Try another category.</p>
                <div className="feed-empty-actions">
                  <button type="button" className="primary-button" onClick={() => setActiveCategory("All")}>
                    Show all offers
                  </button>
                </div>
              </>
            ) : selection?.area ? (
              <>
                <p className="feed-empty-title">No offers in {selection.area} yet</p>
                <p className="feed-empty-text">Nothing posted here in the last 14 days. Other parts of {selection.cityName} may have some.</p>
                <div className="feed-empty-actions">
                  <button type="button" className="primary-button" onClick={showWholeCity}>
                    See all of {selection.cityName}
                  </button>
                </div>
              </>
            ) : (
              <>
                <p className="feed-empty-title">No offers in {selection?.cityName} yet</p>
                <p className="feed-empty-text">Shops here haven't posted an offer in the last 14 days. Check back soon, or browse the shops.</p>
                <div className="feed-empty-actions">
                  <Link className="primary-button" to="/shops">
                    Browse shops
                  </Link>
                </div>
              </>
            )}
          </div>
        )}

        <div className="feed-list">
          {(cityId ? posts ?? [] : []).map((post) => {
            // Opens WhatsApp with the offer quoted, so the shop knows which one the shopper means. "" = no usable number.
            const whatsapp = whatsappURL(
              normalizeEnquiryNumber(post.enquiryWhatsapp ?? undefined),
              `Hi ${post.businessName}, I saw your offer on Neighbourly: "${post.content}"`,
            );
            const ends = endsLabel(post.validUntil);
            return (
              <article className="feed-card" key={post.tenantId + post.createdAt}>
                <div className="feed-card-head">
                  <span className="feed-card-avatar" aria-hidden="true">
                    {post.businessName.trim().charAt(0).toUpperCase()}
                  </span>
                  <div className="feed-card-shop">
                    <p className="feed-card-name">{post.businessName}</p>
                    <p className="feed-card-meta">{[post.area, timeAgo(post.createdAt)].filter(Boolean).join(" · ")}</p>
                  </div>
                  <span className={`feed-card-type ${post.businessType === "WHOLESALER" ? "wholesale" : "retail"}`}>
                    {TYPE_LABEL[post.businessType]}
                  </span>
                  {post.discountLabel && <span className="feed-card-badge">{post.discountLabel}</span>}
                </div>
                <p className="feed-card-content">{post.content}</p>
                {(ends || post.categoryName) && (
                  <div className="feed-card-tags">
                    {ends && <span className="feed-card-ends">{ends}</span>}
                    {post.categoryName && <p className="feed-card-category">{post.categoryName}</p>}
                  </div>
                )}
                <div className="feed-card-actions">
                  <Link className="feed-card-view" to={`/shop/${post.tenantId}`} aria-label={`View ${post.businessName}`}>
                    View shop
                  </Link>
                  {whatsapp && (
                    <a
                      className="feed-card-whatsapp"
                      href={whatsapp}
                      target="_blank"
                      rel="noopener noreferrer"
                      aria-label={`WhatsApp ${post.businessName} about this offer`}
                    >
                      WhatsApp
                    </a>
                  )}
                </div>
              </article>
            );
          })}
        </div>
        {cityId && hasMore && (
          <div className="load-more-row">
            <button type="button" className="secondary-button" onClick={loadMore} disabled={loadingMore}>
              {loadingMore ? "Loading…" : "Load more offers"}
            </button>
          </div>
        )}
      </main>
      <SideRailAd side="right" ad={rightAd} />
      </div>
      <SiteFooter listBusinessLabel="List your business" />

      {categories && <DiscoveryWelcomeDialog key={pickerKey} categories={categories} kind="shops" onApply={applySelection} allowBrowseAll={false} />}
    </>
  );
}
