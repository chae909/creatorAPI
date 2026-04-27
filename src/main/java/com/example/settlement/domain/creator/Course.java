package com.example.settlement.domain.creator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private String id;

    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Course() {}

    public Course(String id, String creatorId, String title, Instant createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCreatorId() { return creatorId; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
}
