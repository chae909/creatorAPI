package com.example.settlement.application.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SettlementUseCase {

    record MonthlySettlementQuery(String creatorId, String yearMonth) {}

    record MonthlySettlementResponse(
            String creatorId, String creatorName, int year, int month,
            long totalSales, long totalRefunds, long netSales,
            BigDecimal feeRate, long feeAmount, long payoutAmount,
            int saleCount, int cancelCount, String status) {}

    record AdminSettlementQuery(LocalDate from, LocalDate to) {}

    record AdminSettlementItem(
            String creatorId, String creatorName, int year, int month,
            long totalSales, long totalRefunds, long netSales,
            BigDecimal feeRate, long feeAmount, long payoutAmount,
            int saleCount, int cancelCount, String status) {}

    record AdminSettlementSummary(List<AdminSettlementItem> items, long grandTotal) {}

    MonthlySettlementResponse getMonthly(MonthlySettlementQuery query);

    MonthlySettlementResponse confirm(String creatorId, String yearMonth);

    MonthlySettlementResponse pay(String creatorId, String yearMonth);

    AdminSettlementSummary getAdminSummary(AdminSettlementQuery query);
}
