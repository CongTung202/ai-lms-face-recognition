package com.datn.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDto {
    private Long id;
    private Long chapterId;
    private String title;
    private String lessonType;
    private Integer orderIndex;
    private Integer durationSeconds;
    private Boolean isPreview;
}
