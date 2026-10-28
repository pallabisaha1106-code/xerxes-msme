import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ToastProvider } from "./components/Toast";
import { ExploreShopsPage } from "./pages/ExploreShopsPage";
import { ExploreWholesalerPage } from "./pages/ExploreWholesalerPage";
import { PricingPage } from "./pages/PricingPage";
import { PartnerApplyPage } from "./pages/PartnerApplyPage";
import { FeedPage } from "./pages/FeedPage";
import { ShopProductPage } from "./pages/ShopProductPage";
import { LiveShopPage } from "./pages/LiveShopPage";
import { JoinLandingPage } from "./pages/JoinLandingPage";
import { VendorApplyPage } from "./pages/VendorApplyPage";
import { VendorCatalogueEntryPage } from "./pages/VendorCatalogueEntryPage";
import { VendorCataloguePage } from "./pages/VendorCataloguePage";

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<FeedPage />} />
          <Route path="/shops" element={<ExploreShopsPage />} />
          <Route path="/wholesalers" element={<ExploreWholesalerPage />} />
          {/* Trending is the home page now; the old address still works. Must stay above the greedy /:pageFile below. */}
          <Route path="/feed" element={<Navigate to="/" replace />} />
          <Route path="/pricing" element={<PricingPage />} />
          <Route path="/partners/apply" element={<PartnerApplyPage />} />
          <Route path="/list-your-business" element={<JoinLandingPage />} />
          <Route path="/list-your-business/apply" element={<VendorApplyPage />} />
          <Route path="/list-your-business/catalogue" element={<VendorCatalogueEntryPage />} />
          <Route path="/list-your-business/catalogue/:code" element={<VendorCataloguePage />} />
          {/* A real, API-backed listing's tenantId - see BusinessCard/liveMarketplace.ts. */}
          <Route path="/shop/:tenantId" element={<LiveShopPage />} />
          {/* Per-shop expanded catalogues, e.g. /veera-collection.html - the
              filename lives in that business's productPage.url in the data,
              exactly as it did as a standalone static page. */}
          <Route path="/:pageFile" element={<ShopProductPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
