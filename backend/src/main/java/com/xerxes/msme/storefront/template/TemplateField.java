package com.xerxes.msme.storefront.template;

/** One configurable text field a template exposes, e.g. "hero.headline". Rendered as a form field by the admin UI. */
public record TemplateField(String key, String label, String type, String defaultValue) {}
