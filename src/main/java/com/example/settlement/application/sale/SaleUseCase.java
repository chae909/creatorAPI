package com.example.settlement.application.sale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SaleUseCase {

    record RegisterSaleCommand(String courseId, String studentId, long amount, Instant paidAt) {}

    record CancelSaleCommand(String saleRecordId, long refundAmount, Instant cancelledAt) {}

    record SaleRecordResponse(
            String id, String courseId, String creatorId, String studentId,
            long amount, Instant paidAt, boolean cancelled, Long refundAmount, Instant cancelledAt) {}

    record SaleListQuery(String creatorId, Instant from, Instant to) {}

    SaleRecordResponse register(RegisterSaleCommand cmd);

    SaleRecordResponse cancel(CancelSaleCommand cmd);

    Page<SaleRecordResponse> list(SaleListQuery query, Pageable pageable);
}
