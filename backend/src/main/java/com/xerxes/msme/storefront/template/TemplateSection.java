package com.xerxes.msme.storefront.template;

import java.util.List;

/** A group of related fields, e.g. "Hero Banner" or "Contact & Store Info" - purely for organizing the admin form. */
public record TemplateSection(String key, String label, List<TemplateField> fields) {}
