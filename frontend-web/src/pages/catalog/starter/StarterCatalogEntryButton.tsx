import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../ui";
import { getMarketplaceListing } from "../../../api/marketplaceListingApi";
import { listStarterCatalog } from "../../../api/starterCatalogApi";

/**
 * The "Add from starter catalogue" button for the products page. Shown only to shops whose marketplace category has a
 * starter catalogue (Grocery and Ladies Wear today, and any category the Super Admin adds one for); everyone else, and
 * anyone whose category can't be read, sees nothing and the page is unchanged.
 */
export function StarterCatalogEntryButton() {
  const navigate = useNavigate();
  const [show, setShow] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getMarketplaceListing()
      .then((listing) => (listing.categoryId ? listStarterCatalog(listing.categoryId) : []))
      .then((catalog) => {
        if (!cancelled) setShow(catalog.length > 0);
      })
      .catch(() => {
        if (!cancelled) setShow(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!show) return null;
  return (
    <Button icon="plus" variant="outlined" onClick={() => navigate("/products/from-catalogue")}>
      Add from starter catalogue
    </Button>
  );
}
