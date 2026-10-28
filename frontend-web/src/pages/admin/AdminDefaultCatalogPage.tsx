import { useEffect, useState } from "react";
import { Button, Card, Dialog, ImagePicker, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import { resolveImagePath } from "../../lib/mediaUrl";
import { listMarketplaceCategories, type MarketplaceCategory } from "../../api/marketplaceListingApi";
import * as adminDefaultCatalogApi from "../../api/adminDefaultCatalogApi";
import type { AdminDefaultProduct, AdminDefaultProductCategory } from "../../api/adminDefaultCatalogApi";

interface CategoryFormState {
  name: string;
  displayOrder: string;
  active: boolean;
}

interface ProductFormState {
  name: string;
  displayOrder: string;
  active: boolean;
}

export function AdminDefaultCatalogPage() {
  const toast = useToast();
  const [marketplaceCategories, setMarketplaceCategories] = useState<MarketplaceCategory[]>([]);
  const [marketplaceCategoryId, setMarketplaceCategoryId] = useState("");

  const [subCategories, setSubCategories] = useState<AdminDefaultProductCategory[]>([]);
  const [subCategoriesLoading, setSubCategoriesLoading] = useState(false);
  const [selectedSubCategoryId, setSelectedSubCategoryId] = useState<string | null>(null);

  const [products, setProducts] = useState<AdminDefaultProduct[]>([]);
  const [productsLoading, setProductsLoading] = useState(false);

  const [categoryDialog, setCategoryDialog] = useState<{ editing: AdminDefaultProductCategory | null } | null>(null);
  const [categoryForm, setCategoryForm] = useState<CategoryFormState>({ name: "", displayOrder: "0", active: true });
  const [categorySaving, setCategorySaving] = useState(false);

  const [productDialog, setProductDialog] = useState<{ editing: AdminDefaultProduct | null } | null>(null);
  const [productForm, setProductForm] = useState<ProductFormState>({ name: "", displayOrder: "0", active: true });
  const [productSaving, setProductSaving] = useState(false);
  const [productPhotoUploading, setProductPhotoUploading] = useState(false);
  const [pendingProductPhoto, setPendingProductPhoto] = useState<File | null>(null);
  const [productPickerKey, setProductPickerKey] = useState(0);

  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importManifestFile, setImportManifestFile] = useState<File | null>(null);
  const [importImageFiles, setImportImageFiles] = useState<File[]>([]);
  const [importSaving, setImportSaving] = useState(false);
  const [importResult, setImportResult] = useState<adminDefaultCatalogApi.ImportDefaultCatalogResult | null>(null);

  useEffect(() => {
    listMarketplaceCategories()
      .then((categories) => {
        setMarketplaceCategories(categories);
        if (categories.length > 0) setMarketplaceCategoryId(categories[0].id);
      })
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load categories", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadSubCategories(categoryId: string) {
    setSubCategoriesLoading(true);
    setSelectedSubCategoryId(null);
    setProducts([]);
    try {
      setSubCategories(await adminDefaultCatalogApi.listDefaultCategories(categoryId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load sub-categories", detail: extractErrorMessage(err) });
    } finally {
      setSubCategoriesLoading(false);
    }
  }

  useEffect(() => {
    if (marketplaceCategoryId) loadSubCategories(marketplaceCategoryId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [marketplaceCategoryId]);

  async function loadProducts(subCategoryId: string) {
    setProductsLoading(true);
    try {
      setProducts(await adminDefaultCatalogApi.listDefaultProducts(subCategoryId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load products", detail: extractErrorMessage(err) });
    } finally {
      setProductsLoading(false);
    }
  }

  function selectSubCategory(subCategory: AdminDefaultProductCategory) {
    setSelectedSubCategoryId(subCategory.id);
    loadProducts(subCategory.id);
  }

  function openCategoryDialog(editing: AdminDefaultProductCategory | null) {
    setCategoryForm(
      editing
        ? { name: editing.name, displayOrder: String(editing.displayOrder), active: editing.active }
        : { name: "", displayOrder: "0", active: true },
    );
    setCategoryDialog({ editing });
  }

  async function handleSaveCategory() {
    if (!marketplaceCategoryId) return;
    setCategorySaving(true);
    try {
      if (categoryDialog?.editing) {
        await adminDefaultCatalogApi.updateDefaultCategory(
          categoryDialog.editing.id,
          categoryForm.name,
          Number(categoryForm.displayOrder) || 0,
          categoryForm.active,
        );
      } else {
        await adminDefaultCatalogApi.createDefaultCategory(
          marketplaceCategoryId,
          categoryForm.name,
          Number(categoryForm.displayOrder) || 0,
        );
      }
      toast.show({ severity: "success", summary: "Sub-category saved" });
      setCategoryDialog(null);
      await loadSubCategories(marketplaceCategoryId);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save sub-category", detail: extractErrorMessage(err) });
    } finally {
      setCategorySaving(false);
    }
  }

  function openProductDialog(editing: AdminDefaultProduct | null) {
    setProductForm(
      editing
        ? { name: editing.name, displayOrder: String(editing.displayOrder), active: editing.active }
        : { name: "", displayOrder: "0", active: true },
    );
    setPendingProductPhoto(null);
    setProductPickerKey((k) => k + 1);
    setProductDialog({ editing });
  }

  async function handleSaveProduct() {
    if (!selectedSubCategoryId) return;
    setProductSaving(true);
    try {
      if (productDialog?.editing) {
        await adminDefaultCatalogApi.updateDefaultProduct(
          productDialog.editing.id,
          productForm.name,
          Number(productForm.displayOrder) || 0,
          productForm.active,
        );
      } else {
        await adminDefaultCatalogApi.createDefaultProduct(
          selectedSubCategoryId,
          productForm.name,
          Number(productForm.displayOrder) || 0,
        );
      }
      toast.show({ severity: "success", summary: "Product saved" });
      setProductDialog(null);
      await loadProducts(selectedSubCategoryId);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save product", detail: extractErrorMessage(err) });
    } finally {
      setProductSaving(false);
    }
  }

  async function handleUploadProductPhoto() {
    if (!pendingProductPhoto || !productDialog?.editing || !selectedSubCategoryId) return;
    setProductPhotoUploading(true);
    try {
      const updated = await adminDefaultCatalogApi.uploadDefaultProductPhoto(productDialog.editing.id, pendingProductPhoto);
      setProductDialog({ editing: updated });
      setPendingProductPhoto(null);
      setProductPickerKey((k) => k + 1);
      toast.show({ severity: "success", summary: "Photo uploaded" });
      await loadProducts(selectedSubCategoryId);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not upload photo", detail: extractErrorMessage(err) });
    } finally {
      setProductPhotoUploading(false);
    }
  }

  function openImportDialog() {
    setImportManifestFile(null);
    setImportImageFiles([]);
    setImportResult(null);
    setImportDialogOpen(true);
  }

  async function handleImport() {
    if (!marketplaceCategoryId || !importManifestFile) return;

    let categories: adminDefaultCatalogApi.ImportDefaultCatalogCategory[];
    try {
      const parsed = JSON.parse(await importManifestFile.text());
      if (!Array.isArray(parsed.categories)) throw new Error("missing categories array");
      categories = parsed.categories;
    } catch {
      toast.show({
        severity: "error",
        summary: "Invalid manifest file",
        detail: 'Must be JSON with a top-level "categories" array - see the format note below.',
      });
      return;
    }

    setImportSaving(true);
    try {
      const result = await adminDefaultCatalogApi.importDefaultCatalog({ marketplaceCategoryId, categories }, importImageFiles);
      setImportResult(result);
      toast.show({
        severity: "success",
        summary: "Catalog imported",
        detail: `${result.categoriesCreated + result.categoriesUpdated} categories, ${result.productsCreated + result.productsUpdated} products, ${result.imagesAttached} images`,
      });
      await loadSubCategories(marketplaceCategoryId);
    } catch (err) {
      toast.show({ severity: "error", summary: "Import failed", detail: extractErrorMessage(err) });
    } finally {
      setImportSaving(false);
    }
  }

  const subCategoryColumns: Column<AdminDefaultProductCategory>[] = [
    {
      field: "name",
      header: "Sub-category",
      body: (c) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => selectSubCategory(c)}>
          {c.name}
        </Button>
      ),
    },
    { field: "displayOrder", header: "Order" },
    { field: "active", header: "Status", body: (c) => (c.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">INACTIVE</Tag>) },
    {
      field: "actions",
      header: "",
      body: (c) => (
        <Button size="sm" variant="outlined" severity="secondary" onClick={() => openCategoryDialog(c)}>
          Edit
        </Button>
      ),
    },
  ];

  const productColumns: Column<AdminDefaultProduct>[] = [
    {
      field: "imageUrl",
      header: "Photo",
      body: (p) =>
        p.imageUrl ? (
          <img
            src={resolveImagePath(p.imageUrl)}
            alt={p.name}
            style={{ width: 32, height: 32, objectFit: "cover", borderRadius: 4 }}
          />
        ) : (
          <span className="field-hint">—</span>
        ),
    },
    { field: "name", header: "Product" },
    { field: "displayOrder", header: "Order" },
    { field: "active", header: "Status", body: (p) => (p.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">INACTIVE</Tag>) },
    {
      field: "actions",
      header: "",
      body: (p) => (
        <Button size="sm" variant="outlined" severity="secondary" onClick={() => openProductDialog(p)}>
          Edit
        </Button>
      ),
    },
  ];

  const selectedSubCategory = subCategories.find((c) => c.id === selectedSubCategoryId) ?? null;

  return (
    <>
      <Card
        title="Starter Catalog — Sub-categories"
        actions={
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Select
              id="marketplace-category"
              value={marketplaceCategoryId}
              onChange={(e) => setMarketplaceCategoryId(e.target.value)}
              options={marketplaceCategories.map((c) => ({ label: `${c.icon} ${c.name}`, value: c.id }))}
            />
            <Button size="sm" onClick={() => openCategoryDialog(null)} disabled={!marketplaceCategoryId}>
              Add Sub-category
            </Button>
            <Button size="sm" variant="outlined" onClick={openImportDialog} disabled={!marketplaceCategoryId}>
              Bulk Import
            </Button>
          </div>
        }
      >
        <Table
          data={subCategories}
          columns={subCategoryColumns}
          rowKey={(row) => row.id}
          loading={subCategoriesLoading}
          emptyMessage="No sub-categories yet for this marketplace category."
        />
      </Card>

      <Dialog
        visible={selectedSubCategory != null}
        onHide={() => setSelectedSubCategoryId(null)}
        title={`Products under ${selectedSubCategory?.name ?? ""}`}
        width={720}
        footer={
          <Button variant="outlined" severity="secondary" onClick={() => setSelectedSubCategoryId(null)}>
            Close
          </Button>
        }
      >
        <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "1rem" }}>
          <Button size="sm" onClick={() => openProductDialog(null)}>
            Add Product
          </Button>
        </div>
        <Table
          data={products}
          columns={productColumns}
          rowKey={(row) => row.id}
          loading={productsLoading}
          emptyMessage="No default products yet under this sub-category."
        />
      </Dialog>

      <Dialog
        visible={categoryDialog != null}
        onHide={() => setCategoryDialog(null)}
        title={categoryDialog?.editing ? "Edit Sub-category" : "Add Sub-category"}
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setCategoryDialog(null)}>
              Cancel
            </Button>
            <Button loading={categorySaving} onClick={handleSaveCategory}>
              Save
            </Button>
          </>
        }
      >
        <Input
          id="category-name"
          label="Name"
          value={categoryForm.name}
          onChange={(e) => setCategoryForm((f) => ({ ...f, name: e.target.value }))}
        />
        <Input
          id="category-display-order"
          label="Display order"
          type="number"
          value={categoryForm.displayOrder}
          onChange={(e) => setCategoryForm((f) => ({ ...f, displayOrder: e.target.value }))}
        />
        {categoryDialog?.editing && (
          <Select
            id="category-active"
            label="Status"
            value={categoryForm.active ? "true" : "false"}
            onChange={(e) => setCategoryForm((f) => ({ ...f, active: e.target.value === "true" }))}
            options={[
              { label: "Active", value: "true" },
              { label: "Inactive", value: "false" },
            ]}
          />
        )}
      </Dialog>

      <Dialog
        visible={productDialog != null}
        onHide={() => setProductDialog(null)}
        title={productDialog?.editing ? "Edit Product" : "Add Product"}
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setProductDialog(null)}>
              Cancel
            </Button>
            <Button loading={productSaving} onClick={handleSaveProduct}>
              Save
            </Button>
          </>
        }
      >
        <Input
          id="product-name"
          label="Name"
          value={productForm.name}
          onChange={(e) => setProductForm((f) => ({ ...f, name: e.target.value }))}
        />
        <Input
          id="product-display-order"
          label="Display order"
          type="number"
          value={productForm.displayOrder}
          onChange={(e) => setProductForm((f) => ({ ...f, displayOrder: e.target.value }))}
        />
        {productDialog?.editing && (
          <Select
            id="product-active"
            label="Status"
            value={productForm.active ? "true" : "false"}
            onChange={(e) => setProductForm((f) => ({ ...f, active: e.target.value === "true" }))}
            options={[
              { label: "Active", value: "true" },
              { label: "Inactive", value: "false" },
            ]}
          />
        )}
        {productDialog?.editing && (
          <div className="field">
            <label className="field-label">Photo</label>
            <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
              {productDialog.editing.imageUrl ? (
                <img
                  src={resolveImagePath(productDialog.editing.imageUrl)}
                  alt={productDialog.editing.name}
                  style={{ width: 64, height: 64, objectFit: "cover", borderRadius: 4 }}
                />
              ) : (
                <span className="field-hint">No photo yet</span>
              )}
            </div>
            <ImagePicker
              key={productPickerKey}
              id="product-photo-file"
              rules={{ maxBytes: 10 * 1024 * 1024 }}
              disabled={productPhotoUploading}
              onChange={setPendingProductPhoto}
            />
            <Button
              type="button"
              variant="outlined"
              size="sm"
              loading={productPhotoUploading}
              disabled={!pendingProductPhoto}
              onClick={handleUploadProductPhoto}
            >
              Upload photo
            </Button>
          </div>
        )}
      </Dialog>

      <Dialog
        visible={importDialogOpen}
        onHide={() => setImportDialogOpen(false)}
        title="Bulk Import Starter Catalog"
        width={520}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setImportDialogOpen(false)}>
              Close
            </Button>
            <Button loading={importSaving} disabled={!importManifestFile} onClick={handleImport}>
              Import
            </Button>
          </>
        }
      >
        <p className="field-hint">
          Imports into{" "}
          <strong>{marketplaceCategories.find((c) => c.id === marketplaceCategoryId)?.name ?? "the selected category"}</strong> above.
          Upserts by name, so re-running the same manifest is safe.
        </p>
        <div className="field">
          <label className="field-label">Manifest (.json)</label>
          <input
            type="file"
            accept="application/json"
            onChange={(e) => setImportManifestFile(e.target.files?.[0] ?? null)}
          />
          <p className="field-hint">
            {'{ "categories": [{ "name": "Rice", "products": [{ "name": "Kohinoor Basmati Rice", "imageFileName": "groc-1-1.jpg" }] }] }'}
          </p>
        </div>
        <div className="field">
          <label className="field-label">Product images (optional)</label>
          <input
            type="file"
            accept="image/*"
            multiple
            onChange={(e) => setImportImageFiles(e.target.files ? Array.from(e.target.files) : [])}
          />
          <p className="field-hint">
            {importImageFiles.length > 0
              ? `${importImageFiles.length} file(s) selected - matched to products by their imageFileName.`
              : "Select every image referenced by imageFileName in the manifest."}
          </p>
        </div>
        {importResult && (
          <p className="field-hint">
            Last run: {importResult.categoriesCreated} categories created, {importResult.categoriesUpdated} updated;{" "}
            {importResult.productsCreated} products created, {importResult.productsUpdated} updated;{" "}
            {importResult.imagesAttached} images attached.
          </p>
        )}
        {importResult && importResult.skippedImages.length > 0 && (
          <div className="field">
            <span className="field-error">{importResult.skippedImages.length} image(s) skipped:</span>
            <ul className="field-hint">
              {importResult.skippedImages.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          </div>
        )}
      </Dialog>
    </>
  );
}
