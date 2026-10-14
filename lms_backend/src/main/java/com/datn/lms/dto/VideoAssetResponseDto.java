package com.datn.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAssetResponseDto {
    private Long id;
    private Long lessonId;
    private String originalFileName;
    private String streamUrl;
    private Long fileSizeBytes;
    private String resolution;
    private String encodeStatus;
}
