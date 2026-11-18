package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    Optional<CourseEnrollment> findByStudentUserIdAndCourseId(Long studentUserId, Long courseId);

    boolean existsByStudentUserIdAndCourseIdAndStatus(Long studentUserId, Long courseId, CourseEnrollment.EnrollmentStatus status);

    List<CourseEnrollment> findByStudentUserIdAndStatus(Long studentUserId, CourseEnrollment.EnrollmentStatus status);

    List<CourseEnrollment> findByStudentUserId(Long studentUserId);

    List<CourseEnrollment> findByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, CourseEnrollment.EnrollmentStatus status);
}
