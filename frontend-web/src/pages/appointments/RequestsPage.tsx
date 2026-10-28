import { useCallback, useEffect, useState } from "react";
import { Button, Card, Dialog, Select, Table, Tag, useToast, type Column } from "../../ui";
import { useAuth } from "../../auth/AuthContext";
import { extractErrorMessage } from "../../api/client";
import * as appointmentApi from "../../api/appointmentApi";
import type { AppointmentRequest, AppointmentStatus } from "../../api/appointmentApi";
import { dateLabel, othersLabel, timeRange } from "./appointmentFormat";
import { callLink, whatsappLink } from "./contactLinks";
import { notifyAppointmentsChanged } from "../../layout/usePendingAppointments";
import "./appointments.css";

const STATUS_OPTIONS = [
  { label: "Waiting for you", value: "PENDING" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "Cancelled", value: "CANCELLED" },
  { label: "Expired", value: "EXPIRED" },
  { label: "All requests", value: "" },
];

function statusTag(status: AppointmentStatus) {
  if (status === "APPROVED") return <Tag severity="success">APPROVED</Tag>;
  if (status === "REJECTED") return <Tag severity="danger">REJECTED</Tag>;
  if (status === "PENDING") return <Tag severity="warning">WAITING</Tag>;
  return <Tag severity="secondary">{status}</Tag>;
}

/** The business's inbox of appointment requests: approve one request per time, reject the rest, and tell people. */
export function RequestsPage() {
  const toast = useToast();
  const { user } = useAuth();
  const businessName = user?.businessName ?? "our shop";

  const [status, setStatus] = useState<AppointmentStatus | "">("PENDING");
  const [requests, setRequests] = useState<AppointmentRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<AppointmentRequest | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRequests(await appointmentApi.listRequests(status || undefined));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load requests", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  useEffect(() => {
    load();
  }, [load]);

  // Coming back to the tab is the moment new requests are most likely waiting.
  useEffect(() => {
    window.addEventListener("focus", load);
    return () => window.removeEventListener("focus", load);
  }, [load]);

  async function decide(request: AppointmentRequest, action: "approve" | "reject" | "cancel") {
    setActingId(request.id);
    try {
      if (action === "approve") await appointmentApi.approveRequest(request.id);
      else if (action === "reject") await appointmentApi.rejectRequest(request.id);
      else await appointmentApi.cancelRequest(request.id);
      toast.show({
        severity: "success",
        summary: action === "approve" ? "Request approved" : action === "reject" ? "Request rejected" : "Appointment cancelled",
        detail: "Let the customer know with the WhatsApp or Call button.",
      });
      setCancelTarget(null);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update the request", detail: extractErrorMessage(err) });
    } finally {
      setActingId(null);
      await load();
      notifyAppointmentsChanged();
    }
  }

  const columns: Column<AppointmentRequest>[] = [
    {
      field: "date",
      header: "When",
      body: (r) => (
        <div>
          <strong>{dateLabel(r.date)}</strong>
          <div className="appt-sub">{timeRange(r.time, r.endsAt)}</div>
        </div>
      ),
    },
    {
      field: "customerName",
      header: "Customer",
      body: (r) => (
        <div>
          <strong>{r.customerName}</strong>
          <div className="appt-sub">{r.customerPhone}</div>
          {r.note && <div className="appt-note">&ldquo;{r.note}&rdquo;</div>}
        </div>
      ),
    },
    {
      field: "serviceName",
      header: "Service",
      body: (r) => (
        <div>
          {r.serviceName}
          {r.price != null && r.price > 0 && <div className="appt-sub">₹{r.price.toFixed(2)}</div>}
        </div>
      ),
    },
    {
      field: "status",
      header: "Status",
      body: (r) => (
        <div className="appt-status">
          {statusTag(r.status)}
          {r.timeAlreadyBooked && <Tag severity="danger">TIME ALREADY BOOKED</Tag>}
          {r.othersAtSameTime > 0 && <span className="appt-sub">{othersLabel(r.othersAtSameTime)}</span>}
        </div>
      ),
    },
    {
      field: "actions",
      header: "",
      body: (r) => (
        <div className="appt-actions">
          {r.status === "PENDING" && (
            <>
              <Button size="sm" severity="success" loading={actingId === r.id} disabled={r.timeAlreadyBooked} onClick={() => decide(r, "approve")}>
                Approve
              </Button>
              <Button size="sm" variant="outlined" severity="danger" disabled={actingId === r.id} onClick={() => decide(r, "reject")}>
                Reject
              </Button>
            </>
          )}
          {r.status === "APPROVED" && (
            <Button size="sm" variant="outlined" severity="danger" disabled={actingId === r.id} onClick={() => setCancelTarget(r)}>
              Cancel
            </Button>
          )}
          <a
            className="btn btn-outlined btn-secondary btn-sm"
            href={whatsappLink(r, businessName)}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`WhatsApp ${r.customerName}`}
          >
            WhatsApp
          </a>
          <a className="btn btn-outlined btn-secondary btn-sm" href={callLink(r.customerPhone)} aria-label={`Call ${r.customerName}`}>
            Call
          </a>
        </div>
      ),
    },
  ];

  return (
    <Card
      title="Appointment Requests"
      actions={
        <div className="appt-toolbar">
          <Select
            id="appointment-status-filter"
            aria-label="Show requests"
            value={status}
            onChange={(e) => setStatus(e.target.value as AppointmentStatus | "")}
            options={STATUS_OPTIONS}
          />
          <Button variant="outlined" severity="secondary" onClick={load} loading={loading}>
            Refresh
          </Button>
        </div>
      }
    >
      <p className="field-hint appt-intro">
        Several people can ask for the same time. Approve one and reject the others, then tell each person by WhatsApp or a call.
      </p>
      <Table
        data={requests}
        columns={columns}
        rowKey={(row) => row.id}
        loading={loading}
        emptyMessage="No requests here. When someone requests a slot on your shop page it will show up."
      />

      <Dialog
        visible={cancelTarget != null}
        onHide={() => setCancelTarget(null)}
        title="Cancel this appointment?"
        width={440}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setCancelTarget(null)}>
              Keep it
            </Button>
            <Button severity="danger" loading={actingId != null} onClick={() => cancelTarget && decide(cancelTarget, "cancel")}>
              Cancel appointment
            </Button>
          </>
        }
      >
        {cancelTarget && (
          <p>
            {cancelTarget.customerName} - {cancelTarget.serviceName}, {dateLabel(cancelTarget.date)} at {cancelTarget.time}. The time becomes
            available again. Tell {cancelTarget.customerName} with the WhatsApp or Call button.
          </p>
        )}
      </Dialog>
    </Card>
  );
}
