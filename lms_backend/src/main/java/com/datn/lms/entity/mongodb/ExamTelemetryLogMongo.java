package com.datn.lms.entity.mongodb;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Log giám sát thời gian thực (Telemetry stream) ghi nhận chuỗi hoạt động của thí sinh
 * trong suốt thời gian làm bài thi trắc nghiệm (FPS, tọa độ khuôn mặt, góc đầu 3D, nhịp tim mạng).
 */
@Document(collection = "exam_telemetry_logs")
@CompoundIndex(name = "idx_attempt_time", def = "{'attempt_id': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTelemetryLogMongo {

    @Id
    private String id;

    @Indexed
    @Field("attempt_id")
    private Long attemptId; // Lượt thi MySQL

    @Indexed
    @Field("student_id")
    private Long studentId; // UID số của sinh viên

    @Field("exam_id")
    private Long examId;

    @Field("event_type")
    private String eventType; // HEARTBEAT, HEAD_POSE_TICK, WEBCAM_FRAME_CHECK, KEY_EVENT

    @Field("face_detected")
    private Boolean faceDetected;

    @Field("pitch")
    private Double pitch; // Góc ngẩng/cúi đầu từ module 3D head pose

    @Field("yaw")
    private Double yaw; // Góc quay trái/phải

    @Field("roll")
    private Double roll; // Góc nghiêng đầu

    @Field("metadata")
    private Map<String, Object> metadata;

    @CreatedDate
    @Field("timestamp")
    private LocalDateTime timestamp;
}
