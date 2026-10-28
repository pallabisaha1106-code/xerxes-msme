import { describe, expect, it } from "vitest";
import type { AppointmentRequest, AppointmentStatus } from "../../api/appointmentApi";
import { callLink, whatsappLink, whatsappMessage } from "./contactLinks";

const request = (status: AppointmentStatus, overrides: Partial<AppointmentRequest> = {}): AppointmentRequest => ({
  id: "1",
  serviceName: "Haircut (Men)",
  price: 300,
  customerName: "Asha",
  customerPhone: "9876543210",
  note: null,
  date: "2026-10-21",
  time: "10:30",
  endsAt: "11:00",
  status,
  othersAtSameTime: 0,
  timeAlreadyBooked: false,
  requestedAt: "2026-10-19T04:30:00Z",
  ...overrides,
});

describe("callLink", () => {
  it("adds the country code to a ten-digit number", () => {
    expect(callLink("9876543210")).toBe("tel:+919876543210");
  });

  it("keeps digits only and leaves other lengths as they are", () => {
    expect(callLink("98765 43210")).toBe("tel:+919876543210");
    expect(callLink("+44 7911 123456")).toBe("tel:+447911123456");
  });
});

describe("whatsappMessage", () => {
  it("confirms an approved appointment", () => {
    expect(whatsappMessage(request("APPROVED"), "Glow Salon")).toBe(
      "Hi Asha, your appointment for Haircut (Men) on Wed 21 Oct at 10:30 is confirmed. See you at Glow Salon!",
    );
  });

  it("apologises for a rejected request, inviting another time", () => {
    expect(whatsappMessage(request("REJECTED"), "Glow Salon")).toBe(
      "Hi Asha, sorry, we can't take your request for Haircut (Men) on Wed 21 Oct at 10:30. Please request another time.",
    );
  });

  it("tells a customer about a cancellation", () => {
    expect(whatsappMessage(request("CANCELLED"), "Glow Salon")).toContain("we have to cancel your appointment for Haircut (Men) on Wed 21 Oct at 10:30");
  });

  it("opens the conversation for a waiting or expired request", () => {
    const expected = "Hi Asha, this is Glow Salon. About your request for Haircut (Men) on Wed 21 Oct at 10:30.";
    expect(whatsappMessage(request("PENDING"), "Glow Salon")).toBe(expected);
    expect(whatsappMessage(request("EXPIRED"), "Glow Salon")).toBe(expected);
  });
});

describe("whatsappLink", () => {
  it("points at the customer's number with the message encoded", () => {
    const link = whatsappLink(request("APPROVED"), "Glow & Co");
    const url = new URL(link);
    expect(url.origin + url.pathname).toBe("https://wa.me/919876543210");
    expect(url.searchParams.get("text")).toBe(whatsappMessage(request("APPROVED"), "Glow & Co"));
    expect(link).toContain("Glow%20%26%20Co");
  });
});
