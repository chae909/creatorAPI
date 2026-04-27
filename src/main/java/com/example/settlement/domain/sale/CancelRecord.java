package com.example.settlement.domain.sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cancel_records")
public class CancelRecord {

    @Id
    private String id;

    @Column(name = "sale_record_id", nullable = false, unique = true)
    private String saleRecordId;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CancelRecord() {}

    public CancelRecord(String id, String saleRecordId, Long refundAmount, Instant cancelledAt, Instant createdAt) {
        this.id = id;
        this.saleRecordId = saleRecordId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSaleRecordId() { return saleRecordId; }
    public Long getRefundAmount() { return refundAmount; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
}
