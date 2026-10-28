package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * A Super Admin-managed advertisement for one of the marketplace's 3 fixed
 * slots (see AdvertisementSlot). "Currently live" is computed at read time
 * (published=true AND now() between startsAt/endsAt) rather than stored, so
 * an ad expires on its own without a background job - see
 * AdvertisementRepository.findCurrentlyLive. imagePath is nullable because a
 * row exists briefly between creation and its follow-up photo upload (same
 * two-step pattern as DefaultProduct) - publish() requires one to be set.
 */
@Entity
@Table(name = "advertisements")
@Getter
@Setter
public class Advertisement extends MutableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdvertisementSlot slot;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "alt_text", nullable = false)
    private String altText;

    private String headline;

    private String description;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;
}
