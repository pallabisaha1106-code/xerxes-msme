import { useEffect, useState, type FormEvent } from "react";
import { BulkImportControls, Button, Card, Dialog, Input, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as vendorApi from "../../api/vendorApi";
import type { VendorResponse } from "../../api/vendorApi";

const SAMPLE_ROWS: string[][] = [
  ["Name", "GSTIN", "State Code", "Contact Person", "Phone", "Email", "Address", "City", "Pincode"],
  [
    "Surat Textile Mills",
    "24AAAPS1234C1Z5",
    "24",
    "Rajesh Patel",
    "9825012345",
    "sales@surattextiles.example",
    "Ring Road Industrial Area",
    "Surat",
    "395002",
  ],
  [
    "Tirupur Knitwear Exports",
    "33AAAPT5678D1Z2",
    "33",
    "Muthu Kumar",
    "9840098765",
    "export@tirupurknits.example",
    "SIDCO Industrial Estate",
    "Tirupur",
    "641604",
  ],
];

const initialForm = {
  name: "",
  gstin: "",
  stateCode: "",
  contactPerson: "",
  phone: "",
  email: "",
  addressLine1: "",
  city: "",
  pincode: "",
};

export function VendorsPage() {
  const toast = useToast();
  const [vendors, setVendors] = useState<VendorResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogVisible, setDialogVisible] = useState(false);
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);

  async function loadVendors() {
    setLoading(true);
    try {
      setVendors(await vendorApi.listVendors());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load vendors", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadVendors();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function update<K extends keyof typeof initialForm>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function openDialog() {
    setForm(initialForm);
    setDialogVisible(true);
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await vendorApi.createVendor({
        name: form.name,
        gstin: form.gstin || undefined,
        stateCode: form.stateCode || undefined,
        contactPerson: form.contactPerson || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        addressLine1: form.addressLine1 || undefined,
        city: form.city || undefined,
        pincode: form.pincode || undefined,
      });
      toast.show({ severity: "success", summary: "Vendor created", detail: form.name });
      setDialogVisible(false);
      await loadVendors();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create vendor", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  const columns: Column<VendorResponse>[] = [
    { field: "name", header: "Name", sortable: true },
    { field: "gstin", header: "GSTIN", body: (v) => v.gstin ?? "-" },
    { field: "contactPerson", header: "Contact", body: (v) => v.contactPerson ?? "-" },
    { field: "phone", header: "Phone", body: (v) => v.phone ?? "-" },
    { field: "city", header: "City", body: (v) => v.city ?? "-" },
  ];

  async function handleBulkUpload(dataRows: string[][]) {
    const requests = dataRows.map((row) => ({
      name: (row[0] ?? "").trim(),
      gstin: row[1]?.trim() || undefined,
      stateCode: row[2]?.trim() || undefined,
      contactPerson: row[3]?.trim() || undefined,
      phone: row[4]?.trim() || undefined,
      email: row[5]?.trim() || undefined,
      addressLine1: row[6]?.trim() || undefined,
      city: row[7]?.trim() || undefined,
      pincode: row[8]?.trim() || undefined,
    }));
    return vendorApi.bulkCreateVendors(requests);
  }

  return (
    <Card
      title="Vendors"
      actions={
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <BulkImportControls
            sampleFilename="vendors-sample.csv"
            sampleRows={SAMPLE_ROWS}
            onUpload={handleBulkUpload}
            onImportComplete={loadVendors}
          />
          <Button icon="plus" severity="success" onClick={openDialog}>
            New Vendor
          </Button>
        </div>
      }
    >
      <Table
        data={vendors}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No vendors yet. Create your first one."
      />

      <Dialog
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        title="New Vendor"
        width={560}
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
          <Input id="vendor-name" label="Name" value={form.name} onChange={(e) => update("name", e.target.value)} required />

          <div className="form-row">
            <Input
              id="vendor-gstin"
              label="GSTIN (optional)"
              value={form.gstin}
              onChange={(e) => update("gstin", e.target.value)}
            />
            <Input
              id="vendor-stateCode"
              label="GST state code (optional)"
              placeholder="e.g. 27"
              maxLength={2}
              value={form.stateCode}
              onChange={(e) => update("stateCode", e.target.value)}
            />
          </div>

          <div className="form-row">
            <Input
              id="vendor-contactPerson"
              label="Contact person (optional)"
              value={form.contactPerson}
              onChange={(e) => update("contactPerson", e.target.value)}
            />
            <Input
              id="vendor-phone"
              label="Phone (optional)"
              value={form.phone}
              onChange={(e) => update("phone", e.target.value)}
            />
          </div>

          <Input
            id="vendor-email"
            label="Email (optional)"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
          />

          <Input
            id="vendor-addressLine1"
            label="Address (optional)"
            value={form.addressLine1}
            onChange={(e) => update("addressLine1", e.target.value)}
          />

          <div className="form-row">
            <Input id="vendor-city" label="City (optional)" value={form.city} onChange={(e) => update("city", e.target.value)} />
            <Input
              id="vendor-pincode"
              label="Pincode (optional)"
              value={form.pincode}
              onChange={(e) => update("pincode", e.target.value)}
            />
          </div>
        </form>
      </Dialog>
    </Card>
  );
}
