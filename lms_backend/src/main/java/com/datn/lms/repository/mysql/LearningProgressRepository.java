package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {
    Optional<LearningProgress> findByStudentUserIdAndLessonId(Long studentUserId, Long lessonId);
    List<LearningProgress> findByStudentUserIdAndCourseId(Long studentUserId, Long courseId);
}
