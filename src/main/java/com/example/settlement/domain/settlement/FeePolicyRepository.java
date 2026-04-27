package com.example.settlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface FeePolicyRepository extends JpaRepository<FeePolicy, Long> {
    Optional<FeePolicy> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}
