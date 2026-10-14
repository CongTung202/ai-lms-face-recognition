package com.datn.lms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceAiEnrollResultDto {
    private boolean success;

    @JsonProperty("employee_id")
    private String employeeId;

    private String message;

    @JsonProperty("face_detected")
    private boolean faceDetected;

    @JsonProperty("quality_passed")
    private boolean qualityPassed;

    @JsonProperty("image_saved_path")
    private String imageSavedPath;

    @JsonProperty("cloudinary_url")
    private String cloudinaryUrl;

    @JsonProperty("registered_angles")
    private List<String> registeredAngles;

    private List<String> reasons;
}
