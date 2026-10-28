package com.xerxes.msme.storefront;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One row per tenant: which template they've picked and their text overrides for it. See V4 migration. */
@Entity
@Table(name = "storefront_settings")
@Getter
@Setter
public class StorefrontSettings extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "template_id", nullable = false)
    private String templateId = "veera";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> content = new LinkedHashMap<>();

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
}
