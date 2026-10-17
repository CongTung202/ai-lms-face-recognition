package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.CourseChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseChapterRepository extends JpaRepository<CourseChapter, Long> {
    List<CourseChapter> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    void deleteByCourseId(Long courseId);
}
