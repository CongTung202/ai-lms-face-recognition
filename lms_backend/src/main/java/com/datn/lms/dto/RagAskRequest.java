package com.datn.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagAskRequest {

    @NotNull(message = "ID khóa học không được để trống")
    private Long courseId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String question;
}
