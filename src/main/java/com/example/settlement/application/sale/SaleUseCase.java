package com.example.settlement.application.sale;

import java.time.Instant;
import java.util.List;

public interface SaleUseCase {

    record RegisterSaleCommand(String courseId, String studentId, long amount, Instant paidAt) {}

    record CancelSaleCommand(String saleRecordId, long refundAmount, Instant cancelledAt) {}

    record SaleRecordResponse(
            String id, String courseId, String creatorId, String studentId,
            long amount, Instant paidAt, boolean cancelled, Long refundAmount, Instant cancelledAt) {}

    record SaleListQuery(String creatorId, Instant from, Instant to) {}

    SaleRecordResponse register(RegisterSaleCommand cmd);

    SaleRecordResponse cancel(CancelSaleCommand cmd);

    List<SaleRecordResponse> list(SaleListQuery query);
}
