package com.example.settlement.domain.sale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {
    List<SaleRecord> findByCourseIdInAndPaidAtGreaterThanEqualAndPaidAtLessThan(List<String> courseIds, Instant from, Instant to);
    Page<SaleRecord> findByCourseIdInAndPaidAtGreaterThanEqualAndPaidAtLessThan(List<String> courseIds, Instant from, Instant to, Pageable pageable);
    List<SaleRecord> findByCourseIdIn(List<String> courseIds);
}
