import type { AppointmentRequest } from "../../api/appointmentApi";
import { dateLabel } from "./appointmentFormat";

/** The ten digits the backend stores; any other length is passed through as typed digits. */
function digitsOf(phone: string): string {
  return phone.replace(/\D/g, "");
}

/** Indian mobile numbers are kept as ten digits, so the country code is added for the chat and call links. */
function internationalDigits(phone: string): string {
  const digits = digitsOf(phone);
  return digits.length === 10 ? `91${digits}` : digits;
}

export function callLink(phone: string): string {
  return `tel:+${internationalDigits(phone)}`;
}

/** The message the WhatsApp button opens with, matched to where the request stands. */
export function whatsappMessage(request: AppointmentRequest, businessName: string): string {
  const what = `${request.serviceName} on ${dateLabel(request.date)} at ${request.time}`;
  switch (request.status) {
    case "APPROVED":
      return `Hi ${request.customerName}, your appointment for ${what} is confirmed. See you at ${businessName}!`;
    case "REJECTED":
      return `Hi ${request.customerName}, sorry, we can't take your request for ${what}. Please request another time.`;
    case "CANCELLED":
      return `Hi ${request.customerName}, we're sorry, we have to cancel your appointment for ${what}. Please request another time.`;
    default:
      // Waiting or expired: just opens the conversation about the request.
      return `Hi ${request.customerName}, this is ${businessName}. About your request for ${what}.`;
  }
}

export function whatsappLink(request: AppointmentRequest, businessName: string): string {
  return `https://wa.me/${internationalDigits(request.customerPhone)}?text=${encodeURIComponent(whatsappMessage(request, businessName))}`;
}
