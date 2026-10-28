import { useEffect, useState } from "react";
import { Button, Card, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminPartnerApi from "../../api/adminPartnerApi";
import type { AdminPartner } from "../../api/adminPartnerApi";
import { EditPartnerAreasDialog } from "./EditPartnerAreasDialog";

function statusTag(status: AdminPartner["status"]) {
  if (status === "ACTIVE") return <Tag severity="success">ACTIVE</Tag>;
  if (status === "INACTIVE") return <Tag severity="warning">INACTIVE</Tag>;
  if (status === "REJECTED") return <Tag severity="danger">REJECTED</Tag>;
  return <Tag severity="secondary">APPLIED</Tag>;
}

/** Approved partners (and the ones switched off or rejected): switch on/off and change the areas they cover. */
export function AdminPartnersPage() {
  const toast = useToast();
  const [partners, setPartners] = useState<AdminPartner[]>([]);
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [editTarget, setEditTarget] = useState<AdminPartner | null>(null);

  useEffect(() => {
    adminPartnerApi
      .listPartners()
      .then((all) => setPartners(all.filter((p) => p.status !== "APPLIED")))
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load partners", detail: extractErrorMessage(err) }))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyUpdated(updated: AdminPartner) {
    setPartners((current) => current.map((p) => (p.id === updated.id ? updated : p)));
  }

  async function handleToggle(partner: AdminPartner) {
    setActioningId(partner.id);
    try {
      const updated =
        partner.status === "ACTIVE"
          ? await adminPartnerApi.deactivatePartner(partner.id)
          : await adminPartnerApi.reactivatePartner(partner.id);
      applyUpdated(updated);
      toast.show({
        severity: "success",
        summary: updated.status === "ACTIVE" ? "Partner reactivated" : "Partner deactivated",
        detail: partner.name,
      });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update partner", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  const columns: Column<AdminPartner>[] = [
    { field: "name", header: "Name" },
    { field: "email", header: "Email" },
    { field: "whatsapp", header: "WhatsApp", body: (p) => `+${p.whatsapp}` },
    { field: "areas", header: "Areas", body: (p) => p.areas.map((a) => `${a.areaName}, ${a.cityName}`).join("; ") },
    { field: "assignedShopCount", header: "Shops" },
    { field: "status", header: "Status", body: (p) => statusTag(p.status) },
    {
      field: "actions",
      header: "",
      body: (p) =>
        p.status === "ACTIVE" || p.status === "INACTIVE" ? (
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              size="sm"
              variant="outlined"
              severity={p.status === "ACTIVE" ? "danger" : "success"}
              loading={actioningId === p.id}
              onClick={() => handleToggle(p)}
            >
              {p.status === "ACTIVE" ? "Deactivate" : "Reactivate"}
            </Button>
            <Button size="sm" variant="outlined" severity="secondary" onClick={() => setEditTarget(p)}>
              Edit areas
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <Card title="Local Partners">
      <Table columns={columns} data={partners} rowKey={(row) => row.id} loading={loading} emptyMessage="No local partners yet." />
      <EditPartnerAreasDialog
        partner={editTarget}
        onHide={() => setEditTarget(null)}
        onSaved={(updated) => {
          applyUpdated(updated);
          setEditTarget(null);
        }}
      />
    </Card>
  );
}
