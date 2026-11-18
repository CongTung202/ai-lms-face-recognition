package com.datn.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocumentDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long lessonId;
    private String lessonTitle;
    private String title;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSizeBytes;
    private String mongoDocId;
    private Boolean ragIndexed;
    private Integer totalPages;
    private Integer chunkCount;
    private String ragStatus;
    private LocalDateTime createdAt;
}
