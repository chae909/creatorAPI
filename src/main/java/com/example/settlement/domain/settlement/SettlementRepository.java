package com.example.settlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByCreatorIdAndYearAndMonth(String creatorId, int year, int month);
    List<Settlement> findByCreatorIdAndYearAndMonthBetween(String creatorId, int year, int fromMonth, int toMonth);

    @Query("SELECT s FROM Settlement s WHERE " +
           "(s.year > :fromYear OR (s.year = :fromYear AND s.month >= :fromMonth)) AND " +
           "(s.year < :toYear OR (s.year = :toYear AND s.month <= :toMonth))")
    List<Settlement> findAllInYearMonthRange(
            @Param("fromYear") int fromYear, @Param("fromMonth") int fromMonth,
            @Param("toYear") int toYear, @Param("toMonth") int toMonth);
}
