package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    List<QuizOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);
}
