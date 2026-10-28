import { useCallback, useEffect, useState } from "react";
import { Button, Card, Tag, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as appointmentApi from "../../api/appointmentApi";
import type { OpenDay } from "../../api/appointmentApi";
import { dateLabel } from "./appointmentFormat";

/**
 * The dates shoppers can request, from today for 14 days. A date that isn't switched on shows no slots, which is how a
 * holiday or leave is handled - there is no holiday list to keep up to date.
 */
export function PublishedDatesCard() {
  const toast = useToast();
  const [days, setDays] = useState<OpenDay[]>([]);
  /** Approved appointments per date, so switching a date off can say they stay booked. */
  const [approvedByDate, setApprovedByDate] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [openDays, approved] = await Promise.all([appointmentApi.getOpenDays(), appointmentApi.listRequests("APPROVED")]);
      setDays(openDays);
      const counts: Record<string, number> = {};
      for (const appointment of approved) counts[appointment.date] = (counts[appointment.date] ?? 0) + 1;
      setApprovedByDate(counts);
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load dates", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function toggle(day: OpenDay) {
    // The switch moves straight away; it goes back if the server says no.
    setDays((current) => current.map((d) => (d.date === day.date ? { ...d, open: !day.open } : d)));
    try {
      const updated = await appointmentApi.setDayOpen(day.date, !day.open);
      setDays((current) => current.map((d) => (d.date === updated.date ? updated : d)));
      const booked = approvedByDate[day.date] ?? 0;
      if (!updated.open && booked > 0) {
        toast.show({
          severity: "info",
          summary: `${dateLabel(day.date)} is closed to new requests`,
          detail: `${booked} approved ${booked === 1 ? "appointment is" : "appointments are"} still booked. Cancel ${booked === 1 ? "it" : "them"} from Requests if needed.`,
        });
      }
    } catch (err) {
      setDays((current) => current.map((d) => (d.date === day.date ? day : d)));
      toast.show({ severity: "error", summary: "Could not update the date", detail: extractErrorMessage(err) });
    }
  }

  async function publishWeek() {
    setBusy(true);
    try {
      const before = days.filter((d) => d.open).length;
      const updated = await appointmentApi.publishNext(7);
      setDays(updated);
      const added = updated.filter((d) => d.open).length - before;
      toast.show({ severity: "success", summary: added > 0 ? `Opened ${added} ${added === 1 ? "date" : "dates"}` : "Nothing new to open", detail: "Dates with no hours that weekday stay closed." });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not publish", detail: extractErrorMessage(err) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card
      title="Dates open for appointments"
      actions={
        <Button variant="outlined" onClick={publishWeek} loading={busy}>
          Publish next 7 days
        </Button>
      }
    >
      <p className="field-hint appt-intro">
        Switch on the dates you will take appointments. Leave a holiday or day off switched off and shoppers will see no slots that day.
      </p>
      {loading ? (
        <p className="appt-sub">Loading...</p>
      ) : (
        <ul className="appt-dates">
          {days.map((day) => {
            const label = dateLabel(day.date);
            const booked = approvedByDate[day.date] ?? 0;
            return (
              <li key={day.date} className="appt-date">
                <label className="appt-switch">
                  <input type="checkbox" role="switch" aria-label={`Open ${label}`} checked={day.open} onChange={() => toggle(day)} />
                  <span>{label}</span>
                </label>
                {day.open && !day.weekdayHasHours && <Tag severity="warning">No hours that day</Tag>}
                {booked > 0 && (
                  <span className="appt-sub">
                    {booked} approved {booked === 1 ? "appointment" : "appointments"}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
