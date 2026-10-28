package com.xerxes.msme.admin.dto;

import com.xerxes.msme.tenant.MembershipType;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantPlanRequest(@NotNull MembershipType subscriptionPlan) {}
