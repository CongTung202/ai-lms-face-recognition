package com.datn.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOptionDto {
    private Long id;
    private String optionText;
    private Integer orderIndex;
}
