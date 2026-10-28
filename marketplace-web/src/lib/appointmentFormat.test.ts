import { describe, expect, it } from "vitest";
import { confirmationText, dateLabel, isIndianMobile, timeLabel } from "./appointmentFormat";

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

describe("timeLabel", () => {
  it("uses a twelve-hour clock with am and pm", () => {
    expect(timeLabel("10:30")).toBe("10:30 am");
    expect(timeLabel("17:30")).toBe("5:30 pm");
    expect(timeLabel("09:05")).toBe("9:05 am");
  });

  it("gets midnight and noon right", () => {
    expect(timeLabel("00:00")).toBe("12:00 am");
    expect(timeLabel("00:45")).toBe("12:45 am");
    expect(timeLabel("12:00")).toBe("12:00 pm");
    expect(timeLabel("12:15")).toBe("12:15 pm");
    expect(timeLabel("23:59")).toBe("11:59 pm");
  });
});

describe("isIndianMobile", () => {
  it("accepts a ten-digit mobile however it is typed", () => {
    for (const ok of ["9876543210", " 98765 43210 ", "98765-43210", "(98765) 43210", "+91 98765 43210", "919876543210", "09876543210"]) {
      expect(isIndianMobile(ok), ok).toBe(true);
    }
  });

  it("accepts every valid first digit", () => {
    for (const first of "6789") expect(isIndianMobile(`${first}123456789`)).toBe(true);
  });

  it("rejects anything else", () => {
    for (const bad of ["", "   ", "12345", "98765432", "987654321012", "5876543210", "0123456789", "abcdefghij", "98765 4321a", "+1 415 555 2671", "+91 5876543210"]) {
      expect(isIndianMobile(bad), bad).toBe(false);
    }
  });
});

describe("confirmationText", () => {
  it("tells the shopper who will confirm, what, when and how - and that nothing is booked yet", () => {
    expect(confirmationText("Glow Salon", "Haircut (Men)", "2026-10-21", "10:30", "98765 43210")).toBe(
      "Request sent! Glow Salon will confirm your appointment for Haircut (Men) on Wed 21 Oct at 10:30 am by WhatsApp or a call on 98765 43210. Nothing is booked until they confirm.",
    );
  });
});
