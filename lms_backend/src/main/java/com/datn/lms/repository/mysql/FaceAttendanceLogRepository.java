package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.FaceAttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaceAttendanceLogRepository extends JpaRepository<FaceAttendanceLog, Long> {
    List<FaceAttendanceLog> findByStudentUserIdOrderByCaptureTimeDesc(Long studentUserId);
    void deleteByStudentUserId(Long studentUserId);
}
