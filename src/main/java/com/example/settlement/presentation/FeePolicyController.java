package com.example.settlement.presentation;

import com.example.settlement.domain.settlement.FeePolicy;
import com.example.settlement.domain.settlement.FeePolicyRepository;
import com.example.settlement.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/fee-policies")
@Validated
public class FeePolicyController {

    private final FeePolicyRepository feePolicyRepository;

    public FeePolicyController(FeePolicyRepository feePolicyRepository) {
        this.feePolicyRepository = feePolicyRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeePolicyResponse>>> list() {
        List<FeePolicyResponse> result = feePolicyRepository.findAllByOrderByEffectiveFromDesc()
                .stream()
                .map(FeePolicyController::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeePolicyResponse>> create(@Valid @RequestBody CreateFeePolicyRequest req) {
        FeePolicy saved = feePolicyRepository.save(
                new FeePolicy(req.feeRate(), req.effectiveFrom(), Instant.now()));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(saved)));
    }

    private static FeePolicyResponse toResponse(FeePolicy p) {
        String pct = p.getFeeRate().multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros().toPlainString() + "%";
        return new FeePolicyResponse(p.getId(), p.getFeeRate(), pct, p.getEffectiveFrom());
    }

    record FeePolicyResponse(Long id, BigDecimal feeRate, String feeRatePercent, LocalDate effectiveFrom) {}

    record CreateFeePolicyRequest(
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal feeRate,
            @NotNull LocalDate effectiveFrom) {}
}
