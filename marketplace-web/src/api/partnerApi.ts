import { apiUrl } from "./apiBase";
import { toApiError } from "./apiError";

/** What a shop owner sees of their local partner: a name and a WhatsApp number (digits with the country code). */
export interface PartnerContact {
  name: string;
  whatsapp: string;
}

export interface PartnerApplicationRequest {
  name: string;
  whatsapp: string;
  email: string;
  password: string;
  areaIds: string[];
}

export interface PartnerApplicationResponse {
  partnerId: string;
  name: string;
}

/** Someone asking to become a local partner. Nothing is switched on until Super Admin approves. */
export async function applyAsPartner(request: PartnerApplicationRequest): Promise<PartnerApplicationResponse> {
  const response = await fetch(apiUrl("/api/public/partner-applications"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw await toApiError(response, "Could not send your application");
  return response.json();
}
