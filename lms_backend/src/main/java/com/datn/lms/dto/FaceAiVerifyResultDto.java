package com.datn.lms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceAiVerifyResultDto {
    private boolean matched;

    @JsonProperty("employee_id")
    private String employeeId;

    @JsonProperty("full_name")
    private String fullName;

    private float similarity;
    private float threshold;

    @JsonProperty("matched_angle")
    private String matchedAngle;

    @JsonProperty("liveness_passed")
    private boolean livenessPassed;

    @JsonProperty("liveness_score")
    private float livenessScore;

    private List<String> reasons;
}
