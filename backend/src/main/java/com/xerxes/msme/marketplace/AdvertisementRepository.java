package com.xerxes.msme.marketplace;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository extends JpaRepository<Advertisement, UUID> {

    /** All ads for a slot, incl. drafts/expired/future-scheduled - for Super Admin management. */
    List<Advertisement> findAllBySlotOrderByStartsAtDesc(AdvertisementSlot slot);

    /**
     * The one ad actually shown for a slot right now, if any: published and
     * within its date range. Ordered by startsAt desc so that if a Super
     * Admin publishes a new ad for a slot that already has one live, the
     * newer one wins immediately regardless of the older one's end date -
     * see AdminAdvertisementService.publish().
     */
    Optional<Advertisement> findFirstBySlotAndPublishedTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtDesc(
            AdvertisementSlot slot, OffsetDateTime startsAtNow, OffsetDateTime endsAtNow);
}
