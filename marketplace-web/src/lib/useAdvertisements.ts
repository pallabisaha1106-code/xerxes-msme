import { useEffect, useState } from "react";
import { listAdvertisements, type AdvertisementDto } from "../api/marketplaceApi";
import { DEFAULT_ADS } from "../data/defaultAds";

/**
 * The three Super Admin-managed ad slots for a page, fetched once. A slot shows its built-in default only once the
 * request has answered AND that slot has no real ad - never alongside one, and never in the gap while the request is
 * still loading (null then, so the page can reserve the space). A failed request counts as "no real ads".
 */
export function useAdvertisements(): { topAd: AdvertisementDto | null; leftAd: AdvertisementDto | null; rightAd: AdvertisementDto | null } {
  const [ads, setAds] = useState<Awaited<ReturnType<typeof listAdvertisements>> | null>(null);

  useEffect(() => {
    listAdvertisements()
      .then(setAds)
      .catch(() => setAds({ topCarousel: null, leftRail: null, rightRail: null }));
  }, []);

  return {
    topAd: ads ? ads.topCarousel ?? DEFAULT_ADS.topCarousel : null,
    leftAd: ads ? ads.leftRail ?? DEFAULT_ADS.leftRail : null,
    rightAd: ads ? ads.rightRail ?? DEFAULT_ADS.rightRail : null,
  };
}
