package com.datn.lms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionDto {
    private Long id;
    private String questionText;
    private String questionType;
    private Integer orderIndex;
    private BigDecimal points;
    private List<QuizOptionDto> options;
}
