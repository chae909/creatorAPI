package com.example.settlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {
    List<SaleRecord> findByCourseIdInAndPaidAtBetween(List<String> courseIds, Instant from, Instant to);
    List<SaleRecord> findByCourseIdIn(List<String> courseIds);
}
