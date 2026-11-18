package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudentUserIdAndLessonIdOrderBySubmittedAtDesc(Long studentId, Long lessonId);
    Optional<QuizAttempt> findFirstByStudentUserIdAndLessonIdOrderByScorePercentageDesc(Long studentId, Long lessonId);
    long countByStudentUserIdAndQuizId(Long studentId, Long quizId);
}
