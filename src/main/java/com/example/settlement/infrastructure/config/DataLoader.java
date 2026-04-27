package com.example.settlement.infrastructure.config;

import com.example.settlement.domain.creator.Course;
import com.example.settlement.domain.creator.CourseRepository;
import com.example.settlement.domain.creator.Creator;
import com.example.settlement.domain.creator.CreatorRepository;
import com.example.settlement.domain.sale.CancelRecord;
import com.example.settlement.domain.sale.CancelRecordRepository;
import com.example.settlement.domain.sale.SaleRecord;
import com.example.settlement.domain.sale.SaleRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;

    public DataLoader(CreatorRepository creatorRepository,
                      CourseRepository courseRepository,
                      SaleRecordRepository saleRecordRepository,
                      CancelRecordRepository cancelRecordRepository) {
        this.creatorRepository = creatorRepository;
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancelRecordRepository = cancelRecordRepository;
    }

    @Override
    public void run(String... args) {
        int creators = 0, courses = 0, sales = 0, cancels = 0;

        if (!creatorRepository.existsById("creator-1")) {
            creatorRepository.save(new Creator("creator-1", "김강사", Instant.now()));
            creators++;
        }
        if (!creatorRepository.existsById("creator-2")) {
            creatorRepository.save(new Creator("creator-2", "이강사", Instant.now()));
            creators++;
        }
        if (!creatorRepository.existsById("creator-3")) {
            creatorRepository.save(new Creator("creator-3", "박강사", Instant.now()));
            creators++;
        }

        if (!courseRepository.existsById("course-1")) {
            courseRepository.save(new Course("course-1", "creator-1", "Spring Boot 입문", Instant.now()));
            courses++;
        }
        if (!courseRepository.existsById("course-2")) {
            courseRepository.save(new Course("course-2", "creator-1", "JPA 실전", Instant.now()));
            courses++;
        }
        if (!courseRepository.existsById("course-3")) {
            courseRepository.save(new Course("course-3", "creator-2", "Kotlin 기초", Instant.now()));
            courses++;
        }
        if (!courseRepository.existsById("course-4")) {
            courseRepository.save(new Course("course-4", "creator-3", "MSA 설계", Instant.now()));
            courses++;
        }

        if (!saleRecordRepository.existsById("sale-1")) {
            saleRecordRepository.save(new SaleRecord("sale-1", "course-1", "student-1", 50000L, Instant.parse("2025-03-05T01:00:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-2")) {
            saleRecordRepository.save(new SaleRecord("sale-2", "course-1", "student-2", 50000L, Instant.parse("2025-03-15T05:30:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-3")) {
            saleRecordRepository.save(new SaleRecord("sale-3", "course-2", "student-3", 80000L, Instant.parse("2025-03-20T00:00:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-4")) {
            saleRecordRepository.save(new SaleRecord("sale-4", "course-2", "student-4", 80000L, Instant.parse("2025-03-22T02:00:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-5")) {
            saleRecordRepository.save(new SaleRecord("sale-5", "course-3", "student-5", 60000L, Instant.parse("2025-01-31T14:30:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-6")) {
            saleRecordRepository.save(new SaleRecord("sale-6", "course-3", "student-6", 60000L, Instant.parse("2025-03-10T07:00:00Z"), Instant.now()));
            sales++;
        }
        if (!saleRecordRepository.existsById("sale-7")) {
            saleRecordRepository.save(new SaleRecord("sale-7", "course-4", "student-7", 120000L, Instant.parse("2025-02-14T01:00:00Z"), Instant.now()));
            sales++;
        }

        if (!cancelRecordRepository.existsById("cancel-1")) {
            cancelRecordRepository.save(new CancelRecord("cancel-1", "sale-3", 80000L, Instant.parse("2025-03-25T00:00:00Z"), Instant.now()));
            cancels++;
        }
        if (!cancelRecordRepository.existsById("cancel-2")) {
            cancelRecordRepository.save(new CancelRecord("cancel-2", "sale-4", 30000L, Instant.parse("2025-03-27T00:00:00Z"), Instant.now()));
            cancels++;
        }
        if (!cancelRecordRepository.existsById("cancel-3")) {
            cancelRecordRepository.save(new CancelRecord("cancel-3", "sale-5", 60000L, Instant.parse("2025-02-03T01:00:00Z"), Instant.now()));
            cancels++;
        }

        log.info("DataLoader: inserted {} creators, {} courses, {} sale records, {} cancel records",
                creators, courses, sales, cancels);
    }
}
