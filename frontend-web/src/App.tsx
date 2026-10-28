import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { ToastProvider } from "./ui";
import { AppLayout } from "./layout/AppLayout";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CategoriesPage } from "./pages/catalog/CategoriesPage";
import { ProductsPage } from "./pages/catalog/ProductsPage";
import { NewProductPage } from "./pages/catalog/NewProductPage";
import { QuickAddPage } from "./pages/catalog/quickadd/QuickAddPage";
import { StarterCatalogPage } from "./pages/catalog/starter/StarterCatalogPage";
import { LabelsPage } from "./pages/catalog/LabelsPage";
import { RequestsPage } from "./pages/appointments/RequestsPage";
import { AvailabilityPage } from "./pages/appointments/AvailabilityPage";
import { StockPage } from "./pages/inventory/StockPage";
import { VendorsPage } from "./pages/purchase/VendorsPage";
import { PurchasesPage } from "./pages/purchase/PurchasesPage";
import { NewPurchasePage } from "./pages/purchase/NewPurchasePage";
import { CustomersPage } from "./pages/sales/CustomersPage";
import { InvoicesPage } from "./pages/sales/InvoicesPage";
import { NewInvoicePage } from "./pages/sales/NewInvoicePage";
import { StorefrontOrdersPage } from "./pages/sales/StorefrontOrdersPage";
import { StorefrontSitePage } from "./pages/storefront/StorefrontSitePage";
import { MarketplaceListingPage } from "./pages/marketplace/MarketplaceListingPage";
import { FeedPostsPage } from "./pages/marketplace/FeedPostsPage";
import { BillingPage } from "./pages/settings/BillingPage";
import { AdminLayout } from "./layout/AdminLayout";
import { AdminVendorsPage } from "./pages/admin/AdminVendorsPage";
import { AdminVendorDetailPage } from "./pages/admin/AdminVendorDetailPage";
import { AdminMarketplaceListingsPage } from "./pages/admin/AdminMarketplaceListingsPage";
import { AdminFeedPostsPage } from "./pages/admin/AdminFeedPostsPage";
import { AdminMarketplaceCategoriesPage } from "./pages/admin/AdminMarketplaceCategoriesPage";
import { AdminDefaultCatalogPage } from "./pages/admin/AdminDefaultCatalogPage";
import { AdminServiceLocationsPage } from "./pages/admin/AdminServiceLocationsPage";
import { AdminServiceStatesPage } from "./pages/admin/AdminServiceStatesPage";
import { AdminAdvertisementsPage } from "./pages/admin/AdminAdvertisementsPage";
import { PartnerProfilePage } from "./pages/admin/PartnerProfilePage";
import { AdminPartnerApplicationsPage } from "./pages/admin/AdminPartnerApplicationsPage";
import { AdminPartnersPage } from "./pages/admin/AdminPartnersPage";
import { AdminWaitingShopsPage } from "./pages/admin/AdminWaitingShopsPage";

// Lazy-loaded: pulls in the @zxing barcode-decoding library, which is sizable
// and only needed on this one page.
const QuickBillingPage = lazy(() =>
  import("./pages/sales/QuickBillingPage").then((m) => ({ default: m.QuickBillingPage })),
);

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route
              element={
                <ProtectedRoute mode="admin">
                  <AdminLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/admin/profile" element={<PartnerProfilePage />} />
              <Route path="/admin" element={<AdminVendorsPage />} />
              <Route path="/admin/vendors/:tenantId" element={<AdminVendorDetailPage />} />
              <Route path="/admin/marketplace/listings" element={<AdminMarketplaceListingsPage />} />
              <Route path="/admin/feed-posts" element={<AdminFeedPostsPage />} />
              <Route
                element={
                  <ProtectedRoute mode="superAdmin">
                    <Outlet />
                  </ProtectedRoute>
                }
              >
                <Route path="/admin/marketplace-categories" element={<AdminMarketplaceCategoriesPage />} />
                <Route path="/admin/default-catalog" element={<AdminDefaultCatalogPage />} />
                <Route path="/admin/service-locations" element={<AdminServiceLocationsPage />} />
                <Route path="/admin/service-states" element={<AdminServiceStatesPage />} />
                <Route path="/admin/advertisements" element={<AdminAdvertisementsPage />} />
                <Route path="/admin/partner-applications" element={<AdminPartnerApplicationsPage />} />
                <Route path="/admin/partners" element={<AdminPartnersPage />} />
                <Route path="/admin/partner-waiting" element={<AdminWaitingShopsPage />} />
              </Route>
            </Route>

            <Route
              element={
                <ProtectedRoute mode="tenant">
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<DashboardPage />} />
              <Route path="/categories" element={<CategoriesPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/products/new" element={<NewProductPage />} />
              <Route path="/products/from-photos" element={<QuickAddPage />} />
              <Route path="/products/from-catalogue" element={<StarterCatalogPage />} />
              <Route path="/products/:productId/labels" element={<LabelsPage />} />
              <Route path="/appointments" element={<RequestsPage />} />
              <Route path="/appointments/availability" element={<AvailabilityPage />} />
              <Route path="/stock" element={<StockPage />} />
              <Route path="/vendors" element={<VendorsPage />} />
              <Route path="/purchases" element={<PurchasesPage />} />
              <Route path="/purchases/new" element={<NewPurchasePage />} />
              <Route path="/customers" element={<CustomersPage />} />
              <Route path="/invoices" element={<InvoicesPage />} />
              <Route path="/invoices/new" element={<NewInvoicePage />} />
              <Route path="/storefront/orders" element={<StorefrontOrdersPage />} />
              <Route path="/storefront/builder" element={<StorefrontSitePage />} />
              <Route path="/marketplace/listing" element={<MarketplaceListingPage />} />
              <Route path="/marketplace/feed" element={<FeedPostsPage />} />
              <Route path="/settings/billing" element={<BillingPage />} />
              <Route
                path="/billing/quick"
                element={
                  <Suspense fallback={null}>
                    <QuickBillingPage />
                  </Suspense>
                }
              />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
