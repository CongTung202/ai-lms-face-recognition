package com.datn.lms.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmissionRequest {
    private Long studentId;
    private Integer timeSpentSeconds;
    private List<AnswerItem> answers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerItem {
        private Long questionId;
        private Long selectedOptionId;
    }
}
