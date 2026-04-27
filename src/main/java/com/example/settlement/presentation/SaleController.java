package com.example.settlement.presentation;

import com.example.settlement.application.sale.SaleUseCase;
import com.example.settlement.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleUseCase saleUseCase;

    public SaleController(SaleUseCase saleUseCase) {
        this.saleUseCase = saleUseCase;
    }

    private record RegisterSaleRequest(
            @NotBlank String courseId,
            @NotBlank String studentId,
            @Positive long amount,
            @NotBlank String paidAt) {}

    private record CancelSaleRequest(
            @Positive long refundAmount,
            @NotBlank String cancelledAt) {}

    @PostMapping
    public ResponseEntity<?> register(@RequestBody @Valid RegisterSaleRequest request) {
        Instant paidAt;
        try {
            paidAt = OffsetDateTime.parse(request.paidAt()).toInstant();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("올바르지 않은 날짜 형식입니다. ISO 8601 형식을 사용하세요."));
        }
        var cmd = new SaleUseCase.RegisterSaleCommand(
                request.courseId(), request.studentId(), request.amount(), paidAt);
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.register(cmd)));
    }

    @PostMapping("/{saleId}/cancel")
    public ResponseEntity<?> cancel(
            @PathVariable String saleId,
            @RequestBody @Valid CancelSaleRequest request) {
        Instant cancelledAt;
        try {
            cancelledAt = OffsetDateTime.parse(request.cancelledAt()).toInstant();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("올바르지 않은 날짜 형식입니다. ISO 8601 형식을 사용하세요."));
        }
        var cmd = new SaleUseCase.CancelSaleCommand(saleId, request.refundAmount(), cancelledAt);
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.cancel(cmd)));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam String creatorId,
            @RequestParam String from,
            @RequestParam String to) {
        Instant fromInstant, toInstant;
        try {
            fromInstant = OffsetDateTime.parse(from).toInstant();
            toInstant = OffsetDateTime.parse(to).toInstant();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("올바르지 않은 날짜 형식입니다. ISO 8601 형식을 사용하세요."));
        }
        var query = new SaleUseCase.SaleListQuery(creatorId, fromInstant, toInstant);
        return ResponseEntity.ok(ApiResponse.ok(saleUseCase.list(query)));
    }
}
