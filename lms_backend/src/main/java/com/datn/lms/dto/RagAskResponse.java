package com.datn.lms.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagAskResponse {

    private String answer;
    private Long courseId;
    private String question;
    private List<RagSourceChunkDto> sources;
    private Long executionTimeMs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagSourceChunkDto {
        private String chunkId;
        private Integer pageNumber;
        private Double similarityScore;
        private String snippet;
    }
}
