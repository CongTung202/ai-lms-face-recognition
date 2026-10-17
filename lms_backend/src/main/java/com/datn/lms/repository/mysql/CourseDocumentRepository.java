package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.CourseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDocumentRepository extends JpaRepository<CourseDocument, Long> {
    List<CourseDocument> findByCourseId(Long courseId);
    Optional<CourseDocument> findByMongoDocId(String mongoDocId);
    void deleteByCourseId(Long courseId);
}
