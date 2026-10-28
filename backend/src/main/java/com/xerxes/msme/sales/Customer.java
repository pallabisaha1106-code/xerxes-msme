package com.xerxes.msme.sales;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String gstin;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "contact_person")
    private String contactPerson;

    private String phone;

    private String email;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;

    private String pincode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
