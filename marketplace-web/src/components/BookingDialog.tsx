import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { getAvailability, requestAppointment, type Availability } from "../api/appointmentApi";
import { ApiRequestError, extractErrorMessage } from "../api/apiError";
import { confirmationText, dateLabel, isIndianMobile, timeLabel } from "../lib/appointmentFormat";

export interface BookingService {
  variantId: string;
  /** What the shopper sees and what the business receives: "Haircut (Men)". */
  name: string;
}

const EMPTY_FORM = { name: "", phone: "", note: "" };

/**
 * Requesting an appointment for one service: pick a date, pick a time, give a name and number, send. Everything is on
 * one page that grows as the shopper goes (the first date is chosen for them). The business confirms later by
 * WhatsApp or a call, so the last screen is only a confirmation that the request was received.
 */
export function BookingDialog({
  service,
  tenantId,
  businessName,
  whatsappUrl,
  onClose,
}: {
  /** null keeps the dialog closed. */
  service: BookingService | null;
  tenantId: string;
  businessName: string;
  /** The shop's WhatsApp chat link, offered when there are no times to pick; "" when the shop has none. */
  whatsappUrl: string;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [date, setDate] = useState<string | null>(null);
  const [time, setTime] = useState<string | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<string | null>(null);

  const load = useCallback(
    async (keepDate: string | null) => {
      setLoadError(null);
      try {
        const loaded = await getAvailability(tenantId);
        setAvailability(loaded);
        // Keep the shopper's date if it still has times; otherwise start from the first one.
        setDate(loaded.days.find((day) => day.date === keepDate)?.date ?? loaded.days[0]?.date ?? null);
      } catch (err) {
        setLoadError(extractErrorMessage(err));
      }
    },
    [tenantId],
  );

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (service && !dialog.open) {
      setAvailability(null);
      setTime(null);
      setForm(EMPTY_FORM);
      setError(null);
      setConfirmation(null);
      dialog.showModal();
      load(null);
    }
    if (!service && dialog.open) dialog.close();
  }, [service, load]);

  const day = availability?.days.find((d) => d.date === date);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!service || !date || !time) return;
    if (!isIndianMobile(form.phone)) {
      setError("Enter a valid 10-digit mobile number.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const sent = await requestAppointment(tenantId, {
        variantId: service.variantId,
        date,
        time,
        name: form.name.trim(),
        phone: form.phone.trim(),
        note: form.note.trim() || undefined,
      });
      setConfirmation(confirmationText(businessName, sent.serviceName, sent.date, sent.time, form.phone.trim()));
    } catch (err) {
      setError(extractErrorMessage(err));
      // A time taken in the meantime (or a date no longer open): show what is free now and let them pick again.
      if (err instanceof ApiRequestError && err.status === 422 && /no longer available/i.test(err.message)) {
        setTime(null);
        await load(date);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <dialog ref={dialogRef} className="checkout-dialog booking-dialog" aria-labelledby="booking-title" onClose={onClose}>
      <button className="dialog-close" type="button" aria-label="Close" onClick={() => dialogRef.current?.close()}>
        ×
      </button>
      <h2 id="booking-title">Request an appointment</h2>
      {service && <p className="booking-service">{service.name}</p>}

      {confirmation ? (
        <>
          <p className="checkout-confirmation">{confirmation}</p>
          <button type="button" className="primary-button" onClick={() => dialogRef.current?.close()}>
            Done
          </button>
        </>
      ) : loadError ? (
        <>
          <p className="checkout-error">{loadError}</p>
          <button type="button" className="primary-button" onClick={() => load(date)}>
            Try again
          </button>
        </>
      ) : availability === null ? (
        <p>Checking available times…</p>
      ) : availability.days.length === 0 ? (
        <>
          <p>No appointment times are open right now. Please check again soon.</p>
          {whatsappUrl && (
            <a className="whatsapp-us-link" href={whatsappUrl} target="_blank" rel="noopener noreferrer">
              WhatsApp us ↗
            </a>
          )}
        </>
      ) : (
        <form className="checkout-form" onSubmit={handleSubmit}>
          <fieldset className="booking-group">
            <legend>Pick a date</legend>
            <div className="booking-chips">
              {availability.days.map((d) => (
                <button
                  key={d.date}
                  type="button"
                  className="booking-chip"
                  aria-pressed={d.date === date}
                  onClick={() => {
                    setDate(d.date);
                    setTime(null);
                  }}
                >
                  {dateLabel(d.date)}
                </button>
              ))}
            </div>
          </fieldset>

          {day && (
            <fieldset className="booking-group">
              <legend>Pick a time ({availability.slotMinutes} minutes)</legend>
              <div className="booking-chips">
                {day.times.map((t) => (
                  <button key={t} type="button" className="booking-chip" aria-pressed={t === time} onClick={() => setTime(t)}>
                    {timeLabel(t)}
                  </button>
                ))}
              </div>
            </fieldset>
          )}

          {date && time && (
            <>
              <p className="booking-summary">
                {dateLabel(date)} at {timeLabel(time)}
              </p>
              <label>
                Your name
                <input required maxLength={120} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </label>
              <label>
                Mobile number
                <input required inputMode="tel" autoComplete="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </label>
              <label>
                Note (optional)
                <input maxLength={200} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
              </label>
              <p className="booking-hint">Please don’t put medical or other private details in the note.</p>
            </>
          )}

          {error && <p className="checkout-error">{error}</p>}
          {date && time && (
            <button type="submit" className="primary-button" disabled={submitting}>
              {submitting ? "Sending…" : "Send request"}
            </button>
          )}
        </form>
      )}
    </dialog>
  );
}
