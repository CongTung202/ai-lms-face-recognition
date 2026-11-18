package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.dto.CourseDocumentDto;
import com.datn.lms.dto.RagAskRequest;
import com.datn.lms.dto.RagAskResponse;
import com.datn.lms.entity.mongodb.CourseDocumentMongo;
import com.datn.lms.entity.mongodb.DocumentChunkMongo;
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
     * 1. Lấy toàn bộ danh sách tài liệu giáo trình kèm số chunks trong MongoDB
     * GET /api/documents
     */
    @GetMapping({"", "/"})
    public ResponseEntity<ApiResponse<List<CourseDocumentDto>>> getAllDocuments() {
        try {
            List<CourseDocumentDto> docs = documentRagSyncService.getAllDocuments();
            return ResponseEntity.ok(ApiResponse.success(docs, "Lấy danh sách toàn bộ tài liệu thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách tài liệu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(500, e.getMessage()));
        }
    }

    /**
     * 2. Giảng viên / Admin tải lên tài liệu (.pdf, .docx, .txt) & Tự động bóc tách thành các Chunks lưu vào MongoDB
     * POST /api/documents/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CourseDocumentMongo>> uploadDocument(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "lessonId", required = false) Long lessonId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("file") MultipartFile file) {
        try {
            CourseDocumentMongo mongoDoc = documentRagSyncService.uploadAndIndexDocument(courseId, lessonId, title, file);
            return ResponseEntity.ok(ApiResponse.success(mongoDoc,
                    String.format("Tải lên và bóc tách thành công! (%d trang, %d đoạn Chunks đã lưu vào MongoDB)",
                            mongoDoc.getTotalPages(), mongoDoc.getChunkCount())));
        } catch (Exception e) {
            log.error("Lỗi khi tải lên và lập chỉ mục tài liệu môn học {}: {}", courseId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi xử lý tài liệu: " + e.getMessage()));
        }
    }

    /**
     * 3. Lấy danh sách các đoạn Chunks từ MongoDB của một tài liệu
     * GET /api/documents/{id}/chunks
     */
    @GetMapping("/{id}/chunks")
    public ResponseEntity<ApiResponse<List<DocumentChunkMongo>>> getDocumentChunks(@PathVariable("id") Long id) {
        try {
            List<DocumentChunkMongo> chunks = documentRagSyncService.getDocumentChunks(id);
            return ResponseEntity.ok(ApiResponse.success(chunks,
                    String.format("Lấy thành công %d đoạn Chunks từ MongoDB", chunks.size())));
        } catch (Exception e) {
            log.error("Lỗi khi lấy chunks của tài liệu {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 4. Lấy toàn văn bản MongoDB của một tài liệu
     * GET /api/documents/{id}/content
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<ApiResponse<CourseDocumentMongo>> getDocumentContent(@PathVariable("id") Long id) {
        try {
            return documentRagSyncService.getDocumentMongo(id)
                    .map(doc -> ResponseEntity.ok(ApiResponse.success(doc, "Lấy nội dung tài liệu thành công")))
                    .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error(404, "Không tìm thấy nội dung văn bản trong MongoDB")));
        } catch (Exception e) {
            log.error("Lỗi khi lấy content tài liệu {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 5. Xóa tài liệu khỏi MySQL, xóa toàn bộ Chunks và Doc trong MongoDB
     * DELETE /api/documents/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable("id") Long id) {
        try {
            documentRagSyncService.deleteDocument(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa tài liệu và các Chunks liên quan trong MongoDB"));
        } catch (Exception e) {
            log.error("Lỗi khi xóa tài liệu {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 6. Tái phân đoạn văn bản và cập nhật chỉ mục Chunks trong MongoDB
     * POST /api/documents/{id}/reindex
     */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiResponse<CourseDocumentMongo>> reindexDocument(@PathVariable("id") Long id) {
        try {
            CourseDocumentMongo mongoDoc = documentRagSyncService.reindexDocument(id);
            return ResponseEntity.ok(ApiResponse.success(mongoDoc,
                    String.format("Tái lập chỉ mục RAG thành công! (%d trang, %d đoạn Chunks)",
                            mongoDoc.getTotalPages(), mongoDoc.getChunkCount())));
        } catch (Exception e) {
            log.error("Lỗi khi tái lập chỉ mục tài liệu {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 7. Sinh viên hỏi đáp bài học với Trợ Lý AI RAG (Tìm kiếm ngữ nghĩa & Trích dẫn số trang)
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
     * 8. Lấy danh sách tài liệu môn học hiện có kèm số Chunks MongoDB & buổi học đã gắn
     * GET /api/documents/course/{courseId}
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseDocumentDto>>> getCourseDocuments(
            @PathVariable Long courseId) {
        List<CourseDocumentDto> docs = documentRagSyncService.getDocumentsByCourseId(courseId);
        return ResponseEntity.ok(ApiResponse.success(docs, "Lấy danh sách tài liệu thành công"));
    }

    /**
     * 9. Lấy danh sách tài liệu đính kèm của một buổi học (lesson)
     * GET /api/documents/lesson/{lessonId}
     */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<CourseDocumentDto>>> getLessonDocuments(
            @PathVariable Long lessonId) {
        try {
            List<CourseDocumentDto> docs = documentRagSyncService.getDocumentsByLessonId(lessonId);
            return ResponseEntity.ok(ApiResponse.success(docs, "Lấy tài liệu buổi học thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi lấy tài liệu buổi học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 10. Gán hoặc Hủy gán tài liệu vào một buổi học
     * PUT /api/documents/{id}/assign-lesson?lessonId={lessonId}
     */
    @PutMapping("/{id}/assign-lesson")
    public ResponseEntity<ApiResponse<CourseDocumentDto>> assignLesson(
            @PathVariable("id") Long id,
            @RequestParam(value = "lessonId", required = false) Long lessonId) {
        try {
            CourseDocumentDto updated = documentRagSyncService.assignDocumentToLesson(id, lessonId);
            return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật gán tài liệu cho buổi học thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi gán tài liệu {} cho buổi học {}: {}", id, lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
