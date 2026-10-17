package com.datn.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterDto {
    private Long id;
    private Long courseId;
    private String title;
    private Integer orderIndex;
}
