const WEEKDAYS_SHORT = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS_SHORT = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** ISO weekday names, 1 = Monday ... 7 = Sunday, as the weekly hours use them. */
export const WEEKDAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

/** "Wed 21 Oct" for "2026-10-21". Built by hand so it never depends on the browser's locale or time zone. */
export function dateLabel(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  return `${WEEKDAYS_SHORT[weekday]} ${day} ${MONTHS_SHORT[month - 1]}`;
}

/** "10:30 - 11:00" */
export function timeRange(start: string, end: string): string {
  return `${start} - ${end}`;
}

/** "1 other", "3 others" - the hint on a waiting request that others also asked for. */
export function othersLabel(count: number): string {
  return `${count} ${count === 1 ? "other" : "others"} asked for this time`;
}
