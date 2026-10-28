package com.xerxes.msme.subscription.dto;

import com.xerxes.msme.tenant.MembershipType;

/** One membership plan as the public pricing page and the plan picker show it. The price is without GST. */
public record MembershipPlanResponse(MembershipType plan, long monthlyAmountPaise, int productLimit) {}
