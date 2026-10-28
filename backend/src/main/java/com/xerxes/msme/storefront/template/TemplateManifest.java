package com.xerxes.msme.storefront.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes one storefront template: its identity plus every piece of text
 * it lets a tenant configure. The admin content-editing form is generated
 * entirely from this - adding a field to a template means editing its
 * manifest.json, not writing new frontend code. See StorefrontTemplateRegistry.
 */
public record TemplateManifest(String id, String name, String description, List<TemplateSection> sections) {

    public Map<String, String> defaultValues() {
        Map<String, String> defaults = new LinkedHashMap<>();
        for (TemplateSection section : sections) {
            for (TemplateField field : section.fields()) {
                defaults.put(field.key(), field.defaultValue() == null ? "" : field.defaultValue());
            }
        }
        return defaults;
    }
}
