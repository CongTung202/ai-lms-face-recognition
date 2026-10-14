package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.dto.FaceAiVerifyResultDto;
import com.datn.lms.dto.ProgressHeartbeatRequest;
import com.datn.lms.dto.ProgressResponse;
import com.datn.lms.entity.mysql.VideoAsset;
import com.datn.lms.security.JwtTokenProvider;
import com.datn.lms.service.LearningProgressService;
import com.datn.lms.service.VideoStreamingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoStreamingService videoStreamingService;
    private final LearningProgressService learningProgressService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 1. Tải lên tệp Video cho một bài học (MP4, MKV, WebM)
     * POST /api/videos/upload/{lessonId}
     */
    @PostMapping("/upload/{lessonId}")
    public ResponseEntity<ApiResponse<com.datn.lms.dto.VideoAssetResponseDto>> uploadLessonVideo(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file) {
        try {
            VideoAsset asset = videoStreamingService.uploadLessonVideo(lessonId, file);
            com.datn.lms.dto.VideoAssetResponseDto dto = com.datn.lms.dto.VideoAssetResponseDto.builder()
                    .id(asset.getId())
                    .lessonId(lessonId)
                    .originalFileName(asset.getOriginalFileName())
                    .streamUrl("/api/videos/stream/" + lessonId)
                    .fileSizeBytes(asset.getFileSizeBytes())
                    .resolution(asset.getResolution())
                    .encodeStatus(asset.getEncodeStatus() != null ? asset.getEncodeStatus().name() : "READY")
                    .build();
            return ResponseEntity.ok(ApiResponse.success(dto, "Tải lên video bài giảng thành công!"));
        } catch (Exception e) {
            log.error("Lỗi tải lên video bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi tải lên video: " + e.getMessage()));
        }
    }

    /**
     * 2. Phát video chuẩn HTTP 206 Byte-Range Request (Hỗ trợ tua cực nhanh, không giật lag)
     * GET /api/videos/stream/{lessonId}
     */
    @GetMapping("/stream/{lessonId}")
    public ResponseEntity<byte[]> streamVideo(
            @PathVariable Long lessonId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        return videoStreamingService.streamVideoChunk(lessonId, rangeHeader);
    }

    /**
     * 3. Ghi nhận nhịp tim học tập (Heartbeat) định kỳ (mỗi 5-10s) & chống tua
     * POST /api/videos/progress/heartbeat
     */
    @PostMapping("/progress/heartbeat")
    public ResponseEntity<ApiResponse<ProgressResponse>> recordHeartbeat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "userId", required = false) Long fallbackUserId,
            @Valid @RequestBody ProgressHeartbeatRequest request) {
        try {
            Long userId = resolveUserId(authHeader, paramToken, fallbackUserId);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "Yêu cầu đăng nhập để ghi nhận tiến trình học"));
            }

            ProgressResponse response = learningProgressService.recordHeartbeat(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật tiến trình học tập thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật tiến trình học: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 4. Lấy vị trí dừng học gần nhất (Resume player) và trạng thái hoàn thành
     * GET /api/videos/progress/{lessonId}
     */
    @GetMapping("/progress/{lessonId}")
    public ResponseEntity<ApiResponse<ProgressResponse>> getProgress(
            @PathVariable Long lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "userId", required = false) Long fallbackUserId) {
        try {
            Long userId = resolveUserId(authHeader, paramToken, fallbackUserId);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "Yêu cầu đăng nhập để xem tiến trình"));
            }

            ProgressResponse response = learningProgressService.getProgress(userId, lessonId);
            return ResponseEntity.ok(ApiResponse.success(response, "Lấy tiến trình bài học thành công"));
        } catch (Exception e) {
            log.error("Lỗi lấy tiến trình bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 5. Điểm danh khuôn mặt bất ngờ trong khi đang xem video
     * POST /api/videos/attendance/check-in
     */
    @PostMapping("/attendance/check-in")
    public ResponseEntity<ApiResponse<FaceAiVerifyResultDto>> checkInFaceDuringVideo(
            @RequestParam("lessonId") Long lessonId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "userId", required = false) Long fallbackUserId) {
        try {
            Long userId = resolveUserId(authHeader, paramToken, fallbackUserId);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "Yêu cầu đăng nhập để điểm danh"));
            }

            FaceAiVerifyResultDto result = learningProgressService.checkInFaceDuringVideo(userId, lessonId, image);
            return ResponseEntity.ok(ApiResponse.success(result, result.isMatched()
                    ? "Điểm danh khuôn mặt trong giờ học thành công!"
                    : "Điểm danh không khớp: " + (result.getReasons() != null ? String.join(", ", result.getReasons()) : "Khuôn mặt không khớp")));
        } catch (Exception e) {
            log.error("Lỗi điểm danh video: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    private Long resolveUserId(String authHeader, String paramToken, Long fallbackUserId) {
        String token = null;
        if (paramToken != null && !paramToken.isBlank()) {
            token = paramToken.trim();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        if (token != null && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUserIdFromToken(token);
        }

        // Dùng fallbackUserId khi debug hoặc test nội bộ
        return fallbackUserId;
    }
}
