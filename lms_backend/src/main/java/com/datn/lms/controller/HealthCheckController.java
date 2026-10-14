package com.datn.lms.controller;

import com.datn.lms.repository.mongodb.CourseDocumentMongoRepository;
import com.datn.lms.repository.mysql.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final UserRepository userRepository;
    private final CourseDocumentMongoRepository mongoDocumentRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "LMS Spring Boot Backend");

        // Kiểm tra MySQL
        try {
            long userCount = userRepository.count();
            response.put("mysql_connection", "OK");
            response.put("mysql_total_users", userCount);
        } catch (Exception e) {
            response.put("mysql_connection", "FAILED: " + e.getMessage());
        }

        // Kiểm tra MongoDB
        try {
            long mongoDocCount = mongoDocumentRepository.count();
            response.put("mongodb_connection", "OK");
            response.put("mongodb_total_docs", mongoDocCount);
        } catch (Exception e) {
            response.put("mongodb_connection", "FAILED: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
