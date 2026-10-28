import { describe, expect, it } from "vitest";
import { isValidEmail } from "./emailFormat";

describe("isValidEmail", () => {
  it("accepts ordinary addresses", () => {
    expect(isValidEmail("ok@example.com")).toBe(true);
    expect(isValidEmail("anita.sharma+shop@mail.co.in")).toBe(true);
    expect(isValidEmail("  padded@example.com  ")).toBe(true);
  });

  it("rejects addresses without a real domain", () => {
    expect(isValidEmail("a@b")).toBe(false);
    expect(isValidEmail("test@test")).toBe(false);
    expect(isValidEmail("user@localhost")).toBe(false);
    expect(isValidEmail("a@b.c")).toBe(false);
  });

  it("rejects malformed addresses", () => {
    expect(isValidEmail("")).toBe(false);
    expect(isValidEmail("plainaddress")).toBe(false);
    expect(isValidEmail("user@@x.com")).toBe(false);
    expect(isValidEmail("user name@x.com")).toBe(false);
    expect(isValidEmail("x@.com")).toBe(false);
  });
});
