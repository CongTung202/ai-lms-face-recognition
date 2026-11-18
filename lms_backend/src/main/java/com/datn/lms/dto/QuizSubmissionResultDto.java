package com.datn.lms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmissionResultDto {
    private Long attemptId;
    private Long quizId;
    private Long lessonId;
    private BigDecimal scorePercentage;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Boolean isPassed;
    private BigDecimal passingScorePercentage;
    private Boolean canUnlockNext;
    private Long nextLessonId;
    private String nextLessonTitle;
    private String message;
    private List<QuestionFeedbackDto> feedback;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionFeedbackDto {
        private Long questionId;
        private String questionText;
        private Long selectedOptionId;
        private Long correctOptionId;
        private String correctOptionText;
        private Boolean isCorrect;
        private String explanation;
    }
}
