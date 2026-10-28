import { apiClient } from "./client";

export type AppointmentStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED" | "CANCELLED";

export interface AppointmentSettings {
  slotMinutes: number;
  allowedSlotMinutes: number[];
}

/** One opening range on one weekday (1 = Monday ... 7 = Sunday), times as "HH:mm". */
export interface OpeningRange {
  dayOfWeek: number;
  opensAt: string;
  closesAt: string;
}

export interface OpenDay {
  date: string;
  open: boolean;
  weekdayHasHours: boolean;
}

export interface AppointmentRequest {
  id: string;
  serviceName: string;
  price: number | null;
  customerName: string;
  customerPhone: string;
  note: string | null;
  /** The business's local date (YYYY-MM-DD) and "HH:mm" start and end. */
  date: string;
  time: string;
  endsAt: string;
  /** Effective status: a waiting request whose time has passed reads EXPIRED. */
  status: AppointmentStatus;
  othersAtSameTime: number;
  timeAlreadyBooked: boolean;
  requestedAt: string;
}

export async function getSettings(): Promise<AppointmentSettings> {
  const { data } = await apiClient.get<AppointmentSettings>("/appointments/settings");
  return data;
}

export async function saveSettings(slotMinutes: number): Promise<AppointmentSettings> {
  const { data } = await apiClient.put<AppointmentSettings>("/appointments/settings", { slotMinutes });
  return data;
}

export async function getHours(): Promise<OpeningRange[]> {
  const { data } = await apiClient.get<{ ranges: OpeningRange[] }>("/appointments/hours");
  return data.ranges;
}

/** Replaces the whole weekly template. */
export async function saveHours(ranges: OpeningRange[]): Promise<OpeningRange[]> {
  const { data } = await apiClient.put<{ ranges: OpeningRange[] }>("/appointments/hours", { ranges });
  return data.ranges;
}

export async function getOpenDays(): Promise<OpenDay[]> {
  const { data } = await apiClient.get<{ days: OpenDay[] }>("/appointments/open-days");
  return data.days;
}

export async function setDayOpen(date: string, open: boolean): Promise<OpenDay> {
  const { data } = await apiClient.put<OpenDay>(`/appointments/open-days/${date}`, { open });
  return data;
}

export async function publishNext(days: number): Promise<OpenDay[]> {
  const { data } = await apiClient.post<{ days: OpenDay[] }>("/appointments/open-days/publish-next", { days });
  return data.days;
}

export async function listRequests(status?: AppointmentStatus): Promise<AppointmentRequest[]> {
  const { data } = await apiClient.get<AppointmentRequest[]>("/appointments/requests", { params: status ? { status } : {} });
  return data;
}

export async function getPendingCount(): Promise<number> {
  const { data } = await apiClient.get<{ pending: number }>("/appointments/requests/pending-count");
  return data.pending;
}

export async function approveRequest(id: string): Promise<void> {
  await apiClient.post(`/appointments/requests/${id}/approve`);
}

export async function rejectRequest(id: string): Promise<void> {
  await apiClient.post(`/appointments/requests/${id}/reject`);
}

export async function cancelRequest(id: string): Promise<void> {
  await apiClient.post(`/appointments/requests/${id}/cancel`);
}
