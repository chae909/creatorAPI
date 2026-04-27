package com.example.settlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {
    List<CancelRecord> findBySaleRecordIdIn(List<String> saleIds);
    Optional<CancelRecord> findBySaleRecordId(String saleRecordId);
    List<CancelRecord> findByCancelledAtGreaterThanEqualAndCancelledAtLessThanAndSaleRecordIdIn(Instant from, Instant to, List<String> saleRecordIds);

    @Query("""
            SELECT c FROM CancelRecord c
            WHERE c.saleRecordId IN (
                SELECT s.id FROM SaleRecord s WHERE s.courseId IN :courseIds
            )
            AND c.cancelledAt >= :from AND c.cancelledAt < :end
            """)
    List<CancelRecord> findByCourseIdsAndCancelledAtRange(
            @Param("courseIds") List<String> courseIds,
            @Param("from") Instant from,
            @Param("end") Instant end);
}
