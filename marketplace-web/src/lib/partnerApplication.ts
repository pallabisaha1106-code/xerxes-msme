import { isIndianMobile } from "./appointmentFormat";
import { isValidEmail } from "./emailFormat";

export interface PartnerFormValues {
  name: string;
  whatsapp: string;
  email: string;
  password: string;
  areaIds: string[];
}

/** What is still missing or wrong on the "Become a local partner" form, in the order it appears; empty when ready to send. */
export function partnerFormProblems(form: PartnerFormValues): string[] {
  const problems: string[] = [];
  if (!form.name.trim()) problems.push("your name");
  if (!isIndianMobile(form.whatsapp)) problems.push("a valid Indian mobile number for WhatsApp");
  if (!isValidEmail(form.email)) problems.push("a valid email address (like name@example.com)");
  if (form.password.length < 8) problems.push("a password of at least 8 characters");
  if (form.areaIds.length === 0) problems.push("at least one area you can cover");
  return problems;
}
