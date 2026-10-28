package com.xerxes.msme.sales;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_number_sequences")
@Getter
@Setter
public class InvoiceNumberSequence {

    @EmbeddedId
    private InvoiceNumberSequenceId id;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;
}
