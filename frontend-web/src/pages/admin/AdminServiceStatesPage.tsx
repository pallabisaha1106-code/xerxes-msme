import { useEffect, useState } from "react";
import { Button, Card, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminServiceStateApi from "../../api/adminServiceStateApi";
import type { AdminServiceState } from "../../api/adminServiceStateApi";

export function AdminServiceStatesPage() {
  const toast = useToast();
  const [states, setStates] = useState<AdminServiceState[]>([]);
  const [loading, setLoading] = useState(true);
  const [togglingCode, setTogglingCode] = useState<string | null>(null);

  async function loadStates() {
    setLoading(true);
    try {
      setStates(await adminServiceStateApi.listServiceStates());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load states", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadStates();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleToggle(state: AdminServiceState) {
    setTogglingCode(state.code);
    try {
      const updated = await adminServiceStateApi.updateServiceStateStatus(state.code, !state.active);
      setStates((current) => current.map((s) => (s.code === updated.code ? updated : s)));
      toast.show({
        severity: "success",
        summary: updated.active ? "State enabled" : "State disabled",
        detail: updated.name,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update state", detail: extractErrorMessage(err) });
    } finally {
      setTogglingCode(null);
    }
  }

  const columns: Column<AdminServiceState>[] = [
    { field: "code", header: "GST Code" },
    { field: "name", header: "State" },
    {
      field: "active",
      header: "Status",
      body: (s) => (s.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">DISABLED</Tag>),
    },
    {
      field: "actions",
      header: "",
      body: (s) => (
        <Button
          size="sm"
          variant="outlined"
          severity={s.active ? "danger" : "success"}
          loading={togglingCode === s.code}
          onClick={() => handleToggle(s)}
        >
          {s.active ? "Disable" : "Enable"}
        </Button>
      ),
    },
  ];

  return (
    <Card title="Service States">
      <p className="field-hint" style={{ marginBottom: "1rem" }}>
        Disabling a state hides it from vendor registration&rsquo;s GST state dropdown and blocks new registrations
        under it - existing tenants already registered under it are left alone. This is the fixed official GST
        state/UT list, so states can only be enabled or disabled here, not added or removed.
      </p>
      <Table data={states} columns={columns} rowKey={(row) => row.code} loading={loading} emptyMessage="No states found." />
    </Card>
  );
}
