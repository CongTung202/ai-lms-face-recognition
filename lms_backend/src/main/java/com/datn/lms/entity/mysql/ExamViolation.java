package com.datn.lms.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 40)
    private ViolationType violationType;

    @CreationTimestamp
    @Column(name = "violation_time", updatable = false)
    private LocalDateTime violationTime;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "evidence_snapshot_url", length = 500)
    private String evidenceSnapshotUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 20)
    @Builder.Default
    private SeverityLevel severityLevel = SeverityLevel.WARNING;

    public enum ViolationType {
        TAB_SWITCH,            // Chuyển tab trình duyệt
        WINDOW_BLUR,           // Click ra ngoài ứng dụng
        INSPECT_KEY_PRESS,     // Bấm F12, Ctrl+Shift+I, Chuột phải
        FULLSCREEN_EXIT,       // Thoát chế độ toàn màn hình
        NO_FACE_DETECTED,      // Không thấy mặt trong camera
        MULTIPLE_FACES,        // Xuất hiện nhiều người
        FACE_MISMATCH,         // Nhận diện không khớp (thi hộ)
        HEAD_POSE_LOOKING_AWAY // Nhìn lệch góc nghi vấn chép bài
    }

    public enum SeverityLevel {
        WARNING, CRITICAL
    }
}
