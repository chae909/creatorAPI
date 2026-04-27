package com.example.settlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {
    List<CancelRecord> findBySaleRecordIdIn(List<String> saleIds);
    Optional<CancelRecord> findBySaleRecordId(String saleRecordId);
    List<CancelRecord> findByCancelledAtBetweenAndSaleRecordIdIn(Instant from, Instant to, List<String> saleRecordIds);
}
