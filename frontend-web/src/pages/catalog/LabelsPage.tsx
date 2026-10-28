import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import JsBarcode from "jsbarcode";
import { Button, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import type { ProductResponse, VariantResponse } from "../../api/productApi";
import "./labels.css";

function BarcodeLabel({ product, variant }: { product: ProductResponse; variant: VariantResponse }) {
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    if (!svgRef.current || !variant.barcode) return;
    JsBarcode(svgRef.current, variant.barcode, {
      format: "CODE128",
      width: 1.6,
      height: 40,
      displayValue: false,
      margin: 0,
    });
  }, [variant.barcode]);

  const attributeText = Object.entries(variant.attributes)
    .map(([key, value]) => `${key}: ${value}`)
    .join(", ");

  return (
    <div className="barcode-label">
      <div className="barcode-label-name">{product.name}</div>
      {attributeText && <div className="barcode-label-attrs">{attributeText}</div>}
      <svg ref={svgRef} className="barcode-label-svg" />
      <div className="barcode-label-code">{variant.barcode}</div>
      <div className="barcode-label-price">₹{variant.sellingPrice.toFixed(2)}</div>
    </div>
  );
}

export function LabelsPage() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!productId) return;
    productApi
      .getProduct(productId)
      .then(setProduct)
      .catch((err) => {
        toast.show({ severity: "error", summary: "Failed to load product", detail: extractErrorMessage(err) });
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productId]);

  useEffect(() => {
    document.body.classList.add("print-page");
    return () => document.body.classList.remove("print-page");
  }, []);

  if (loading) return null;
  if (!product) return null;

  const printableVariants = product.variants.filter((v) => v.active);

  return (
    <div>
      <div className="labels-toolbar no-print">
        <Button variant="outlined" severity="secondary" onClick={() => navigate("/products")}>
          Back to Products
        </Button>
        <Button icon="printer" onClick={() => window.print()}>
          Print Labels
        </Button>
      </div>

      {printableVariants.length === 0 ? (
        <p className="no-print">This product has no active variants to print labels for.</p>
      ) : (
        <div className="labels-grid">
          {printableVariants.map((variant) => (
            <BarcodeLabel key={variant.id} product={product} variant={variant} />
          ))}
        </div>
      )}
    </div>
  );
}
