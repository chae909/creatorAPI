package com.example.settlement.application.feepolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FeePolicyUseCase {

    record CreateFeePolicyCommand(BigDecimal feeRate, LocalDate effectiveFrom) {}

    record FeePolicyResponse(Long id, BigDecimal feeRate, String feeRatePercent, LocalDate effectiveFrom) {}

    List<FeePolicyResponse> getAll();

    FeePolicyResponse create(CreateFeePolicyCommand cmd);
}
