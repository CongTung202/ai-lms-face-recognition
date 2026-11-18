package com.datn.lms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDto {
    private Long id;
    private String title;
    private String category;
    private String categoryName;
    private String badgeClass;
    private String summary;
    private String content;
    private String imageUrl;
    private String cloudinaryPublicId;
    private String eventDate;
    private String location;
    private String author;
    private Boolean isFeatured;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

