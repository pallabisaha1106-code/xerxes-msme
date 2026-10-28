import type { OpeningRange } from "../../api/appointmentApi";
import { WEEKDAY_NAMES } from "./appointmentFormat";

/** The editor's shape: for each ISO weekday (1 = Monday), its opening ranges as "HH:mm" text. */
export type Week = Record<number, { opensAt: string; closesAt: string }[]>;

export const DAYS = [1, 2, 3, 4, 5, 6, 7];
export const NEW_RANGE = { opensAt: "10:00", closesAt: "13:00" };
const MAX_RANGES_PER_DAY = 4;

export function emptyWeek(): Week {
  return Object.fromEntries(DAYS.map((day) => [day, []]));
}

export function weekFromRanges(ranges: OpeningRange[]): Week {
  const week = emptyWeek();
  for (const range of ranges) week[range.dayOfWeek].push({ opensAt: range.opensAt, closesAt: range.closesAt });
  for (const day of DAYS) week[day].sort((a, b) => a.opensAt.localeCompare(b.opensAt));
  return week;
}

export function rangesFromWeek(week: Week): OpeningRange[] {
  return DAYS.flatMap((day) => week[day].map((range) => ({ dayOfWeek: day, ...range })));
}

/**
 * The first thing wrong with a week, in the backend's own wording, or null. Only a courtesy so the vendor hears about
 * a slip before saving; the backend checks every rule again (including that a range fits one appointment).
 */
export function firstProblem(week: Week): string | null {
  for (const day of DAYS) {
    const name = WEEKDAY_NAMES[day - 1];
    const ranges = [...week[day]].sort((a, b) => a.opensAt.localeCompare(b.opensAt));
    if (ranges.some((range) => !range.opensAt || !range.closesAt)) return `${name}: fill in both times.`;
    if (ranges.some((range) => range.opensAt >= range.closesAt)) return `${name}: the opening time must be before the closing time.`;
    if (ranges.length > MAX_RANGES_PER_DAY) return `${name}: at most ${MAX_RANGES_PER_DAY} opening ranges per day.`;
    for (let i = 1; i < ranges.length; i++) {
      if (ranges[i].opensAt < ranges[i - 1].closesAt) return `${name}: opening ranges must not overlap.`;
    }
  }
  return null;
}
