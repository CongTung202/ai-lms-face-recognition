package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.CourseMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMajorRepository extends JpaRepository<CourseMajor, Long> {
    List<CourseMajor> findByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
    List<CourseMajor> findByMajorId(Long majorId);
}
