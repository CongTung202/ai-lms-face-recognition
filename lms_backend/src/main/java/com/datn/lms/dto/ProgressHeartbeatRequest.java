package com.datn.lms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressHeartbeatRequest {

    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    @NotNull(message = "Thời gian xem hiện tại không được để trống")
    private Integer currentTimeSecond;

    @NotNull(message = "Tổng thời lượng bài học không được để trống")
    private Integer durationSecond;
}
