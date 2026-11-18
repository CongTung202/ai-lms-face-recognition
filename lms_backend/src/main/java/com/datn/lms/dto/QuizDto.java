package com.datn.lms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizDto {
    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private BigDecimal passingScorePercentage;
    private Integer maxAttempts;
    private Integer questionsCount;
    private List<QuizQuestionDto> questions;
    private Boolean alreadyPassed;
    private BigDecimal bestScore;
    private Integer attemptsMade;
}
