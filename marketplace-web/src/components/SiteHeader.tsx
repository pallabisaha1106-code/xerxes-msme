import { useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { clearDiscoveryPreference, loadDiscoveryPreference } from "../lib/discoveryStorage";

/** `cart` is an optional slot for the shop page's cart icon - every other page renders the header without it. */
export function SiteHeader({ cart }: { cart?: ReactNode }) {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const isWholesalers = location.pathname === "/wholesalers";
  const isFeed = location.pathname === "/";
  // A shop's own page belongs under Explore Shops; any other page (pricing, the application forms...) highlights nothing.
  const isExploreShops = location.pathname === "/shops" || location.pathname.startsWith("/shop/");
  const isPricing = location.pathname === "/pricing";

  // Explore Shops/Wholesaler are the only pages with the welcome dialog this reopens -
  // everywhere else (e.g. Feed) defaults to the "shops" kind's choice, if any.
  const discoveryKind = isWholesalers ? "wholesalers" : "shops";
  const cityLabel = loadDiscoveryPreference(discoveryKind)?.cityName || "Choose your city";

  function closeMenu() {
    setOpen(false);
  }

  function reopenCityPicker() {
    clearDiscoveryPreference(discoveryKind);
    // Reload the list page you are on, which is where the welcome dialog lives; from anywhere else go home.
    window.location.href = ["/", "/shops", "/wholesalers"].includes(location.pathname) ? location.pathname : "/";
  }

  return (
    <header>
      <div className="header-inner">
        <div className="brand-lockup">
          <Link className="brand" to="/">
            <span className="brand-icon">
              n<span>•</span>
            </span>
            neighbourly<span className="brand-dot">.</span>
          </Link>
          <p className="brand-tagline">YOUR NEIGHBOURHOOD. YOUR FAVORITE SHOPS.</p>
        </div>
        <nav id="navigation" aria-label="Main navigation" className={open ? "open" : ""}>
          <Link className={isFeed ? "active" : ""} aria-current={isFeed ? "page" : undefined} to="/" onClick={closeMenu}>
            <span aria-hidden="true">🔥</span> Trending
          </Link>
          <Link className={isExploreShops ? "active" : ""} aria-current={isExploreShops ? "page" : undefined} to="/shops" onClick={closeMenu}>
            Explore Shops
          </Link>
          <Link className={isWholesalers ? "active" : ""} aria-current={isWholesalers ? "page" : undefined} to="/wholesalers" onClick={closeMenu}>
            Explore Wholesaler
          </Link>
          <Link className={isPricing ? "active" : ""} aria-current={isPricing ? "page" : undefined} to="/pricing" onClick={closeMenu}>
            Pricing
          </Link>
          <a href="#list-business" onClick={closeMenu}>
            For business owners <span aria-hidden="true">↗</span>
          </a>
        </nav>
        <button type="button" className="header-location" onClick={reopenCityPicker}>
          ⌖ &nbsp;{cityLabel}
        </button>
        {cart}
        <button
          className="menu-button"
          aria-expanded={open}
          aria-controls="navigation"
          aria-label="Toggle navigation"
          onClick={() => setOpen((current) => !current)}
        >
          ☰
        </button>
      </div>
    </header>
  );
}
