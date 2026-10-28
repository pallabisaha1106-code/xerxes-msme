import { useEffect, useState } from "react";
import { Button, Card, Dialog, Input, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminPartnerApi from "../../api/adminPartnerApi";
import type { AdminPartner } from "../../api/adminPartnerApi";

/** Partner applications waiting for a decision: approve, or reject with a reason. */
export function AdminPartnerApplicationsPage() {
  const toast = useToast();
  const [applications, setApplications] = useState<AdminPartner[]>([]);
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [rejectTarget, setRejectTarget] = useState<AdminPartner | null>(null);
  const [reason, setReason] = useState("");

  async function load() {
    setLoading(true);
    try {
      setApplications(await adminPartnerApi.listPartners("APPLIED"));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load applications", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function removeFromList(id: string) {
    setApplications((current) => current.filter((a) => a.id !== id));
  }

  async function handleApprove(partner: AdminPartner) {
    setActioningId(partner.id);
    try {
      await adminPartnerApi.approvePartner(partner.id);
      removeFromList(partner.id);
      toast.show({ severity: "success", summary: "Partner approved", detail: partner.name });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not approve partner", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  async function handleReject() {
    if (!rejectTarget) return;
    setActioningId(rejectTarget.id);
    try {
      await adminPartnerApi.rejectPartner(rejectTarget.id, reason);
      removeFromList(rejectTarget.id);
      toast.show({ severity: "success", summary: "Application rejected", detail: rejectTarget.name });
      setRejectTarget(null);
      setReason("");
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not reject application", detail: extractErrorMessage(err) });
    } finally {
      setActioningId(null);
    }
  }

  const columns: Column<AdminPartner>[] = [
    { field: "name", header: "Name" },
    { field: "email", header: "Email" },
    { field: "whatsapp", header: "WhatsApp", body: (p) => `+${p.whatsapp}` },
    { field: "areas", header: "Areas", body: (p) => p.areas.map((a) => `${a.areaName}, ${a.cityName}`).join("; ") },
    { field: "createdAt", header: "Applied", body: (p) => new Date(p.createdAt).toLocaleString() },
    {
      field: "actions",
      header: "",
      body: (p) => (
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button size="sm" severity="success" loading={actioningId === p.id} onClick={() => handleApprove(p)}>
            Approve
          </Button>
          <Button size="sm" variant="outlined" severity="danger" onClick={() => setRejectTarget(p)}>
            Reject
          </Button>
        </div>
      ),
    },
  ];

  return (
    <Card title="Partner Applications">
      <Table
        columns={columns}
        data={applications}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No partner applications waiting."
      />
      <Dialog
        visible={rejectTarget != null}
        onHide={() => { setRejectTarget(null); setReason(""); }}
        title="Reject Application"
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => { setRejectTarget(null); setReason(""); }}>
              Cancel
            </Button>
            <Button severity="danger" disabled={!reason.trim()} loading={actioningId === rejectTarget?.id} onClick={handleReject}>
              Reject
            </Button>
          </>
        }
      >
        <p>
          Reject the application from <strong>{rejectTarget?.name}</strong>?
        </p>
        <Input id="partner-reject-reason" label="Reason (required)" value={reason} onChange={(e) => setReason(e.target.value)} />
      </Dialog>
    </Card>
  );
}
