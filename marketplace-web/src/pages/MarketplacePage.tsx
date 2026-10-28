import { useEffect, useMemo, useState, type ReactNode } from "react";
import { TopStrip } from "../components/TopStrip";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { AdCarousel } from "../components/AdCarousel";
import { DiscoveryStrip } from "../components/DiscoveryStrip";
import { CategoryList } from "../components/CategoryList";
import { BusinessGrid } from "../components/BusinessGrid";
import { ShopGridSkeleton } from "../components/ShopGridSkeleton";
import { SideRailAd } from "../components/SideRailAd";
import { OnboardingSection } from "../components/OnboardingSection";
import { LightboxDialog } from "../components/LightboxDialog";
import { ContactNoticeDialog } from "../components/ContactNoticeDialog";
import { DiscoveryWelcomeDialog } from "../components/DiscoveryWelcomeDialog";
import { matchesFilters, uniqueSorted } from "../lib/filtering";
import type { BusinessType } from "../api/vendorOnboardingApi";
import { clearDiscoveryPreference, saveDiscoveryPreference, type DiscoverySelection } from "../lib/discoveryStorage";
import { listAdvertisements, listMarketplaceListings, type AdvertisementSlotsDto } from "../api/marketplaceApi";
import { listingToBusiness } from "../data/liveMarketplace";
import { DEFAULT_ADS } from "../data/defaultAds";
import type { Business, Category, MarketplaceConfig, Product } from "../types";

const PAGE_SIZE = 20;

export interface MarketplacePageCopy {
  pageTitle: string;
  metaDescription: string;
  skipLinkLabel: string;
  heading: string;
  subheading: string;
  searchAriaLabel: string;
  searchPlaceholder: string;
  searchButtonLabel: string;
  categorySectionAriaLabel: string;
  demoNote?: string;
  emptyHeading: string;
  onboarding: {
    eyebrow: string;
    heading: ReactNode;
    paragraph: string;
    buttonLabel: string;
    listItems: string[];
    footerLinkLabel: string;
    /** An in-app route that replaces the WhatsApp/demo-notice CTA below - see OnboardingSection. */
    ctaTo?: string;
  };
}

export function MarketplacePage({
  categories,
  config,
  kind,
  copy,
}: {
  categories: Category[];
  config: MarketplaceConfig;
  kind: string;
  copy: MarketplacePageCopy;
}) {
  // Explore Shops lists retailers, Explore Wholesaler lists wholesalers - never a mix.
  const businessType: BusinessType = kind === "wholesalers" ? "WHOLESALER" : "RETAILER";
  const listingSingular = config.listingSingular ?? "shop";
  const listingPlural = config.listingPlural ?? "shops";

  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [area, setArea] = useState("");
  const [pincode, setPincode] = useState("");
  const [discoveryResolved, setDiscoveryResolved] = useState(false);
  const [cityId, setCityId] = useState("");
  const [cityName, setCityName] = useState("");

  // Server-paginated, city/category-scoped listings - see the fetch effect below and
  // loadMoreListings. Area/search stay client-side over whatever's loaded so far (matchesFilters).
  const [listings, setListings] = useState<Business[]>([]);
  const [listingsPage, setListingsPage] = useState(0);
  // Bumped to bring the welcome dialog back up: remounting it makes it read the (now cleared) saved choice afresh.
  const [pickerKey, setPickerKey] = useState(0);
  const [hasMoreListings, setHasMoreListings] = useState(false);
  const [loadingListings, setLoadingListings] = useState(true);

  const [previewBusiness, setPreviewBusiness] = useState<Business | null>(null);
  const [previewProduct, setPreviewProduct] = useState<Product | null>(null);
  const [contactNoticeVisible, setContactNoticeVisible] = useState(false);
  const [ads, setAds] = useState<AdvertisementSlotsDto | null>(null);

  useEffect(() => {
    document.title = copy.pageTitle;
    document.querySelector('meta[name="description"]')?.setAttribute("content", copy.metaDescription);
  }, [copy.pageTitle, copy.metaDescription]);

  // One combined fetch for all 3 ad slots, rather than each component
  // fetching its own - fewer public-API calls against PublicApiRateLimiter's
  // shared per-IP budget.
  useEffect(() => {
    listAdvertisements()
      .then(setAds)
      .catch(() => setAds({ topCarousel: null, leftRail: null, rightRail: null }));
  }, []);

  // Re-fetches from page 0 whenever the city or category changes - gated on discoveryResolved
  // so this doesn't fire once for the pre-dialog "no city yet" state and again right after.
  // Area/search aren't sent to the server yet (see MarketplacePage's own step 7a-7c breakdown
  // in todo.md) - they still filter client-side below, just over a city-scoped page now
  // instead of the whole platform.
  useEffect(() => {
    if (!discoveryResolved) return;
    let cancelled = false;
    setLoadingListings(true);
    const categoryId = categories.find((c) => c.name === activeCategory)?.id;
    listMarketplaceListings({ categoryId, cityId: cityId || undefined, businessType, page: 0, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return;
        setListings(result.items.map(listingToBusiness));
        setListingsPage(0);
        setHasMoreListings(result.hasMore);
      })
      .finally(() => {
        if (!cancelled) setLoadingListings(false);
      });
    return () => {
      cancelled = true;
    };
  }, [discoveryResolved, cityId, activeCategory, categories, businessType]);

  function loadMoreListings() {
    const nextPage = listingsPage + 1;
    const categoryId = categories.find((c) => c.name === activeCategory)?.id;
    setLoadingListings(true);
    listMarketplaceListings({ categoryId, cityId: cityId || undefined, businessType, page: nextPage, size: PAGE_SIZE })
      .then((result) => {
        setListings((current) => [...current, ...result.items.map(listingToBusiness)]);
        setListingsPage(nextPage);
        setHasMoreListings(result.hasMore);
      })
      .finally(() => setLoadingListings(false));
  }

  // A slot shows its built-in default only once the ads request has answered AND that slot has no
  // real ad - never alongside one, and never in the gap while `ads` is still null (loading).
  const topAd = ads ? ads.topCarousel ?? DEFAULT_ADS.topCarousel : null;
  const leftAd = ads ? ads.leftRail ?? DEFAULT_ADS.leftRail : null;
  const rightAd = ads ? ads.rightRail ?? DEFAULT_ADS.rightRail : null;

  const areas = useMemo(() => uniqueSorted(listings.map((b) => b.area)), [listings]);
  const pincodes = useMemo(() => uniqueSorted(listings.map((b) => b.pincode)), [listings]);

  const filtered = useMemo(
    () => listings.filter((business) => matchesFilters(business, { search, area, pincode })),
    [listings, search, area, pincode],
  );

  // Mirrors the original's continuous save-on-every-render, gated until the
  // first-visit discovery flow (dialog or stored preference) has resolved.
  // Carries cityId/cityName along too, so a later area/category change here
  // doesn't silently erase the city the dialog captured.
  useEffect(() => {
    if (discoveryResolved) saveDiscoveryPreference(kind, { cityId, cityName, area, category: activeCategory });
  }, [discoveryResolved, kind, cityId, cityName, area, activeCategory]);

  // Same approach as the Feed page's location chip: forget the saved choice, then reopen the picker without reloading.
  function reopenCityPicker() {
    clearDiscoveryPreference(kind);
    setPickerKey((key) => key + 1);
  }

  function handleDiscoveryApply(selection: DiscoverySelection) {
    setCityId(selection.cityId ?? "");
    setCityName(selection.cityName ?? "");
    setArea(selection.area);
    setActiveCategory(selection.category);
    setDiscoveryResolved(true);
  }

  function resetFilters() {
    setActiveCategory("All");
    setSearch("");
    setArea("");
    setPincode("");
  }

  const resultCount = `${filtered.length} ${filtered.length === 1 ? listingSingular : listingPlural} to discover${
    activeCategory !== "All" ? ` · ${activeCategory}` : ""
  }`;

  return (
    <>
      <a className="skip" href="#marketplace">
        {copy.skipLinkLabel}
      </a>
      <TopStrip />
      <SiteHeader />
      <main>
        <AdCarousel ad={topAd} />
        <DiscoveryStrip
          heading={copy.heading}
          subheading={copy.subheading}
          searchAriaLabel={copy.searchAriaLabel}
          searchPlaceholder={copy.searchPlaceholder}
          searchButtonLabel={copy.searchButtonLabel}
          search={search}
          onSearchChange={setSearch}
          onSubmit={() => {}}
          cityName={cityName}
          onChangeCity={reopenCityPicker}
          areas={areas}
          pincodes={pincodes}
          area={area}
          pincode={pincode}
          onAreaChange={setArea}
          onPincodeChange={setPincode}
          onReset={resetFilters}
          resultCount={resultCount}
        />
        <section className="category-section" id="categories" aria-label={copy.categorySectionAriaLabel}>
          <div className="section-label">WHAT ARE YOU LOOKING FOR?</div>
          <CategoryList categories={categories} activeCategory={activeCategory} listingPlural={listingPlural} onSelect={setActiveCategory} />
        </section>
        <div className="market-layout" id="marketplace">
          <SideRailAd side="left" ad={leftAd} />
          {loadingListings && listings.length === 0 ? (
            <ShopGridSkeleton />
          ) : (
            <BusinessGrid
              businesses={filtered}
              demoNote={copy.demoNote}
              emptyHeading={copy.emptyHeading}
              onPreviewProduct={(business, product) => {
                setPreviewBusiness(business);
                setPreviewProduct(product);
              }}
              onResetFilters={resetFilters}
            />
          )}
          <SideRailAd side="right" ad={rightAd} />
        </div>
        {hasMoreListings && (
          <div className="load-more-row">
            <button type="button" className="secondary-button" onClick={loadMoreListings} disabled={loadingListings}>
              {loadingListings ? "Loading…" : "Load more shops"}
            </button>
          </div>
        )}
        <OnboardingSection
          eyebrow={copy.onboarding.eyebrow}
          heading={copy.onboarding.heading}
          paragraph={copy.onboarding.paragraph}
          buttonLabel={copy.onboarding.buttonLabel}
          listItems={copy.onboarding.listItems}
          adminWhatsapp={config.adminWhatsapp}
          submissionMessage={
            config.submissionMessage ||
            "Hello! I would like to list my business on Neighbourly.\nBusiness name:\nBusiness type:\nWhatsApp number:\nComplete address:\nArea:\nPincode:\nGoogle Maps location:\nI will also send shop and product photos."
          }
          onDemoContactClick={() => setContactNoticeVisible(true)}
          ctaTo={copy.onboarding.ctaTo}
        />
      </main>
      <SiteFooter listBusinessLabel={copy.onboarding.footerLinkLabel} />

      <LightboxDialog
        product={previewProduct}
        businessName={previewBusiness?.businessName ?? ""}
        onClose={() => {
          setPreviewProduct(null);
          setPreviewBusiness(null);
        }}
      />
      <ContactNoticeDialog visible={contactNoticeVisible} title="Business submissions open soon" onClose={() => setContactNoticeVisible(false)} />
      <DiscoveryWelcomeDialog key={pickerKey} categories={categories} kind={kind} onApply={handleDiscoveryApply} />
    </>
  );
}
