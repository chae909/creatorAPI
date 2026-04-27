package com.example.settlement.domain.settlement;

import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.SaleRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCalculatorTest {

    private static final Instant NOW = Instant.parse("2025-03-01T00:00:00Z");
    private static final BigDecimal RATE_20 = new BigDecimal("0.2000");
    private static final BigDecimal RATE_15 = new BigDecimal("0.1500");

    private SaleRecord sale(String id, long amount) {
        return new SaleRecord(id, "course-1", "student-1", amount, NOW, NOW);
    }

    private CancelRecord cancel(String id, String saleId, long refund) {
        return new CancelRecord(id, saleId, refund, NOW, NOW);
    }

    @Test
    void mainScenario_threesSalesWithTwoPartialRefunds() {
        var sales = List.of(sale("s1", 100000L), sale("s2", 80000L), sale("s3", 80000L));
        var cancels = List.of(cancel("c1", "s2", 80000L), cancel("c2", "s3", 30000L));

        var result = SettlementCalculator.calculate(sales, cancels, RATE_20);

        assertThat(result.totalSales()).isEqualTo(260000L);
        assertThat(result.totalRefunds()).isEqualTo(110000L);
        assertThat(result.netSales()).isEqualTo(150000L);
        assertThat(result.feeAmount()).isEqualTo(30000L);
        assertThat(result.payoutAmount()).isEqualTo(120000L);
        assertThat(result.saleCount()).isEqualTo(3);
        assertThat(result.cancelCount()).isEqualTo(2);
    }

    @Test
    void partialRefund_refundLessThanSaleAmount() {
        var sales = List.of(sale("s1", 100000L));
        var cancels = List.of(cancel("c1", "s1", 30000L));

        var result = SettlementCalculator.calculate(sales, cancels, RATE_20);

        assertThat(result.totalSales()).isEqualTo(100000L);
        assertThat(result.totalRefunds()).isEqualTo(30000L);
        assertThat(result.netSales()).isEqualTo(70000L);
        assertThat(result.feeAmount()).isEqualTo(14000L);
        assertThat(result.payoutAmount()).isEqualTo(56000L);
        assertThat(result.saleCount()).isEqualTo(1);
        assertThat(result.cancelCount()).isEqualTo(1);
    }

    @Test
    void emptySales_allFieldsAreZero() {
        var result = SettlementCalculator.calculate(List.of(), List.of(), RATE_20);

        assertThat(result.totalSales()).isZero();
        assertThat(result.totalRefunds()).isZero();
        assertThat(result.netSales()).isZero();
        assertThat(result.feeAmount()).isZero();
        assertThat(result.payoutAmount()).isZero();
        assertThat(result.saleCount()).isZero();
        assertThat(result.cancelCount()).isZero();
    }

    @Test
    void fifteenPercentFeeRate_feeAndPayoutCorrect() {
        var sales = List.of(sale("s1", 100000L));

        var result = SettlementCalculator.calculate(sales, List.of(), RATE_15);

        assertThat(result.feeAmount()).isEqualTo(15000L);
        assertThat(result.payoutAmount()).isEqualTo(85000L);
    }

    @Test
    void fifteenPercentFeeRate_downRoundingApplied() {
        // 100001 * 0.15 = 15000.15 → down → 15000
        var sales = List.of(sale("s1", 100001L));

        var result = SettlementCalculator.calculate(sales, List.of(), RATE_15);

        assertThat(result.feeAmount()).isEqualTo(15000L);
        assertThat(result.payoutAmount()).isEqualTo(85001L);
    }

    @Test
    void negative_net_sales_when_refunds_exceed_sales() {
        var sales = List.of(sale("s1", 50000L));
        var cancels = List.of(cancel("c1", "s1", 80000L));

        var result = SettlementCalculator.calculate(sales, cancels, RATE_20);

        assertThat(result.totalSales()).isEqualTo(50000L);
        assertThat(result.totalRefunds()).isEqualTo(80000L);
        assertThat(result.netSales()).isEqualTo(-30000L);
        assertThat(result.feeAmount()).isEqualTo(-6000L);
        assertThat(result.payoutAmount()).isEqualTo(-24000L);
    }

    @Test
    void negativeNetSales_downRoundingTowardZero_notFloor() {
        // netSales = 50000 - 80001 = -30001
        // -30001 * 0.2 = -6000.2
        // DOWN (toward zero) → -6000   (correct: creator owes less fee on a net loss)
        // FLOOR (toward -∞) → -6001   (wrong: would over-deduct fee)
        var sales = List.of(sale("s1", 50000L));
        var cancels = List.of(cancel("c1", "s1", 80001L));

        var result = SettlementCalculator.calculate(sales, cancels, RATE_20);

        assertThat(result.totalSales()).isEqualTo(50000L);
        assertThat(result.totalRefunds()).isEqualTo(80001L);
        assertThat(result.netSales()).isEqualTo(-30001L);
        assertThat(result.feeAmount()).isEqualTo(-6000L);   // DOWN, not FLOOR (-6001)
        assertThat(result.payoutAmount()).isEqualTo(-24001L);
    }
}
