package com.example.settlement.domain.settlement;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(name = "total_sales", nullable = false)
    private Long totalSales;

    @Column(name = "total_refunds", nullable = false)
    private Long totalRefunds;

    @Column(name = "net_sales", nullable = false)
    private Long netSales;

    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    @Column(name = "fee_amount", nullable = false)
    private Long feeAmount;

    @Column(name = "payout_amount", nullable = false)
    private Long payoutAmount;

    @Column(name = "sale_count", nullable = false)
    private int saleCount;

    @Column(name = "cancel_count", nullable = false)
    private int cancelCount;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Settlement() {}

    public Settlement(String creatorId, int year, int month,
                      Long totalSales, Long totalRefunds, Long netSales,
                      BigDecimal feeRate, Long feeAmount, Long payoutAmount,
                      int saleCount, int cancelCount, Instant now) {
        this.creatorId = creatorId;
        this.year = year;
        this.month = month;
        this.status = SettlementStatus.PENDING;
        this.totalSales = totalSales;
        this.totalRefunds = totalRefunds;
        this.netSales = netSales;
        this.feeRate = feeRate;
        this.feeAmount = feeAmount;
        this.payoutAmount = payoutAmount;
        this.saleCount = saleCount;
        this.cancelCount = cancelCount;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void confirm(Instant now) {
        if (this.status != SettlementStatus.PENDING)
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        this.status = status.next();
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    public void pay(Instant now) {
        if (this.status != SettlementStatus.CONFIRMED)
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        this.status = status.next();
        this.paidAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getCreatorId() { return creatorId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public SettlementStatus getStatus() { return status; }
    public Long getTotalSales() { return totalSales; }
    public Long getTotalRefunds() { return totalRefunds; }
    public Long getNetSales() { return netSales; }
    public BigDecimal getFeeRate() { return feeRate; }
    public Long getFeeAmount() { return feeAmount; }
    public Long getPayoutAmount() { return payoutAmount; }
    public int getSaleCount() { return saleCount; }
    public int getCancelCount() { return cancelCount; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
