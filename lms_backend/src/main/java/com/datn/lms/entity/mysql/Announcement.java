package com.datn.lms.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // e.g. startup, talent, charity, sports, honor, grad

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName; // Tên hiển thị tiếng Việt

    @Column(name = "badge_class", length = 50)
    private String badgeClass; // badge-startup, badge-talent, ...

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl; // URL CDN Cloudinary (hoặc fallback relative path)

    @Column(name = "cloudinary_public_id", length = 200)
    private String cloudinaryPublicId;

    @Column(name = "event_date", length = 100)
    private String eventDate; // Ví dụ: 'Tháng 10/2026', 'Tri Ân 30/4'

    @Column(name = "location", length = 255)
    private String location; // Địa điểm tổ chức

    @Column(name = "author", length = 100)
    @Builder.Default
    private String author = "Ban Truyền Thông NTU";

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PUBLISHED"; // PUBLISHED, DRAFT, ARCHIVED

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

