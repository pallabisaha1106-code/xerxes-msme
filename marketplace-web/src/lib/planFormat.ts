import type { MembershipType } from "../api/vendorOnboardingApi";
import type { MembershipPlanDto } from "../api/marketplaceApi";

export const PLAN_LABEL: Record<MembershipType, string> = { FREE: "Free", PLUS: "Plus", PREMIUM: "Premium" };

/** "₹0", "₹199", "₹1,499" - the plan's monthly price in rupees, without GST. */
export function priceText(plan: MembershipPlanDto): string {
  return `₹${(plan.monthlyAmountPaise / 100).toLocaleString("en-IN")}`;
}

/** "Up to 100 products". */
export function productLimitText(plan: MembershipPlanDto): string {
  return `Up to ${plan.productLimit} products`;
}

/** Paid plans are quoted without GST; Free has nothing to add it to. */
export function isPaid(plan: MembershipPlanDto): boolean {
  return plan.monthlyAmountPaise > 0;
}

/** The plan picker's one-line description, e.g. "Up to 100 products in your catalogue. ₹199/month + GST." */
export function pickerDescription(plan: MembershipPlanDto): string {
  return `${productLimitText(plan)} in your catalogue. ${priceText(plan)}/month${isPaid(plan) ? " + GST" : ""}.`;
}
