package com.xerxes.msme.user;

public enum Role {
    OWNER,
    STAFF,
    /**
     * Platform-staff role, not a tenant role: belongs to the sentinel
     * "platform" tenant (see Tenant.isPlatform / V5 migration) rather than a
     * real business. Authorizes /api/admin/** only - see SecurityConfig.
     */
    SUPER_ADMIN,
    /**
     * An independent local partner: like SUPER_ADMIN, a platform-level role that belongs to the sentinel platform
     * tenant rather than a business. Looks after (and reviews) the shops assigned to them - see the partner package.
     */
    PARTNER
}
