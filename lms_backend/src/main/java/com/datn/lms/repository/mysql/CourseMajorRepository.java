package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.CourseMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CourseMajorRepository extends JpaRepository<CourseMajor, Long> {
    List<CourseMajor> findByCourseId(Long courseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CourseMajor cm WHERE cm.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    List<CourseMajor> findByMajorId(Long majorId);
}
