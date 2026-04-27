package com.example.settlement.application.settlement;

import com.example.settlement.domain.common.TimeRangeUtils;
import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;
import com.example.settlement.domain.creator.Creator;
import com.example.settlement.domain.creator.CourseRepository;
import com.example.settlement.domain.creator.CreatorRepository;
import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.CancelRecordRepository;
import com.example.settlement.domain.sale.SaleRecord;
import com.example.settlement.domain.sale.SaleRecordRepository;
import com.example.settlement.domain.settlement.FeePolicy;
import com.example.settlement.domain.settlement.FeePolicyRepository;
import com.example.settlement.domain.settlement.Settlement;
import com.example.settlement.domain.settlement.SettlementCalculator;
import com.example.settlement.domain.settlement.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SettlementUseCaseImpl implements SettlementUseCase {

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final SettlementRepository settlementRepository;

    public SettlementUseCaseImpl(CreatorRepository creatorRepository,
                                 CourseRepository courseRepository,
                                 SaleRecordRepository saleRecordRepository,
                                 CancelRecordRepository cancelRecordRepository,
                                 FeePolicyRepository feePolicyRepository,
                                 SettlementRepository settlementRepository) {
        this.creatorRepository = creatorRepository;
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancelRecordRepository = cancelRecordRepository;
        this.feePolicyRepository = feePolicyRepository;
        this.settlementRepository = settlementRepository;
    }

    @Override
    public MonthlySettlementResponse getMonthly(MonthlySettlementQuery query) {
        Creator creator = creatorRepository.findById(query.creatorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));
        Settlement settlement = findOrCreateSettlement(creator.getId(), query.yearMonth());
        return toMonthlyResponse(settlement, creator.getName());
    }

    @Override
    public MonthlySettlementResponse confirm(String creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));
        Settlement settlement = findOrCreateSettlement(creatorId, yearMonth);
        settlement.confirm(Instant.now());
        settlementRepository.save(settlement);
        return toMonthlyResponse(settlement, creator.getName());
    }

    @Override
    public MonthlySettlementResponse pay(String creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));
        YearMonth ym = TimeRangeUtils.parseYearMonth(yearMonth);
        Settlement settlement = settlementRepository
                .findByCreatorIdAndYearAndMonth(creatorId, ym.getYear(), ym.getMonthValue())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.pay(Instant.now());
        settlementRepository.save(settlement);
        return toMonthlyResponse(settlement, creator.getName());
    }

    @Override
    public AdminSettlementSummary getAdminSummary(AdminSettlementQuery query) {
        List<Settlement> settlements = settlementRepository.findAllInYearMonthRange(
                query.from().getYear(), query.from().getMonthValue(),
                query.to().getYear(), query.to().getMonthValue());

        List<String> creatorIds = settlements.stream()
                .map(Settlement::getCreatorId)
                .distinct()
                .toList();

        Map<String, String> creatorNames = creatorRepository.findAllById(creatorIds)
                .stream()
                .collect(Collectors.toMap(Creator::getId, Creator::getName));

        long grandTotal = settlements.stream()
                .mapToLong(Settlement::getPayoutAmount)
                .sum();

        List<AdminSettlementItem> items = settlements.stream()
                .map(s -> new AdminSettlementItem(
                        s.getCreatorId(),
                        creatorNames.getOrDefault(s.getCreatorId(), ""),
                        s.getYear(),
                        s.getMonth(),
                        s.getTotalSales(),
                        s.getTotalRefunds(),
                        s.getNetSales(),
                        s.getFeeRate(),
                        s.getFeeAmount(),
                        s.getPayoutAmount(),
                        s.getSaleCount(),
                        s.getCancelCount(),
                        s.getStatus().name()
                ))
                .toList();

        return new AdminSettlementSummary(items, grandTotal);
    }

    private Settlement findOrCreateSettlement(String creatorId, String yearMonth) {
        YearMonth ym = TimeRangeUtils.parseYearMonth(yearMonth);
        int year = ym.getYear();
        int month = ym.getMonthValue();
        return settlementRepository.findByCreatorIdAndYearAndMonth(creatorId, year, month)
                .orElseGet(() -> calculateAndSave(creatorId, year, month));
    }

    private Settlement calculateAndSave(String creatorId, int year, int month) {
        TimeRangeUtils.InstantRange range = TimeRangeUtils.toKstMonthRange(year, month);

        List<String> courseIds = courseRepository.findByCreatorId(creatorId)
                .stream()
                .map(c -> c.getId())
                .toList();

        List<SaleRecord> sales;
        List<CancelRecord> cancels;

        if (courseIds.isEmpty()) {
            sales = List.of();
            cancels = List.of();
        } else {
            sales = saleRecordRepository.findByCourseIdInAndPaidAtBetween(courseIds, range.start(), range.end());

            List<String> allSaleIds = saleRecordRepository.findByCourseIdIn(courseIds)
                    .stream()
                    .map(SaleRecord::getId)
                    .toList();

            cancels = allSaleIds.isEmpty() ? List.of()
                    : cancelRecordRepository.findByCancelledAtBetweenAndSaleRecordIdIn(
                            range.start(), range.end(), allSaleIds);
        }

        FeePolicy feePolicy = feePolicyRepository
                .findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.of(year, month, 1))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_DATE_RANGE));

        SettlementCalculator.SettlementResult result =
                SettlementCalculator.calculate(sales, cancels, feePolicy.getFeeRate());

        Instant now = Instant.now();
        Settlement settlement = new Settlement(
                creatorId, year, month,
                result.totalSales(), result.totalRefunds(), result.netSales(),
                feePolicy.getFeeRate(), result.feeAmount(), result.payoutAmount(),
                result.saleCount(), result.cancelCount(),
                now
        );

        return settlementRepository.save(settlement);
    }

    private MonthlySettlementResponse toMonthlyResponse(Settlement s, String creatorName) {
        return new MonthlySettlementResponse(
                s.getCreatorId(), creatorName, s.getYear(), s.getMonth(),
                s.getTotalSales(), s.getTotalRefunds(), s.getNetSales(),
                s.getFeeRate(), s.getFeeAmount(), s.getPayoutAmount(),
                s.getSaleCount(), s.getCancelCount(),
                s.getStatus().name()
        );
    }
}
