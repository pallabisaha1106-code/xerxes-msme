package com.xerxes.msme.tenant;

import java.util.UUID;

/** The sentinel "platform" tenant (see V5__platform_admin.sql) that Super Admin and partner logins belong to. */
public final class PlatformTenant {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PlatformTenant() {}
}
