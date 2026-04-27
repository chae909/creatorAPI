package com.example.settlement.presentation;

import com.example.settlement.application.sale.SaleUseCase;
import com.example.settlement.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/sales")
@Validated
public class SaleController {

    private final SaleUseCase saleUseCase;

    public SaleController(SaleUseCase saleUseCase) {
        this.saleUseCase = saleUseCase;
    }

    private record RegisterSaleRequest(
            @NotBlank(message = "강의 ID는 필수입니다.") String courseId,
            @NotBlank(message = "수강생 ID는 필수입니다.") String studentId,
            @Positive(message = "결제 금액은 0보다 커야 합니다.") long amount,
            @NotBlank(message = "결제 일시는 필수입니다.") String paidAt) {}

    private record CancelSaleRequest(
            @Positive(message = "환불 금액은 0보다 커야 합니다.") long refundAmount,
            @NotBlank(message = "취소 일시는 필수입니다.") String cancelledAt) {}

    @PostMapping
    public ResponseEntity<ApiResponse<SaleUseCase.SaleRecordResponse>> register(
            @RequestBody @Valid RegisterSaleRequest request) {
        Instant paidAt = OffsetDateTime.parse(request.paidAt()).toInstant();
        var cmd = new SaleUseCase.RegisterSaleCommand(
                request.courseId(), request.studentId(), request.amount(), paidAt);
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.register(cmd)));
    }

    @PostMapping("/{saleId}/cancel")
    public ResponseEntity<ApiResponse<SaleUseCase.SaleRecordResponse>> cancel(
            @PathVariable String saleId,
            @RequestBody @Valid CancelSaleRequest request) {
        Instant cancelledAt = OffsetDateTime.parse(request.cancelledAt()).toInstant();
        var cmd = new SaleUseCase.CancelSaleCommand(saleId, request.refundAmount(), cancelledAt);
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.cancel(cmd)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SaleUseCase.SaleRecordResponse>>> list(
            @RequestParam String creatorId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Instant fromInstant = OffsetDateTime.parse(from).toInstant();
        Instant toInstant = OffsetDateTime.parse(to).toInstant();
        var query = new SaleUseCase.SaleListQuery(creatorId, fromInstant, toInstant);
        var pageable = PageRequest.of(page, size, Sort.by("paidAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.list(query, pageable)));
    }
}
