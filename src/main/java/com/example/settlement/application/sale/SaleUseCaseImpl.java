package com.example.settlement.application.sale;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;
import com.example.settlement.domain.creator.Course;
import com.example.settlement.domain.creator.CourseRepository;
import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.CancelRecordRepository;
import com.example.settlement.domain.sale.SaleRecord;
import com.example.settlement.domain.sale.SaleRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleUseCaseImpl implements SaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(SaleUseCaseImpl.class);

    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;

    public SaleUseCaseImpl(CourseRepository courseRepository,
                           SaleRecordRepository saleRecordRepository,
                           CancelRecordRepository cancelRecordRepository) {
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancelRecordRepository = cancelRecordRepository;
    }

    @Override
    public SaleRecordResponse register(RegisterSaleCommand cmd) {
        if (cmd.paidAt().isAfter(Instant.now().plusSeconds(60))) {
            throw new BusinessException(ErrorCode.INVALID_PAID_AT);
        }

        Course course = courseRepository.findById(cmd.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        SaleRecord saleRecord = new SaleRecord(
                UUID.randomUUID().toString(),
                cmd.courseId(),
                cmd.studentId(),
                cmd.amount(),
                cmd.paidAt(),
                Instant.now()
        );
        saleRecordRepository.save(saleRecord);

        return new SaleRecordResponse(
                saleRecord.getId(),
                saleRecord.getCourseId(),
                course.getCreatorId(),
                saleRecord.getStudentId(),
                saleRecord.getAmount(),
                saleRecord.getPaidAt(),
                false,
                null,
                null
        );
    }

    @Override
    public SaleRecordResponse cancel(CancelSaleCommand cmd) {
        if (cmd.refundAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        SaleRecord saleRecord = saleRecordRepository.findById(cmd.saleRecordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SALE_NOT_FOUND));

        if (cmd.refundAmount() > saleRecord.getAmount()) {
            throw new BusinessException(ErrorCode.REFUND_EXCEEDS_PAYMENT);
        }

        Course course = courseRepository.findById(saleRecord.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        CancelRecord cancelRecord = new CancelRecord(
                UUID.randomUUID().toString(),
                cmd.saleRecordId(),
                cmd.refundAmount(),
                cmd.cancelledAt(),
                Instant.now()
        );

        try {
            cancelRecordRepository.saveAndFlush(cancelRecord);
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 취소 시도 감지 - saleRecordId: {}", cmd.saleRecordId());
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }

        return new SaleRecordResponse(
                saleRecord.getId(),
                saleRecord.getCourseId(),
                course.getCreatorId(),
                saleRecord.getStudentId(),
                saleRecord.getAmount(),
                saleRecord.getPaidAt(),
                true,
                cancelRecord.getRefundAmount(),
                cancelRecord.getCancelledAt()
        );
    }

    @Override
    public Page<SaleRecordResponse> list(SaleListQuery query, Pageable pageable) {
        List<Course> courses = courseRepository.findByCreatorId(query.creatorId());
        if (courses.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, String> courseIdToCreatorId = courses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getCreatorId));

        List<String> courseIds = List.copyOf(courseIdToCreatorId.keySet());
        Page<SaleRecord> salesPage = saleRecordRepository
                .findByCourseIdInAndPaidAtGreaterThanEqualAndPaidAtLessThan(courseIds, query.from(), query.to(), pageable);

        if (!salesPage.hasContent()) {
            return Page.empty(pageable);
        }

        List<String> saleIds = salesPage.getContent().stream().map(SaleRecord::getId).toList();
        Map<String, CancelRecord> cancelBySaleId = cancelRecordRepository
                .findBySaleRecordIdIn(saleIds)
                .stream()
                .collect(Collectors.toMap(CancelRecord::getSaleRecordId, Function.identity()));

        return salesPage.map(sale -> {
            CancelRecord cancel = cancelBySaleId.get(sale.getId());
            return new SaleRecordResponse(
                    sale.getId(),
                    sale.getCourseId(),
                    courseIdToCreatorId.get(sale.getCourseId()),
                    sale.getStudentId(),
                    sale.getAmount(),
                    sale.getPaidAt(),
                    cancel != null,
                    cancel != null ? cancel.getRefundAmount() : null,
                    cancel != null ? cancel.getCancelledAt() : null
            );
        });
    }
}
