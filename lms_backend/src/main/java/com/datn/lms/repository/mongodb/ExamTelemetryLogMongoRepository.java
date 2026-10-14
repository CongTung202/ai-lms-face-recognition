package com.datn.lms.repository.mongodb;

import com.datn.lms.entity.mongodb.ExamTelemetryLogMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamTelemetryLogMongoRepository extends MongoRepository<ExamTelemetryLogMongo, String> {
    List<ExamTelemetryLogMongo> findByAttemptIdOrderByTimestampDesc(Long attemptId);
}
