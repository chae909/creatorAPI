package com.example.settlement.presentation;

import com.example.settlement.application.settlement.SettlementUseCase;
import com.example.settlement.presentation.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@Validated
public class SettlementController {

    private final SettlementUseCase settlementUseCase;
    private final CsvExportService csvExportService;

    public SettlementController(SettlementUseCase settlementUseCase, CsvExportService csvExportService) {
        this.settlementUseCase = settlementUseCase;
        this.csvExportService = csvExportService;
    }

    private static final String YEAR_MONTH_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";
    private static final String YEAR_MONTH_MESSAGE = "연월 형식이 올바르지 않습니다. (예: 2025-03)";
    private static final String DATE_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
    private static final String DATE_MESSAGE = "날짜 형식이 올바르지 않습니다. (예: 2025-03-01)";

    @GetMapping("/api/settlements/monthly")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> getMonthly(
            @RequestParam @NotBlank(message = "크리에이터 ID는 필수입니다.") String creatorId,
            @RequestParam @Pattern(regexp = YEAR_MONTH_PATTERN, message = YEAR_MONTH_MESSAGE) String yearMonth) {
        var query = new SettlementUseCase.MonthlySettlementQuery(creatorId, yearMonth);
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.getMonthly(query)));
    }

    @PostMapping("/api/settlements/confirm")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> confirm(
            @RequestParam @NotBlank(message = "크리에이터 ID는 필수입니다.") String creatorId,
            @RequestParam @Pattern(regexp = YEAR_MONTH_PATTERN, message = YEAR_MONTH_MESSAGE) String yearMonth) {
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.confirm(creatorId, yearMonth)));
    }

    @PostMapping("/api/settlements/pay")
    public ResponseEntity<ApiResponse<SettlementUseCase.MonthlySettlementResponse>> pay(
            @RequestParam @NotBlank(message = "크리에이터 ID는 필수입니다.") String creatorId,
            @RequestParam @Pattern(regexp = YEAR_MONTH_PATTERN, message = YEAR_MONTH_MESSAGE) String yearMonth) {
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.pay(creatorId, yearMonth)));
    }

    @GetMapping("/api/admin/settlements")
    public ResponseEntity<?> getAdminSummary(
            @RequestParam @Pattern(regexp = DATE_PATTERN, message = DATE_MESSAGE) String from,
            @RequestParam @Pattern(regexp = DATE_PATTERN, message = DATE_MESSAGE) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        LocalDate fromDate, toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("올바르지 않은 날짜 형식입니다. yyyy-MM-dd 형식을 사용하세요."));
        }
        var query = new SettlementUseCase.AdminSettlementQuery(fromDate, toDate);
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(settlementUseCase.getAdminSummary(query, pageable)));
    }

    @GetMapping("/api/admin/settlements/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam @Pattern(regexp = DATE_PATTERN, message = DATE_MESSAGE) String from,
            @RequestParam @Pattern(regexp = DATE_PATTERN, message = DATE_MESSAGE) String to) {
        LocalDate fromDate, toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("올바르지 않은 날짜 형식입니다. yyyy-MM-dd 형식을 사용하세요.");
        }
        var query = new SettlementUseCase.AdminSettlementQuery(fromDate, toDate);
        var summary = settlementUseCase.getAdminSummary(query, Pageable.unpaged());
        String csv = csvExportService.toCsv(summary);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition",
                        "attachment; filename=\"settlements_" + from + "_" + to + ".csv\"")
                .body(csv);
    }
}
