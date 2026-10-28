package com.xerxes.msme.marketplace.dto;

/** All 3 slots in one response - each field is null if that slot has no currently-live ad, in which case marketplace-web shows its default fallback message. */
public record PublicAdvertisementsResponse(
        AdvertisementResponse topCarousel, AdvertisementResponse leftRail, AdvertisementResponse rightRail) {}
