package com.datn.lms.repository.mongodb;

import com.datn.lms.entity.mongodb.DocumentChunkMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkMongoRepository extends MongoRepository<DocumentChunkMongo, String> {
    List<DocumentChunkMongo> findByDocumentIdOrderByChunkIndexAsc(String documentId);
    List<DocumentChunkMongo> findByCourseId(Long courseId);
    void deleteByDocumentId(String documentId);
}
