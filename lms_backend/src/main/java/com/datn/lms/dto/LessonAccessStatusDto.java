package com.datn.lms.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonAccessStatusDto {
    private Long lessonId;
    private Boolean isLocked;
    private String lockReason;
    private LocalDate scheduledDate;
    private LocalDateTime unlockAt;
    private Boolean isCompleted;
    private Boolean quizPassed;
    private Long prerequisiteLessonId;
    private String prerequisiteLessonTitle;
    private Boolean hasQuiz;
}
