package com.datn.lms.controller;

import com.datn.lms.dto.AnnouncementDto;
import com.datn.lms.dto.ApiResponse;
import com.datn.lms.service.AnnouncementService;
import com.datn.lms.service.ImageCloudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ImageCloudService imageCloudService;

    // =========================================================================
    // 1. PUBLIC API FOR STUDENT PORTAL & LANDING PAGE
    // =========================================================================

    @GetMapping("/api/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getPublicAnnouncements(
            @RequestParam(value = "category", required = false) String category) {
        List<AnnouncementDto> list = announcementService.getAllAnnouncements(category, "PUBLISHED");
        return ResponseEntity.ok(ApiResponse.success(list, "Tải danh sách thông báo hoạt động thành công"));
    }

    @GetMapping("/api/announcements/{id}")
    public ResponseEntity<ApiResponse<AnnouncementDto>> getPublicAnnouncementDetail(@PathVariable("id") Long id) {
        return announcementService.getById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto, "Tải chi tiết thông báo thành công")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Không tìm thấy thông báo ID: " + id)));
    }

    // =========================================================================
    // 2. ADMIN API FOR MANAGEMENT PORTAL
    // =========================================================================

    @GetMapping({"/api/admin/announcements", "/admin/api/announcements"})
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getAdminAnnouncements(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status) {
        List<AnnouncementDto> list = announcementService.getAllAnnouncements(category, status);
        return ResponseEntity.ok(ApiResponse.success(list, "Tải danh sách thông báo quản trị thành công"));
    }

    @GetMapping({"/api/admin/announcements/stats", "/admin/api/announcements/stats"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        Map<String, Object> stats = announcementService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy số liệu thống kê thông báo thành công"));
    }

    @GetMapping({"/api/admin/announcements/{id}", "/admin/api/announcements/{id}"})
    public ResponseEntity<ApiResponse<AnnouncementDto>> getAdminAnnouncementDetail(@PathVariable("id") Long id) {
        return announcementService.getById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto, "Lấy chi tiết thông báo thành công")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Không tìm thấy thông báo ID: " + id)));
    }

    @PostMapping(value = {"/api/admin/announcements", "/admin/api/announcements"}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @RequestPart("data") AnnouncementDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            AnnouncementDto created = announcementService.createAnnouncement(dto, file);
            return ResponseEntity.ok(ApiResponse.success(created, "Tạo thông báo thành công! Ảnh đã được nén và lưu lên Cloudinary."));
        } catch (Exception e) {
            log.error("Lỗi tạo thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi tạo thông báo: " + e.getMessage()));
        }
    }

    @PostMapping(value = {"/api/admin/announcements/json", "/admin/api/announcements/json"}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncementJson(@RequestBody AnnouncementDto dto) {
        try {
            AnnouncementDto created = announcementService.createAnnouncement(dto, null);
            return ResponseEntity.ok(ApiResponse.success(created, "Tạo thông báo thành công!"));
        } catch (Exception e) {
            log.error("Lỗi tạo thông báo JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi tạo thông báo: " + e.getMessage()));
        }
    }

    @PutMapping(value = {"/api/admin/announcements/{id}", "/admin/api/announcements/{id}"}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<AnnouncementDto>> updateAnnouncement(
            @PathVariable("id") Long id,
            @RequestPart("data") AnnouncementDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            AnnouncementDto updated = announcementService.updateAnnouncement(id, dto, file);
            return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thông báo thành công!"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi cập nhật thông báo: " + e.getMessage()));
        }
    }

    @PutMapping(value = {"/api/admin/announcements/{id}/json", "/admin/api/announcements/{id}/json"}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<AnnouncementDto>> updateAnnouncementJson(
            @PathVariable("id") Long id,
            @RequestBody AnnouncementDto dto) {
        try {
            AnnouncementDto updated = announcementService.updateAnnouncement(id, dto, null);
            return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thông báo thành công!"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật thông báo JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi cập nhật thông báo: " + e.getMessage()));
        }
    }

    @DeleteMapping({"/api/admin/announcements/{id}", "/admin/api/announcements/{id}"})
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable("id") Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa thông báo ID: " + id));
        } catch (Exception e) {
            log.error("Lỗi xóa thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi xóa thông báo: " + e.getMessage()));
        }
    }

    @PostMapping(value = {"/api/admin/announcements/upload-image", "/admin/api/announcements/upload-image"}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = imageCloudService.uploadMultipartImage(file, "lms_datn/announcements");
            return ResponseEntity.ok(ApiResponse.success(result, "Upload và nén ảnh lên Cloudinary thành công!"));
        } catch (Exception e) {
            log.error("Lỗi upload ảnh lên Cloudinary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}
