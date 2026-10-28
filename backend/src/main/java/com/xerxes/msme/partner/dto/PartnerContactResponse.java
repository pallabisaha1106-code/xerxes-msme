package com.xerxes.msme.partner.dto;

/** What a shop owner sees of their partner: just a name and a WhatsApp number (digits with the country code). */
public record PartnerContactResponse(String name, String whatsapp) {}
