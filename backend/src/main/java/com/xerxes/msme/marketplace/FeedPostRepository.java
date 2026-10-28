package com.xerxes.msme.marketplace;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    /** A vendor's own post history, incl. pending/rejected ones only they can see - see FeedPostService. */
    List<FeedPost> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** The 1-post-per-7-days cooldown check - see FeedPostService.create(). */
    Optional<FeedPost> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Platform staff's review queue - see AdminFeedPostService. */
    List<FeedPost> findAllByStatusOrderByCreatedAtDesc(FeedPostStatus status);

    List<FeedPost> findAllByOrderByCreatedAtDesc();

    /** A local partner's review queue: only their own shops' posts. */
    List<FeedPost> findAllByTenantIdInOrderByCreatedAtDesc(List<UUID> tenantIds);

    List<FeedPost> findAllByTenantIdInAndStatusOrderByCreatedAtDesc(List<UUID> tenantIds, FeedPostStatus status);

    /**
     * The public feed: approved posts from one of the (city- or city+area-) filtered tenantIds a prior
     * TenantRepository call resolved - see PublicMarketplaceService. No FK/@ManyToOne to Tenant on purpose
     * (same plain-UUID-FK convention used throughout this codebase), so the city/area filter is resolved
     * against TenantRepository first, not joined here.
     *
     * A post with a validUntil is visible through that day (inclusive); one without keeps the original rule,
     * visible while createdAt is after the cutoff (the 14-day window). The order comes from the Pageable's Sort
     * (see FeedSort) - deliberately no ORDER BY here, or it would fight the caller's.
     */
    @Query("""
            SELECT p FROM FeedPost p
            WHERE p.tenantId IN :tenantIds
              AND p.status = :status
              AND ((p.validUntil IS NULL AND p.createdAt > :cutoff) OR p.validUntil >= :today)
            """)
    Slice<FeedPost> findPublicFeedPage(
            @Param("tenantIds") List<UUID> tenantIds,
            @Param("status") FeedPostStatus status,
            @Param("cutoff") OffsetDateTime cutoff,
            @Param("today") LocalDate today,
            Pageable pageable);
}
