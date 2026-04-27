package com.example.settlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByCreatorIdAndYearAndMonth(String creatorId, int year, int month);
    List<Settlement> findByCreatorIdAndYearAndMonthBetween(String creatorId, int year, int fromMonth, int toMonth);
}
