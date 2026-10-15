package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.dto.LoginRequest;
import com.datn.lms.dto.LoginResponse;
import com.datn.lms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * BƯỚC 1: Đăng nhập bằng tên đăng nhập và mật khẩu
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
        } catch (Exception e) {
            log.warn("Đăng nhập thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * BƯỚC 2A: Đăng ký 3 góc khuôn mặt lần đầu tiên (Trực diện, Trái, Phải - Lưu Cloudinary)
     * POST /api/auth/enroll-face
     */
    @PostMapping("/enroll-face")
    public ResponseEntity<ApiResponse<LoginResponse>> enrollFaceFirstTime(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "tempToken", required = false) String paramToken,
            @RequestParam("image_straight") MultipartFile imageStraight,
            @RequestParam("image_left") MultipartFile imageLeft,
            @RequestParam("image_right") MultipartFile imageRight) {

        String token = resolveToken(authHeader, paramToken);
        if (token == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(401, "Thiếu mã xác thực phiên đăng nhập tạm thời"));
        }

        try {
            LoginResponse response = authService.enrollFaceFirstTime(token, imageStraight, imageLeft, imageRight);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi đăng ký khuôn mặt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * BƯỚC 2B: Điểm danh khuôn mặt khi đăng nhập (1 ảnh webcam trực diện)
     * POST /api/auth/verify-face-login
     */
    @PostMapping("/verify-face-login")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyFaceLogin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "tempToken", required = false) String paramToken,
            @RequestParam("image") MultipartFile image) {

        String token = resolveToken(authHeader, paramToken);
        if (token == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(401, "Thiếu mã xác thực phiên đăng nhập tạm thời"));
        }

        try {
            LoginResponse response = authService.verifyFaceAndCheckIn(token, image);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
        } catch (Exception e) {
            log.error("Điểm danh đăng nhập thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    private String resolveToken(String authHeader, String paramToken) {
        if (paramToken != null && !paramToken.isBlank()) {
            return paramToken.trim();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
