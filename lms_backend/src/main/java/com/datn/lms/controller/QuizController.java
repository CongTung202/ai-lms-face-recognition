package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.dto.QuizDto;
import com.datn.lms.dto.QuizSubmissionRequest;
import com.datn.lms.dto.QuizSubmissionResultDto;
import com.datn.lms.security.JwtTokenProvider;
import com.datn.lms.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/quizzes", "/api/lessons"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 1. Lấy thông tin bài trắc nghiệm của bài học để học viên làm bài
     * GET /api/quizzes/lesson/{lessonId} hoặc GET /api/lessons/{lessonId}/quiz
     */
    @GetMapping({"/lesson/{lessonId}", "/{lessonId}/quiz"})
    public ResponseEntity<ApiResponse<QuizDto>> getQuizForLesson(
            @PathVariable Long lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "studentId", required = false) Long fallbackStudentId) {
        try {
            Long studentId = resolveStudentId(authHeader, paramToken, fallbackStudentId);
            QuizDto quizDto = quizService.getQuizForStudent(lessonId, studentId);
            return ResponseEntity.ok(ApiResponse.success(quizDto, "Lấy đề thi trắc nghiệm bài học thành công"));
        } catch (Exception e) {
            log.error("Lỗi lấy đề thi trắc nghiệm bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 2. Nộp bài trắc nghiệm, chấm điểm và lưu kết quả vào CSDL
     * POST /api/quizzes/lesson/{lessonId}/submit hoặc POST /api/lessons/{lessonId}/quiz/submit
     */
    @PostMapping({"/lesson/{lessonId}/submit", "/{lessonId}/quiz/submit"})
    public ResponseEntity<ApiResponse<QuizSubmissionResultDto>> submitQuiz(
            @PathVariable Long lessonId,
            @RequestBody QuizSubmissionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "studentId", required = false) Long fallbackStudentId) {
        try {
            Long studentId = resolveStudentId(authHeader, paramToken, fallbackStudentId);
            if (studentId == null && request != null && request.getStudentId() != null) {
                studentId = request.getStudentId();
            }
            if (studentId == null) {
                studentId = 3L; // Fallback mặc định tài khoản sinh viên mẫu sv_2310900051
            }

            QuizSubmissionResultDto result = quizService.submitQuiz(lessonId, studentId, request);
            return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi nộp bài trắc nghiệm bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi nộp bài trắc nghiệm: " + e.getMessage()));
        }
    }

    private Long resolveStudentId(String authHeader, String paramToken, Long fallbackStudentId) {
        String token = null;
        if (paramToken != null && !paramToken.isBlank()) {
            token = paramToken.trim();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        if (token != null && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUserIdFromToken(token);
        }

        return fallbackStudentId;
    }
}
