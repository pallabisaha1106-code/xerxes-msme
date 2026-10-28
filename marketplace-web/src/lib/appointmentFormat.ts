const WEEKDAYS_SHORT = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS_SHORT = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** "Wed 21 Oct" for "2026-10-21". Built by hand so it never depends on the browser's locale or time zone. */
export function dateLabel(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  return `${WEEKDAYS_SHORT[weekday]} ${day} ${MONTHS_SHORT[month - 1]}`;
}

/** "10:30 am" / "5:30 pm" for "10:30" / "17:30"; midnight is 12:00 am and noon is 12:00 pm. */
export function timeLabel(time: string): string {
  const [hours, minutes] = time.split(":").map(Number);
  const suffix = hours < 12 ? "am" : "pm";
  const twelveHour = hours % 12 === 0 ? 12 : hours % 12;
  return `${twelveHour}:${String(minutes).padStart(2, "0")} ${suffix}`;
}

/**
 * Whether what was typed is an Indian mobile number, by the same rule the server applies: spaces, dashes and brackets
 * are ignored, a leading +91, 91 or 0 is dropped, and what is left must be ten digits starting with 6, 7, 8 or 9.
 */
export function isIndianMobile(raw: string): boolean {
  let digits = raw.replace(/[\s\-().]/g, "");
  if (digits.startsWith("+91")) digits = digits.slice(3);
  else if (digits.length === 12 && digits.startsWith("91")) digits = digits.slice(2);
  else if (digits.length === 11 && digits.startsWith("0")) digits = digits.slice(1);
  return /^[6-9][0-9]{9}$/.test(digits);
}

/** What the shopper is told once the request has been sent. Nothing is booked until the business answers. */
export function confirmationText(businessName: string, serviceName: string, date: string, time: string, phone: string): string {
  return (
    `Request sent! ${businessName} will confirm your appointment for ${serviceName} on ${dateLabel(date)} at ${timeLabel(time)} ` +
    `by WhatsApp or a call on ${phone}. Nothing is booked until they confirm.`
  );
}
