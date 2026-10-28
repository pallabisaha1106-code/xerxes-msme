import { apiUrl } from "./apiBase";
import { toApiError } from "./apiError";

export interface AvailableDay {
  /** The business's local date, YYYY-MM-DD. */
  date: string;
  /** Free start times, "HH:mm", earliest first. Never empty. */
  times: string[];
}

export interface Availability {
  slotMinutes: number;
  days: AvailableDay[];
}

export interface AppointmentRequestBody {
  variantId: string;
  date: string;
  time: string;
  name: string;
  phone: string;
  note?: string;
}

export interface AppointmentRequested {
  serviceName: string;
  date: string;
  time: string;
  slotMinutes: number;
}

/** The dates and free times a shopper can pick from; a shop that cannot take appointments is a 404. */
export async function getAvailability(tenantId: string): Promise<Availability> {
  const response = await fetch(apiUrl(`/api/public/appointments/${encodeURIComponent(tenantId)}/availability`));
  if (!response.ok) throw await toApiError(response, "Could not load appointment times");
  return response.json();
}

/** Sends a request for a slot. The business confirms offline, so success only means "it was received". */
export async function requestAppointment(tenantId: string, body: AppointmentRequestBody): Promise<AppointmentRequested> {
  const response = await fetch(apiUrl(`/api/public/appointments/${encodeURIComponent(tenantId)}/requests`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await toApiError(response, "Could not send your request");
  return response.json();
}
