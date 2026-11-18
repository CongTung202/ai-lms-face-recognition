package com.datn.lms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagAskRequest {

    private Long courseId;

    @JsonAlias({"query", "content", "prompt", "text"})
    private String question;

    private Integer topK;

    public String getEffectiveQuestion() {
        return question != null ? question.trim() : "";
    }
}
