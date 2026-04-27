package com.example.settlement.domain.creator;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByCreatorId(String creatorId);
}
