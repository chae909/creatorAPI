package com.example.settlement.domain.sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sale_records")
public class SaleRecord {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SaleRecord() {}

    public SaleRecord(String id, String courseId, String studentId, Long amount, Instant paidAt, Instant createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.studentId = studentId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getStudentId() { return studentId; }
    public Long getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
}
