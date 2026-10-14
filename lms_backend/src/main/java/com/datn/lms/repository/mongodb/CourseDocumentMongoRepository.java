package com.datn.lms.repository.mongodb;

import com.datn.lms.entity.mongodb.CourseDocumentMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDocumentMongoRepository extends MongoRepository<CourseDocumentMongo, String> {
    Optional<CourseDocumentMongo> findByMysqlDocId(Long mysqlDocId);
    List<CourseDocumentMongo> findByCourseId(Long courseId);
    List<CourseDocumentMongo> findByRagStatus(CourseDocumentMongo.RagStatus status);
}
