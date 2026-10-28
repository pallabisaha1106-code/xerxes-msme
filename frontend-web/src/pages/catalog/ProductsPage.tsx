import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BulkImportControls, Button, Card, Dialog, Icon, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as productApi from "../../api/productApi";
import * as categoryApi from "../../api/categoryApi";
import type { ProductResponse, VariantResponse } from "../../api/productApi";
import type { CategoryResponse } from "../../api/categoryApi";
import type { BulkImportResult } from "../../api/bulkImport";
import { VariantFormDialog } from "./VariantFormDialog";
import { EditProductDialog } from "./EditProductDialog";
import { QuickAddEntryButton } from "./quickadd/QuickAddEntryButton";
import { StarterCatalogEntryButton } from "./starter/StarterCatalogEntryButton";

const SAMPLE_ROWS: string[][] = [
  [
    "Product Name",
    "Brand",
    "Category",
    "HSN Code",
    "GST Rate",
    "Default UOM",
    "SKU",
    "Barcode",
    "Size",
    "Color",
    "Purchase Price",
    "Selling Price",
    "Reorder Threshold",
  ],
  [
    "Men's Round Neck T-Shirt",
    "Xerxes Basics",
    "Men's Apparel",
    "6109",
    "5",
    "PCS",
    "TSHIRT-RN-M-BLU",
    "",
    "M",
    "Blue",
    "249",
    "499",
    "10",
  ],
  [
    "Men's Round Neck T-Shirt",
    "Xerxes Basics",
    "Men's Apparel",
    "6109",
    "5",
    "PCS",
    "TSHIRT-RN-L-BLU",
    "",
    "L",
    "Blue",
    "249",
    "499",
    "10",
  ],
  [
    "Men's Slim Fit Jeans",
    "Xerxes Denim",
    "Men's Apparel",
    "6203",
    "12",
    "PCS",
    "JEANS-SLIM-30-BLK",
    "",
    "30",
    "Black",
    "899",
    "1799",
    "8",
  ],
  [
    "Men's Slim Fit Jeans",
    "Xerxes Denim",
    "Men's Apparel",
    "6203",
    "12",
    "PCS",
    "JEANS-SLIM-32-BLK",
    "",
    "32",
    "Black",
    "899",
    "1799",
    "8",
  ],
  [
    "Women's Anarkali Kurta",
    "Xerxes Ethnic",
    "Women's Apparel",
    "6204",
    "12",
    "PCS",
    "KURTA-ANARK-S-PNK",
    "",
    "S",
    "Pink",
    "650",
    "1499",
    "5",
  ],
  [
    "Women's Anarkali Kurta",
    "Xerxes Ethnic",
    "Women's Apparel",
    "6204",
    "12",
    "PCS",
    "KURTA-ANARK-M-PNK",
    "",
    "M",
    "Pink",
    "650",
    "1499",
    "5",
  ],
];

function stockTag(variant: VariantResponse) {
  if (variant.currentStock <= 0) return <Tag severity="danger">OUT OF STOCK</Tag>;
  if (variant.currentStock <= variant.reorderThreshold) return <Tag severity="warning">LOW STOCK</Tag>;
  return <Tag severity="success">IN STOCK</Tag>;
}

function VariantsTable({
  variants,
  onEdit,
  onDelete,
}: {
  variants: VariantResponse[];
  onEdit: (variant: VariantResponse) => void;
  onDelete: (variant: VariantResponse) => void;
}) {
  const columns: Column<VariantResponse>[] = [
    { field: "sku", header: "SKU" },
    {
      field: "attributes",
      header: "Attributes",
      body: (v) =>
        Object.entries(v.attributes)
          .map(([k, val]) => `${k}: ${val}`)
          .join(", ") || "-",
    },
    { field: "sellingPrice", header: "Selling Price", body: (v) => `₹${v.sellingPrice.toFixed(2)}` },
    { field: "currentStock", header: "Stock", body: (v) => v.currentStock },
    {
      field: "status",
      header: "Status",
      body: (v) => (v.active ? stockTag(v) : <Tag severity="secondary">INACTIVE</Tag>),
    },
    {
      field: "actions",
      header: "",
      width: "5rem",
      body: (v) => (
        <div style={{ display: "flex", gap: "0.35rem" }}>
          <button type="button" className="table-icon-btn" onClick={() => onEdit(v)} aria-label={`Edit ${v.sku}`}>
            <Icon name="edit" size={15} />
          </button>
          <button
            type="button"
            className="table-icon-btn table-icon-btn-danger"
            onClick={() => onDelete(v)}
            disabled={!v.active}
            aria-label={`Delete ${v.sku}`}
          >
            <Icon name="trash" size={15} />
          </button>
        </div>
      ),
    },
  ];

  return <Table data={variants} columns={columns} rowKey={(v) => v.id} emptyMessage="No variants." />;
}

export function ProductsPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState<ProductResponse | null>(null);
  const [variantFormOpen, setVariantFormOpen] = useState(false);
  const [editProductOpen, setEditProductOpen] = useState(false);
  const [editingVariant, setEditingVariant] = useState<VariantResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<VariantResponse | null>(null);
  const [deleting, setDeleting] = useState(false);

  function applyProductUpdate(product: ProductResponse) {
    setProducts((current) => current.map((p) => (p.id === product.id ? product : p)));
    setSelectedProduct(product);
  }

  function openAddVariant() {
    setEditingVariant(null);
    setVariantFormOpen(true);
  }

  function openEditVariant(variant: VariantResponse) {
    setEditingVariant(variant);
    setVariantFormOpen(true);
  }

  async function handleConfirmDelete() {
    if (!deleteTarget || !selectedProduct) return;
    setDeleting(true);
    try {
      const product = await productApi.deleteVariant(selectedProduct.id, deleteTarget.id);
      const wasHardDeleted = !product.variants.some((v) => v.id === deleteTarget.id);
      applyProductUpdate(product);
      toast.show({
        severity: "success",
        summary: wasHardDeleted ? "Variant deleted" : "Variant deactivated",
        detail: wasHardDeleted
          ? deleteTarget.sku
          : `${deleteTarget.sku} has transaction history, so it was deactivated instead of deleted.`,
      });
      setDeleteTarget(null);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not delete variant", detail: extractErrorMessage(err) });
    } finally {
      setDeleting(false);
    }
  }

  async function loadAll() {
    setLoading(true);
    try {
      const [productList, categoryList] = await Promise.all([
        productApi.listProducts(),
        categoryApi.listCategories(),
      ]);
      setProducts(productList);
      setCategories(categoryList);
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load products", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * The CSV is one row per variant (Size/Color are the two attributes an
   * apparel shop needs); rows sharing the same "Product Name" are grouped
   * into one product with multiple variants. Category names that don't
   * already exist are created on the fly. Because several CSV rows can
   * collapse into a single bulk-import request, any error rowIndex the
   * backend reports (which product failed) is remapped back to that
   * product's *first* CSV row so BulkImportControls' "Row N" display still
   * points somewhere useful in the file the user uploaded.
   */
  async function handleBulkUpload(dataRows: string[][]): Promise<BulkImportResult> {
    const order: string[] = [];
    const groups = new Map<string, { firstRowIndex: number; rows: string[][] }>();

    dataRows.forEach((row, index) => {
      const name = (row[0] ?? "").trim();
      if (!name) return;
      if (!groups.has(name)) {
        groups.set(name, { firstRowIndex: index, rows: [] });
        order.push(name);
      }
      groups.get(name)!.rows.push(row);
    });

    const categoryIdByName = new Map<string, string>();
    for (const category of categories) {
      categoryIdByName.set(category.name.toLowerCase(), category.id);
    }

    async function resolveCategoryId(rawName: string): Promise<string | undefined> {
      const name = rawName.trim();
      if (!name) return undefined;
      const key = name.toLowerCase();
      const existing = categoryIdByName.get(key);
      if (existing) return existing;
      const created = await categoryApi.createCategory({ name });
      categoryIdByName.set(key, created.id);
      return created.id;
    }

    const requests: productApi.ProductRequest[] = [];
    const groupFirstRowIndexes: number[] = [];

    for (const name of order) {
      const group = groups.get(name)!;
      const firstRow = group.rows[0];
      const categoryId = await resolveCategoryId(firstRow[2] ?? "");

      requests.push({
        name,
        brand: firstRow[1]?.trim() || undefined,
        categoryId,
        defaultHsnCode: firstRow[3]?.trim() || undefined,
        defaultGstRate: Number(firstRow[4] || 0),
        defaultUom: firstRow[5]?.trim() || "PCS",
        variants: group.rows.map((row) => {
          const attributes: Record<string, string> = {};
          if (row[8]?.trim()) attributes["Size"] = row[8].trim();
          if (row[9]?.trim()) attributes["Color"] = row[9].trim();
          return {
            sku: (row[6] ?? "").trim(),
            barcode: row[7]?.trim() || undefined,
            attributes,
            purchasePrice: Number(row[10] || 0),
            sellingPrice: Number(row[11] || 0),
            reorderThreshold: Number(row[12] || 0),
          };
        }),
      });
      groupFirstRowIndexes.push(group.firstRowIndex);
    }

    const result = await productApi.bulkCreateProducts(requests);

    return {
      ...result,
      errors: result.errors.map((error) => ({
        rowIndex: groupFirstRowIndexes[error.rowIndex] ?? error.rowIndex,
        message: error.message,
      })),
    };
  }

  const columns: Column<ProductResponse>[] = [
    {
      field: "name",
      header: "Name",
      sortable: true,
      body: (p) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => setSelectedProduct(p)}>
          {p.name}
        </Button>
      ),
    },
    { field: "brand", header: "Brand", body: (p) => p.brand ?? "-" },
    { field: "defaultHsnCode", header: "HSN Code", body: (p) => p.defaultHsnCode ?? "-" },
    { field: "defaultGstRate", header: "GST Rate", body: (p) => `${p.defaultGstRate}%` },
    { field: "variants", header: "Variants", body: (p) => p.variants.length },
  ];

  return (
    <Card
      title="Products"
      actions={
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <BulkImportControls
            sampleFilename="products-sample.csv"
            sampleRows={SAMPLE_ROWS}
            onUpload={handleBulkUpload}
            onImportComplete={loadAll}
          />
          <QuickAddEntryButton />
          <StarterCatalogEntryButton />
          <Button icon="plus" severity="success" onClick={() => navigate("/products/new")}>
            New Product
          </Button>
        </div>
      }
    >
      <Table
        data={products}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No products yet. Create your first one."
      />
      <Dialog
        visible={selectedProduct != null}
        onHide={() => setSelectedProduct(null)}
        title={selectedProduct ? `Variants — ${selectedProduct.name}` : "Variants"}
        width={900}
      >
        {selectedProduct && (
          <>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem", marginBottom: "0.75rem" }}>
              <Button icon="edit" size="sm" variant="outlined" onClick={() => setEditProductOpen(true)}>
                Edit Product
              </Button>
              <Button
                icon="printer"
                size="sm"
                variant="outlined"
                onClick={() => navigate(`/products/${selectedProduct.id}/labels`)}
              >
                Print Labels
              </Button>
              <Button icon="plus" size="sm" variant="outlined" onClick={openAddVariant}>
                Add Variant
              </Button>
            </div>
            <VariantsTable
              variants={selectedProduct.variants}
              onEdit={openEditVariant}
              onDelete={setDeleteTarget}
            />
          </>
        )}
      </Dialog>

      {selectedProduct && (
        <VariantFormDialog
          visible={variantFormOpen}
          onHide={() => setVariantFormOpen(false)}
          productId={selectedProduct.id}
          variant={editingVariant}
          onSaved={applyProductUpdate}
        />
      )}

      <EditProductDialog
        visible={editProductOpen}
        onHide={() => setEditProductOpen(false)}
        product={selectedProduct}
        categories={categories}
        onSaved={applyProductUpdate}
      />

      <Dialog
        visible={deleteTarget != null}
        onHide={() => setDeleteTarget(null)}
        title="Delete Variant"
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button severity="danger" loading={deleting} onClick={handleConfirmDelete}>
              Delete
            </Button>
          </>
        }
      >
        <p>
          Delete variant <strong>{deleteTarget?.sku}</strong>? If it has purchase or sale history, it will be
          deactivated instead of removed.
        </p>
      </Dialog>
    </Card>
  );
}
