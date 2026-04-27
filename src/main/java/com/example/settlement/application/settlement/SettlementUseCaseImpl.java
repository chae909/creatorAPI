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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final Logger log = LoggerFactory.getLogger(SettlementUseCaseImpl.class);

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementSaver settlementSaver;

    public SettlementUseCaseImpl(CreatorRepository creatorRepository,
                                 CourseRepository courseRepository,
                                 SaleRecordRepository saleRecordRepository,
                                 CancelRecordRepository cancelRecordRepository,
                                 FeePolicyRepository feePolicyRepository,
                                 SettlementRepository settlementRepository,
                                 SettlementSaver settlementSaver) {
        this.creatorRepository = creatorRepository;
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancelRecordRepository = cancelRecordRepository;
        this.feePolicyRepository = feePolicyRepository;
        this.settlementRepository = settlementRepository;
        this.settlementSaver = settlementSaver;
    }

    @Override
    public MonthlySettlementResponse getMonthly(MonthlySettlementQuery query) {
        log.debug("정산 조회 요청 - creatorId: {}, yearMonth: {}", query.creatorId(), query.yearMonth());
        Creator creator = creatorRepository.findById(query.creatorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));
        Settlement settlement = findOrCreateSettlement(creator.getId(), query.yearMonth());
        log.info("정산 계산 완료 - creatorId: {}, {}-{}, payoutAmount: {}",
                query.creatorId(), settlement.getYear(), settlement.getMonth(), settlement.getPayoutAmount());
        return toMonthlyResponse(settlement, creator.getName());
    }

    @Override
    public MonthlySettlementResponse confirm(String creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));
        Settlement settlement = findOrCreateSettlement(creatorId, yearMonth);
        settlement.confirm(Instant.now());
        settlementRepository.save(settlement);
        log.info("정산 확정 처리 - creatorId: {}, {}-{}, settlementId: {}",
                creatorId, settlement.getYear(), settlement.getMonth(), settlement.getId());
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
        log.info("정산 지급 처리 - creatorId: {}, {}-{}, payoutAmount: {}",
                creatorId, settlement.getYear(), settlement.getMonth(), settlement.getPayoutAmount());
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
                .orElseGet(() -> {
                    Settlement newSettlement = calculateNew(creatorId, year, month);
                    try {
                        // REQUIRES_NEW: inner tx commits immediately; if it fails, only that tx is
                        // rolled back, leaving the outer tx session valid for the re-read below.
                        return settlementSaver.save(newSettlement);
                    } catch (DataIntegrityViolationException e) {
                        return settlementRepository
                                .findByCreatorIdAndYearAndMonth(creatorId, year, month)
                                .orElseThrow();
                    }
                });
    }

    private Settlement calculateNew(String creatorId, int year, int month) {
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
            sales = saleRecordRepository.findByCourseIdInAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                    courseIds, range.start(), range.end());
            cancels = cancelRecordRepository.findByCourseIdsAndCancelledAtRange(
                    courseIds, range.start(), range.end());
        }

        FeePolicy feePolicy = feePolicyRepository
                .findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.of(year, month, 1))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_DATE_RANGE));

        SettlementCalculator.SettlementResult result =
                SettlementCalculator.calculate(sales, cancels, feePolicy.getFeeRate());

        return new Settlement(
                creatorId, year, month,
                result.totalSales(), result.totalRefunds(), result.netSales(),
                feePolicy.getFeeRate(), result.feeAmount(), result.payoutAmount(),
                result.saleCount(), result.cancelCount(),
                Instant.now()
        );
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
