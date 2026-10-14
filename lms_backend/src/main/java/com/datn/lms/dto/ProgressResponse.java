package com.datn.lms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {
    private Long lessonId;
    private Long courseId;
    private Integer lastWatchedSecond; // Điểm dừng gần nhất (cho phép Resume xem tiếp)
    private Integer maxWatchedSecond;  // Mốc xa nhất đã xem (chống tua)
    private Integer totalTimeSpentSeconds;
    private Integer completionPercentage;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
}
