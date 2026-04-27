package com.example.settlement.domain.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fee_policies")
public class FeePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeePolicy() {}

    public FeePolicy(BigDecimal feeRate, LocalDate effectiveFrom, Instant createdAt) {
        this.feeRate = feeRate;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public BigDecimal getFeeRate() { return feeRate; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }
}
