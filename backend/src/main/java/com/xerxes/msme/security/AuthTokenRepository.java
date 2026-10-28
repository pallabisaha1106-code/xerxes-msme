package com.xerxes.msme.security;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    boolean existsByIdAndRevokedAtIsNull(UUID id);

    /** Ends every sign-in this user still has - used when their account is switched off. */
    @Modifying
    @Query("update AuthToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
