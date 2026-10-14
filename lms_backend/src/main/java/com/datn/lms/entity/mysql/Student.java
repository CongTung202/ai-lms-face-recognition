package com.datn.lms.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @Column(name = "user_id")
    private Long userId; // UID số (MapsId với User.id)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "student_code", nullable = false, unique = true, length = 50)
    private String studentCode; // MSSV (vd: SV2026001)

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    @Builder.Default
    private Gender gender = Gender.MALE;

    @Column(name = "face_registered", nullable = false)
    @Builder.Default
    private Boolean faceRegistered = false;

    @Column(name = "face_registered_at")
    private LocalDateTime faceRegisteredAt;

    @Column(name = "face_image_url", length = 500)
    private String faceImageUrl;

    @Column(name = "face_data_id", length = 100)
    private String faceDataId; // ID vector tham chiếu bên FastAPI/MongoDB

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
