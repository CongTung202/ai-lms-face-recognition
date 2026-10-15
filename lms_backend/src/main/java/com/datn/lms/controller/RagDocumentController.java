package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.dto.RagAskRequest;
import com.datn.lms.dto.RagAskResponse;
import com.datn.lms.entity.mongodb.CourseDocumentMongo;
import com.datn.lms.entity.mysql.CourseDocument;
import com.datn.lms.repository.mysql.CourseDocumentRepository;
import com.datn.lms.service.DocumentRagSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/documents", "/api/documents"})
@RequiredArgsConstructor
@Slf4j
public class RagDocumentController {

    private final DocumentRagSyncService documentRagSyncService;
    private final CourseDocumentRepository courseDocumentRepository;

    /**
     * 1. Giảng viên tải lên tài liệu (.pdf, .docx, .txt) & Tự động bóc tách lập chỉ mục RAG
     * POST /api/documents/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CourseDocumentMongo>> uploadDocument(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("file") MultipartFile file) {
        try {
            CourseDocumentMongo mongoDoc = documentRagSyncService.uploadAndIndexDocument(courseId, title, file);
            return ResponseEntity.ok(ApiResponse.success(mongoDoc,
                    String.format("Tải lên và lập chỉ mục RAG thành công! (%d trang, %d đoạn trích)",
                            mongoDoc.getTotalPages(), mongoDoc.getChunkCount())));
        } catch (Exception e) {
            log.error("Lỗi khi tải lên và lập chỉ mục tài liệu môn học {}: {}", courseId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi xử lý tài liệu: " + e.getMessage()));
        }
    }

    /**
     * 2. Sinh viên hỏi đáp bài học với Trợ Lý AI RAG (Tìm kiếm ngữ nghĩa & Trích dẫn số trang)
     * POST /api/documents/rag/ask
     */
    @PostMapping("/rag/ask")
    public ResponseEntity<ApiResponse<RagAskResponse>> askQuestion(
            @Valid @RequestBody RagAskRequest request) {
        try {
            RagAskResponse response = documentRagSyncService.askCourseQuestion(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Trợ lý AI đã phản hồi thành công!"));
        } catch (Exception e) {
            log.error("Lỗi khi xử lý câu hỏi RAG: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi RAG: " + e.getMessage()));
        }
    }

    /**
     * 3. Lấy danh sách tài liệu môn học hiện có
     * GET /api/documents/course/{courseId}
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseDocument>>> getCourseDocuments(
            @PathVariable Long courseId) {
        List<CourseDocument> docs = courseDocumentRepository.findByCourseId(courseId);
        return ResponseEntity.ok(ApiResponse.success(docs, "Lấy danh sách tài liệu thành công"));
    }
}
