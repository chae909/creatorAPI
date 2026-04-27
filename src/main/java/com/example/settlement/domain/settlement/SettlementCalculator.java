package com.example.settlement.domain.settlement;

import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.SaleRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class SettlementCalculator {

    private SettlementCalculator() {}

    public record SettlementResult(
            Long totalSales,
            Long totalRefunds,
            Long netSales,
            Long feeAmount,
            Long payoutAmount,
            int saleCount,
            int cancelCount
    ) {}

    public static SettlementResult calculate(List<SaleRecord> sales, List<CancelRecord> cancels, BigDecimal feeRate) {
        long totalSales = sales.stream().mapToLong(SaleRecord::getAmount).sum();
        long totalRefunds = cancels.stream().mapToLong(CancelRecord::getRefundAmount).sum();
        long netSales = totalSales - totalRefunds;
        long feeAmount = BigDecimal.valueOf(netSales)
                .multiply(feeRate)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long payoutAmount = netSales - feeAmount;
        return new SettlementResult(totalSales, totalRefunds, netSales, feeAmount, payoutAmount,
                sales.size(), cancels.size());
    }
}
