import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../ui";
import { getProductImageRules } from "../../../api/productApi";

/**
 * The "Add from photos" button for the products page. Shown only to shops whose photo rules are the detailed ones
 * (clothing, footwear, jewellery, ...), the shops this flow is built for; everyone else, and anyone whose rules
 * can't be read, sees nothing and the page is unchanged. Fetches the rules itself so the products page needs no
 * other change.
 */
export function QuickAddEntryButton() {
  const navigate = useNavigate();
  const [show, setShow] = useState(false);

  useEffect(() => {
    getProductImageRules()
      .then((rules) => setShow(rules.profile === "DETAIL"))
      .catch(() => setShow(false));
  }, []);

  if (!show) return null;
  return (
    <Button icon="plus" variant="outlined" onClick={() => navigate("/products/from-photos")}>
      Add from photos
    </Button>
  );
}
