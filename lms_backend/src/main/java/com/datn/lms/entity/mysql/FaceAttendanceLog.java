package com.datn.lms.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_attendance_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceAttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @CreationTimestamp
    @Column(name = "capture_time", updatable = false)
    private LocalDateTime captureTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "similarity_score", nullable = false)
    @Builder.Default
    private Float similarityScore = 0.0f;

    @Column(name = "liveness_score", nullable = false)
    @Builder.Default
    private Float livenessScore = 0.0f;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "note")
    private String note;

    public enum VerificationStatus {
        SUCCESS, FAILED, NO_FACE, MULTIPLE_FACES, SPOOF_DETECTED
    }
}
