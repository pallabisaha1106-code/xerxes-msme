import { useEffect, useState, type FormEvent } from "react";
import { BulkImportControls, Button, Card, Dialog, Input, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as customerApi from "../../api/customerApi";
import type { CustomerResponse } from "../../api/customerApi";

const SAMPLE_ROWS: string[][] = [
  ["Name", "GSTIN", "State Code", "Contact Person", "Phone", "Email", "Address", "City", "Pincode"],
  ["Priya Sharma", "", "27", "", "9820011223", "priya.sharma@example.com", "", "Mumbai", "400001"],
  [
    "Rohan Fashions Retail",
    "27AAAPR4321E1Z9",
    "27",
    "Rohan Mehta",
    "9819988776",
    "rohan@rohanfashions.example",
    "Linking Road",
    "Mumbai",
    "400050",
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

export function CustomersPage() {
  const toast = useToast();
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogVisible, setDialogVisible] = useState(false);
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);

  async function loadCustomers() {
    setLoading(true);
    try {
      setCustomers(await customerApi.listCustomers());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load customers", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCustomers();
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
      await customerApi.createCustomer({
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
      toast.show({ severity: "success", summary: "Customer created", detail: form.name });
      setDialogVisible(false);
      await loadCustomers();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not create customer", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  const columns: Column<CustomerResponse>[] = [
    { field: "name", header: "Name", sortable: true },
    { field: "gstin", header: "GSTIN", body: (c) => c.gstin ?? "-" },
    { field: "contactPerson", header: "Contact", body: (c) => c.contactPerson ?? "-" },
    { field: "phone", header: "Phone", body: (c) => c.phone ?? "-" },
    { field: "city", header: "City", body: (c) => c.city ?? "-" },
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
    return customerApi.bulkCreateCustomers(requests);
  }

  return (
    <Card
      title="Customers"
      actions={
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <BulkImportControls
            sampleFilename="customers-sample.csv"
            sampleRows={SAMPLE_ROWS}
            onUpload={handleBulkUpload}
            onImportComplete={loadCustomers}
          />
          <Button icon="plus" severity="success" onClick={openDialog}>
            New Customer
          </Button>
        </div>
      }
    >
      <Table
        data={customers}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No customers yet. Create your first one."
      />

      <Dialog
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        title="New Customer"
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
          <Input
            id="customer-name"
            label="Name"
            value={form.name}
            onChange={(e) => update("name", e.target.value)}
            required
          />

          <div className="form-row">
            <Input
              id="customer-gstin"
              label="GSTIN (optional)"
              value={form.gstin}
              onChange={(e) => update("gstin", e.target.value)}
            />
            <Input
              id="customer-stateCode"
              label="GST state code (optional)"
              placeholder="e.g. 27"
              maxLength={2}
              value={form.stateCode}
              onChange={(e) => update("stateCode", e.target.value)}
            />
          </div>

          <div className="form-row">
            <Input
              id="customer-contactPerson"
              label="Contact person (optional)"
              value={form.contactPerson}
              onChange={(e) => update("contactPerson", e.target.value)}
            />
            <Input
              id="customer-phone"
              label="Phone (optional)"
              value={form.phone}
              onChange={(e) => update("phone", e.target.value)}
            />
          </div>

          <Input
            id="customer-email"
            label="Email (optional)"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
          />

          <Input
            id="customer-addressLine1"
            label="Address (optional)"
            value={form.addressLine1}
            onChange={(e) => update("addressLine1", e.target.value)}
          />

          <div className="form-row">
            <Input
              id="customer-city"
              label="City (optional)"
              value={form.city}
              onChange={(e) => update("city", e.target.value)}
            />
            <Input
              id="customer-pincode"
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
