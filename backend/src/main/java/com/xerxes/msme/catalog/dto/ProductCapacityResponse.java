package com.xerxes.msme.catalog.dto;

/** How many products the vendor's plan allows, how many exist, and how many more can be added. */
public record ProductCapacityResponse(int limit, long used, long remaining) {}
