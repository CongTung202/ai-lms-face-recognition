package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByLessonId(Long lessonId);
    boolean existsByLessonId(Long lessonId);
}
