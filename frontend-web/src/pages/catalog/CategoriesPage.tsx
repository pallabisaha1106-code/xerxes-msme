import { useEffect, useState } from "react";
import { BulkImportControls, Button, Card, Dialog, Input, Select, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as categoryApi from "../../api/categoryApi";
import type { CategoryResponse } from "../../api/categoryApi";

const SAMPLE_ROWS: string[][] = [
  ["Name"],
  ["Men's Apparel"],
  ["Women's Apparel"],
  ["Footwear"],
  ["Accessories"],
];

export function CategoriesPage() {
  const toast = useToast();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogVisible, setDialogVisible] = useState(false);
  const [name, setName] = useState("");
  const [parentCategoryId, setParentCategoryId] = useState("");
  const [saving, setSaving] = useState(false);

  async function loadCategories() {
    setLoading(true);
    try {
      setCategories(await categoryApi.listCategories());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load categories", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function openDialog() {
    setName("");
    setParentCategoryId("");
    setDialogVisible(true);
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await categoryApi.createCategory({ name, parentCategoryId: parentCategoryId || null });
      toast.show({ severity: "success", summary: "Category created", detail: name });
      setDialogVisible(false);
      await loadCategories();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create category", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  function parentName(id: string | null): string {
    if (!id) return "-";
    return categories.find((c) => c.id === id)?.name ?? "-";
  }

  const columns: Column<CategoryResponse>[] = [
    { field: "name", header: "Name", sortable: true },
    {
      field: "parentCategoryId",
      header: "Parent Category",
      body: (row) => parentName(row.parentCategoryId),
    },
  ];

  async function handleBulkUpload(dataRows: string[][]) {
    const requests = dataRows.map((row) => ({ name: (row[0] ?? "").trim() }));
    return categoryApi.bulkCreateCategories(requests);
  }

  return (
    <Card
      title="Categories"
      actions={
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <BulkImportControls
            sampleFilename="categories-sample.csv"
            sampleRows={SAMPLE_ROWS}
            onUpload={handleBulkUpload}
            onImportComplete={loadCategories}
          />
          <Button icon="plus" severity="success" onClick={openDialog}>
            New Category
          </Button>
        </div>
      }
    >
      <Table
        data={categories}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No categories yet. Create your first one."
      />

      <Dialog
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        title="New Category"
        footer={
          <>
            <Button variant="text" severity="secondary" onClick={() => setDialogVisible(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate} loading={saving}>
              Save
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreate}>
          <Input id="category-name" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
          <Select
            id="category-parent"
            label="Parent category (optional)"
            placeholder="None (top-level)"
            value={parentCategoryId}
            onChange={(e) => setParentCategoryId(e.target.value)}
            options={categories.map((c) => ({ label: c.name, value: c.id }))}
          />
        </form>
      </Dialog>
    </Card>
  );
}
