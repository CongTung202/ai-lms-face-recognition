package com.datn.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String cccd;
    private String gender;
    private String dateOfBirth;
    private String address;
    private String avatarUrl;
}
