package com.example.settlement.application.sale;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;
import com.example.settlement.domain.creator.Course;
import com.example.settlement.domain.creator.CourseRepository;
import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.CancelRecordRepository;
import com.example.settlement.domain.sale.SaleRecord;
import com.example.settlement.domain.sale.SaleRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleUseCaseImpl implements SaleUseCase {

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
        SaleRecord saleRecord = saleRecordRepository.findById(cmd.saleRecordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SALE_NOT_FOUND));

        if (cancelRecordRepository.findBySaleRecordId(cmd.saleRecordId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }

        if (cmd.refundAmount() > saleRecord.getAmount()) {
            throw new BusinessException(ErrorCode.REFUND_EXCEEDS_PAYMENT);
        }

        CancelRecord cancelRecord = new CancelRecord(
                UUID.randomUUID().toString(),
                cmd.saleRecordId(),
                cmd.refundAmount(),
                cmd.cancelledAt(),
                Instant.now()
        );
        cancelRecordRepository.save(cancelRecord);

        Course course = courseRepository.findById(saleRecord.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

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
    public List<SaleRecordResponse> list(SaleListQuery query) {
        List<Course> courses = courseRepository.findByCreatorId(query.creatorId());
        if (courses.isEmpty()) {
            return List.of();
        }

        Map<String, String> courseIdToCreatorId = courses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getCreatorId));

        List<String> courseIds = List.copyOf(courseIdToCreatorId.keySet());
        List<SaleRecord> sales = saleRecordRepository
                .findByCourseIdInAndPaidAtGreaterThanEqualAndPaidAtLessThan(courseIds, query.from(), query.to());

        if (sales.isEmpty()) {
            return List.of();
        }

        List<String> saleIds = sales.stream().map(SaleRecord::getId).toList();
        Map<String, CancelRecord> cancelBySaleId = cancelRecordRepository
                .findBySaleRecordIdIn(saleIds)
                .stream()
                .collect(Collectors.toMap(CancelRecord::getSaleRecordId, Function.identity()));

        return sales.stream()
                .map(sale -> {
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
                })
                .toList();
    }
}
