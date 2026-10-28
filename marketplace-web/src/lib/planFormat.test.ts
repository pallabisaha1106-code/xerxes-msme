import { describe, expect, it } from "vitest";
import type { MembershipPlanDto } from "../api/marketplaceApi";
import { isPaid, pickerDescription, priceText, productLimitText } from "./planFormat";

const free: MembershipPlanDto = { plan: "FREE", monthlyAmountPaise: 0, productLimit: 10 };
const plus: MembershipPlanDto = { plan: "PLUS", monthlyAmountPaise: 19_900, productLimit: 100 };
const big: MembershipPlanDto = { plan: "PREMIUM", monthlyAmountPaise: 149_900, productLimit: 500 };

describe("planFormat", () => {
  it("writes the price in rupees from paise", () => {
    expect(priceText(free)).toBe("₹0");
    expect(priceText(plus)).toBe("₹199");
    expect(priceText(big)).toBe("₹1,499");
  });

  it("writes the product limit", () => {
    expect(productLimitText(plus)).toBe("Up to 100 products");
  });

  it("only paid plans are quoted plus GST", () => {
    expect(isPaid(free)).toBe(false);
    expect(isPaid(plus)).toBe(true);
    expect(pickerDescription(free)).toBe("Up to 10 products in your catalogue. ₹0/month.");
    expect(pickerDescription(plus)).toBe("Up to 100 products in your catalogue. ₹199/month + GST.");
  });
});
