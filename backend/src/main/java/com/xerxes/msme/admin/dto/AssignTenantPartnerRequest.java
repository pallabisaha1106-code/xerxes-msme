package com.xerxes.msme.admin.dto;

import java.util.UUID;

/** The partner to give a shop to; null takes the shop back to Super Admin's own queue. */
public record AssignTenantPartnerRequest(UUID partnerId) {}
