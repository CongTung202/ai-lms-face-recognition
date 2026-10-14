package com.datn.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private Long userId; // UID số
    private String username;
    private String fullName;
    private String role;
    private String studentCode;
    private Boolean faceRegistered;
    private String faceImageUrl; // Link ảnh Cloudinary CDN

    /**
     * Trạng thái đăng nhập:
     * - SUCCESS: Đã xác thực đầy đủ, cấp accessToken để truy cập
     * - REQUIRE_FACE_ENROLLMENT: Lần đầu đăng nhập, bắt buộc quét 3 góc mặt (lưu Cloudinary)
     * - REQUIRE_FACE_VERIFICATION: Đã đăng ký khuôn mặt trước đó, bắt buộc chụp 1 ảnh webcam để điểm danh đăng nhập
     */
    private String status;

    private String accessToken; // JWT chính thức
    private String tempToken;   // JWT tạm thời phục vụ bước xác thực/đăng ký khuôn mặt
    private String message;
}
