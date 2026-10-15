package com.datn.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String role;     // ADMIN, LECTURER, STUDENT
    private String status;   // ACTIVE, INACTIVE, LOCKED
    private String studentCode;
    private Long classId;
    private String gender;   // MALE, FEMALE, OTHER
    private String dateOfBirth;
    private Boolean faceRegistered;
    private String faceImageUrl;
}
