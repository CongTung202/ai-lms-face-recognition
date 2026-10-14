package com.datn.lms.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    @Builder.Default
    private StorageProvider storageProvider = StorageProvider.LOCAL;

    @Column(name = "raw_video_url", length = 500)
    private String rawVideoUrl;

    @Column(name = "hls_playlist_url", length = 500)
    private String hlsPlaylistUrl; // file .m3u8 streaming thích ứng băng thông

    @Column(name = "dash_manifest_url", length = 500)
    private String dashManifestUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds", nullable = false)
    @Builder.Default
    private Integer durationSeconds = 0;

    @Column(name = "resolution", length = 50)
    @Builder.Default
    private String resolution = "1080p";

    @Column(name = "file_size_bytes", nullable = false)
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "encode_status", nullable = false, length = 20)
    @Builder.Default
    private EncodeStatus encodeStatus = EncodeStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StorageProvider {
        LOCAL, MINIO, S3, CLOUDINARY
    }

    public enum EncodeStatus {
        UPLOADING, PROCESSING, READY, FAILED
    }
}
