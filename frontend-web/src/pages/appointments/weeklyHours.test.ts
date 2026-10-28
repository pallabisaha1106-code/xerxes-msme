import { describe, expect, it } from "vitest";
import { emptyWeek, firstProblem, rangesFromWeek, weekFromRanges, type Week } from "./weeklyHours";

const week = (overrides: Record<number, { opensAt: string; closesAt: string }[]> = {}): Week => ({ ...emptyWeek(), ...overrides });

describe("weekFromRanges / rangesFromWeek", () => {
  it("groups ranges by weekday, in opening order", () => {
    const result = weekFromRanges([
      { dayOfWeek: 1, opensAt: "17:00", closesAt: "20:00" },
      { dayOfWeek: 1, opensAt: "10:00", closesAt: "13:00" },
      { dayOfWeek: 3, opensAt: "09:00", closesAt: "12:00" },
    ]);

    expect(result[1]).toEqual([{ opensAt: "10:00", closesAt: "13:00" }, { opensAt: "17:00", closesAt: "20:00" }]);
    expect(result[3]).toEqual([{ opensAt: "09:00", closesAt: "12:00" }]);
    expect(result[2]).toEqual([]);
  });

  it("has an entry for every weekday even when nothing is saved", () => {
    expect(Object.keys(weekFromRanges([]))).toEqual(["1", "2", "3", "4", "5", "6", "7"]);
  });

  it("turns the editor back into a flat list ordered by day", () => {
    expect(rangesFromWeek(week({ 2: [{ opensAt: "10:00", closesAt: "11:00" }], 1: [{ opensAt: "08:00", closesAt: "09:00" }] }))).toEqual([
      { dayOfWeek: 1, opensAt: "08:00", closesAt: "09:00" },
      { dayOfWeek: 2, opensAt: "10:00", closesAt: "11:00" },
    ]);
  });

  it("round-trips", () => {
    const ranges = [
      { dayOfWeek: 1, opensAt: "10:00", closesAt: "13:00" },
      { dayOfWeek: 1, opensAt: "17:00", closesAt: "20:00" },
      { dayOfWeek: 7, opensAt: "11:00", closesAt: "14:00" },
    ];
    expect(rangesFromWeek(weekFromRanges(ranges))).toEqual(ranges);
  });
});

describe("firstProblem", () => {
  it("accepts an empty week and a sensible one", () => {
    expect(firstProblem(emptyWeek())).toBeNull();
    expect(firstProblem(week({ 1: [{ opensAt: "10:00", closesAt: "13:00" }, { opensAt: "13:00", closesAt: "15:00" }, { opensAt: "17:00", closesAt: "20:00" }] }))).toBeNull();
  });

  it("flags opening at or after closing, naming the day", () => {
    expect(firstProblem(week({ 1: [{ opensAt: "13:00", closesAt: "10:00" }] }))).toBe("Monday: the opening time must be before the closing time.");
    expect(firstProblem(week({ 2: [{ opensAt: "10:00", closesAt: "10:00" }] }))).toBe("Tuesday: the opening time must be before the closing time.");
  });

  it("flags a missing time", () => {
    expect(firstProblem(week({ 3: [{ opensAt: "", closesAt: "10:00" }] }))).toBe("Wednesday: fill in both times.");
  });

  it("flags overlapping ranges even when entered out of order", () => {
    expect(firstProblem(week({ 4: [{ opensAt: "12:30", closesAt: "15:00" }, { opensAt: "10:00", closesAt: "13:00" }] }))).toBe("Thursday: opening ranges must not overlap.");
  });

  it("flags more than four ranges in a day", () => {
    const five = ["08", "10", "12", "14", "16"].map((hour) => ({ opensAt: `${hour}:00`, closesAt: `${hour}:30` }));
    expect(firstProblem(week({ 5: five }))).toBe("Friday: at most 4 opening ranges per day.");
    expect(firstProblem(week({ 5: five.slice(0, 4) }))).toBeNull();
  });

  it("reports the earliest weekday's problem first", () => {
    expect(firstProblem(week({ 6: [{ opensAt: "13:00", closesAt: "10:00" }], 2: [{ opensAt: "13:00", closesAt: "10:00" }] }))).toContain("Tuesday");
  });
});
