import { describe, expect, it } from "vitest";
import { partnerFormProblems, type PartnerFormValues } from "./partnerApplication";

const complete: PartnerFormValues = {
  name: "Meera Kulkarni",
  whatsapp: "98765 43210",
  email: "meera@example.com",
  password: "SecurePass123",
  areaIds: ["area-1"],
};

describe("partnerFormProblems", () => {
  it("has nothing to say about a complete form", () => {
    expect(partnerFormProblems(complete)).toEqual([]);
  });

  it("lists every missing or wrong field, in form order", () => {
    expect(partnerFormProblems({ name: " ", whatsapp: "12345", email: "meera@test", password: "short", areaIds: [] })).toEqual([
      "your name",
      "a valid Indian mobile number for WhatsApp",
      "a valid email address (like name@example.com)",
      "a password of at least 8 characters",
      "at least one area you can cover",
    ]);
  });

  it("accepts a number with the country code and a plus sign", () => {
    expect(partnerFormProblems({ ...complete, whatsapp: "+91 98765 43210" })).toEqual([]);
  });
});
