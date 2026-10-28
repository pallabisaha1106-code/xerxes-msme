package com.xerxes.msme.partner;

/**
 * APPLIED: asked to become a partner, waiting for Super Admin (the login is switched off).
 * ACTIVE: approved - can sign in and is given new shops in the areas they cover.
 * INACTIVE: approved once, now switched off by Super Admin.
 * REJECTED: the application was turned down (see decisionReason on Partner).
 */
public enum PartnerStatus {
    APPLIED,
    ACTIVE,
    INACTIVE,
    REJECTED
}
