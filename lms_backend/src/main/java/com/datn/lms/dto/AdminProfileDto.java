package com.datn.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDto {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String status;
    private String cccd;
    private String gender;
    private String dateOfBirth;
    private String address;
    private Boolean faceRegistered;
    private String faceImageUrl;
    private String memberType;
}
