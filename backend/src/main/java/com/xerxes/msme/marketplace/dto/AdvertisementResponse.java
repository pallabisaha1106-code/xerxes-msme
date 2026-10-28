package com.xerxes.msme.marketplace.dto;

public record AdvertisementResponse(
        String imageUrl, String linkUrl, String altText, String headline, String description) {}
