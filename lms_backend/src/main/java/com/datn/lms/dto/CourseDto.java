package com.datn.lms.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private BigDecimal price;
    private Boolean isFree;
    private String status;
    private String enrollmentType;
    private Long subjectId;
    private Long lecturerId;
    private java.util.List<Long> majorIds;
}
