import { describe, expect, it } from "vitest";
import { dateLabel, othersLabel, timeRange, WEEKDAY_NAMES } from "./appointmentFormat";

describe("dateLabel", () => {
  it("writes weekday, day and month", () => {
    expect(dateLabel("2026-10-21")).toBe("Wed 21 Oct");
    expect(dateLabel("2026-10-19")).toBe("Mon 19 Oct");
    expect(dateLabel("2026-11-01")).toBe("Sun 1 Nov");
  });

  it("handles month and year edges", () => {
    expect(dateLabel("2026-12-31")).toBe("Thu 31 Dec");
    expect(dateLabel("2027-01-01")).toBe("Fri 1 Jan");
    expect(dateLabel("2028-02-29")).toBe("Tue 29 Feb");
  });
});

describe("timeRange / othersLabel / WEEKDAY_NAMES", () => {
  it("formats a time range", () => {
    expect(timeRange("10:30", "11:00")).toBe("10:30 - 11:00");
  });

  it("pluralises the others hint", () => {
    expect(othersLabel(1)).toBe("1 other asked for this time");
    expect(othersLabel(3)).toBe("3 others asked for this time");
  });

  it("starts the week on Monday", () => {
    expect(WEEKDAY_NAMES[0]).toBe("Monday");
    expect(WEEKDAY_NAMES[6]).toBe("Sunday");
  });
});
