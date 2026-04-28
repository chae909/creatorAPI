package com.example.settlement.presentation;

import com.example.settlement.application.feepolicy.FeePolicyUseCase;
import com.example.settlement.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/fee-policies")
@Validated
public class FeePolicyController {

    private final FeePolicyUseCase feePolicyUseCase;

    public FeePolicyController(FeePolicyUseCase feePolicyUseCase) {
        this.feePolicyUseCase = feePolicyUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeePolicyUseCase.FeePolicyResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(feePolicyUseCase.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeePolicyUseCase.FeePolicyResponse>> create(
            @Valid @RequestBody CreateFeePolicyRequest req) {
        FeePolicyUseCase.FeePolicyResponse response = feePolicyUseCase.create(
                new FeePolicyUseCase.CreateFeePolicyCommand(req.feeRate(), req.effectiveFrom()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    record CreateFeePolicyRequest(
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal feeRate,
            @NotNull LocalDate effectiveFrom) {}
}
