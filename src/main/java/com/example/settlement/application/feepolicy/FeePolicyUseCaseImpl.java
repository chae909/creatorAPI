package com.example.settlement.application.feepolicy;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;
import com.example.settlement.domain.settlement.FeePolicy;
import com.example.settlement.domain.settlement.FeePolicyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeePolicyUseCaseImpl implements FeePolicyUseCase {

    private final FeePolicyRepository feePolicyRepository;
    private final Clock clock;

    public FeePolicyUseCaseImpl(FeePolicyRepository feePolicyRepository, Clock clock) {
        this.feePolicyRepository = feePolicyRepository;
        this.clock = clock;
    }

    @Override
    public List<FeePolicyResponse> getAll() {
        return feePolicyRepository.findAllByOrderByEffectiveFromDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FeePolicyResponse create(CreateFeePolicyCommand cmd) {
        try {
            FeePolicy saved = feePolicyRepository.saveAndFlush(
                    new FeePolicy(cmd.feeRate(), cmd.effectiveFrom(), Instant.now(clock)));
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.FEE_POLICY_DUPLICATE_DATE);
        }
    }

    private FeePolicyResponse toResponse(FeePolicy policy) {
        String feeRatePercent = policy.getFeeRate()
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString() + "%";
        return new FeePolicyResponse(policy.getId(), policy.getFeeRate(), feeRatePercent, policy.getEffectiveFrom());
    }
}
