package com.example.settlement.presentation;

import com.example.settlement.application.settlement.SettlementUseCase;
import com.example.settlement.presentation.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
public class SettlementController {

    private final SettlementUseCase settlementUseCase;
    private final CsvExportService csvExportService;

    public SettlementController(SettlementUseCase settlementUseCase, CsvExportService csvExportService) {
        this.settlementUseCase = settlementUseCase;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/api/settlements/monthly")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> getMonthly(
            @RequestParam String creatorId,
            @RequestParam String yearMonth) {
        var query = new SettlementUseCase.MonthlySettlementQuery(creatorId, yearMonth);
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.getMonthly(query)));
    }

    @PostMapping("/api/settlements/confirm")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> confirm(
            @RequestParam String creatorId,
            @RequestParam String yearMonth) {
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.confirm(creatorId, yearMonth)));
    }

    @PostMapping("/api/settlements/pay")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> pay(
            @RequestParam String creatorId,
            @RequestParam String yearMonth) {
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.pay(creatorId, yearMonth)));
    }

    @GetMapping("/api/admin/settlements")
    public ResponseEntity<?> getAdminSummary(
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate, toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("올바르지 않은 날짜 형식입니다. yyyy-MM-dd 형식을 사용하세요."));
        }
        var query = new SettlementUseCase.AdminSettlementQuery(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.getAdminSummary(query)));
    }

    @GetMapping("/api/admin/settlements/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate, toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("올바르지 않은 날짜 형식입니다. yyyy-MM-dd 형식을 사용하세요.");
        }
        var query = new SettlementUseCase.AdminSettlementQuery(fromDate, toDate);
        var summary = settlementUseCase.getAdminSummary(query);
        String csv = csvExportService.toCsv(summary);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition",
                        "attachment; filename=\"settlements_" + from + "_" + to + ".csv\"")
                .body(csv);
    }
}
