package com.datn.lms.service;

import com.datn.lms.dto.FaceAiVerifyResultDto;
import com.datn.lms.dto.ProgressHeartbeatRequest;
import com.datn.lms.dto.ProgressResponse;
import com.datn.lms.entity.mysql.FaceAttendanceLog;
import com.datn.lms.entity.mysql.LearningProgress;
import com.datn.lms.entity.mysql.Lesson;
import com.datn.lms.entity.mysql.Student;
import com.datn.lms.repository.mysql.FaceAttendanceLogRepository;
import com.datn.lms.repository.mysql.LearningProgressRepository;
import com.datn.lms.repository.mysql.LessonRepository;
import com.datn.lms.repository.mysql.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningProgressService {

    private final LearningProgressRepository learningProgressRepository;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final FaceAttendanceLogRepository faceAttendanceLogRepository;
    private final FaceAiClientService faceAiClientService;

    private static final int COMPLETION_THRESHOLD_PERCENT = 85; // Đạt 85% coi như hoàn thành bài học
    private static final int MAX_ALLOWABLE_JUMP_SECONDS = 15;   // Biên độ chống tua (sai số heartbeat 15s)

    /**
     * Ghi nhận nhịp tim tiến trình học (Heartbeat) & áp dụng thuật toán chống tua gian lận
     */
    @Transactional
    public ProgressResponse recordHeartbeat(Long userId, ProgressHeartbeatRequest req) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin sinh viên với userId=" + userId));

        Lesson lesson = lessonRepository.findById(req.getLessonId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học id=" + req.getLessonId()));

        LearningProgress progress = learningProgressRepository
                .findByStudentUserIdAndLessonId(userId, req.getLessonId())
                .orElseGet(() -> LearningProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .course(lesson.getChapter().getCourse())
                        .lastWatchedSecond(0)
                        .maxWatchedSecond(0)
                        .totalTimeSpentSeconds(0)
                        .completionPercentage(0)
                        .isCompleted(false)
                        .build());

        int currentSecond = Math.max(0, req.getCurrentTimeSecond());
        int prevLastWatched = progress.getLastWatchedSecond() != null ? progress.getLastWatchedSecond() : 0;
        int prevMaxWatched = progress.getMaxWatchedSecond() != null ? progress.getMaxWatchedSecond() : 0;
        int duration = req.getDurationSecond() > 0 ? req.getDurationSecond() : (lesson.getDurationSeconds() > 0 ? lesson.getDurationSeconds() : 1);

        // 1. Tính tổng thời gian thực tế đã bỏ ra xem (Total Time Spent)
        if (currentSecond > prevLastWatched && (currentSecond - prevLastWatched) <= MAX_ALLOWABLE_JUMP_SECONDS) {
            int delta = currentSecond - prevLastWatched;
            progress.setTotalTimeSpentSeconds(progress.getTotalTimeSpentSeconds() + delta);
        }

        // 2. Chống tua gian lận: Chỉ tăng maxWatchedSecond nếu học sinh xem liền mạch trong ngưỡng an toàn
        if (currentSecond <= prevMaxWatched + MAX_ALLOWABLE_JUMP_SECONDS) {
            int newMaxWatched = Math.max(prevMaxWatched, currentSecond);
            progress.setMaxWatchedSecond(newMaxWatched);
        } else {
            log.warn("Cảnh báo tua bài học! Sinh viên {} nhảy từ giây {} đến giây {} (vượt ngưỡng {}s)",
                    student.getStudentCode(), prevMaxWatched, currentSecond, MAX_ALLOWABLE_JUMP_SECONDS);
        }

        // 3. Cập nhật vị trí dừng gần nhất để hỗ trợ Resume
        progress.setLastWatchedSecond(currentSecond);

        // 4. Tính toán phần trăm hoàn thành dựa trên maxWatchedSecond đã xác thực
        int percentage = Math.min(100, (int) Math.round(((double) progress.getMaxWatchedSecond() / duration) * 100));
        progress.setCompletionPercentage(percentage);

        // 5. Kiểm tra điều kiện hoàn thành bài học (>= 85%)
        if (percentage >= COMPLETION_THRESHOLD_PERCENT && !Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            log.info("Chúc mừng sinh viên {} đã hoàn thành bài học '{}' (Đạt {}%)",
                    student.getStudentCode(), lesson.getTitle(), percentage);
        }

        LearningProgress saved = learningProgressRepository.save(progress);
        return mapToResponse(saved);
    }

    /**
     * Lấy tiến trình học tập hiện tại để trình duyệt Resume bài giảng
     */
    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long userId, Long lessonId) {
        return learningProgressRepository.findByStudentUserIdAndLessonId(userId, lessonId)
                .map(this::mapToResponse)
                .orElse(ProgressResponse.builder()
                        .lessonId(lessonId)
                        .lastWatchedSecond(0)
                        .maxWatchedSecond(0)
                        .totalTimeSpentSeconds(0)
                        .completionPercentage(0)
                        .isCompleted(false)
                        .build());
    }

    /**
     * Điểm danh khuôn mặt bất thình lình trong lúc đang xem video bài giảng
     */
    @Transactional
    public FaceAiVerifyResultDto checkInFaceDuringVideo(Long userId, Long lessonId, MultipartFile faceImage) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin sinh viên"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học id=" + lessonId));

        FaceAiVerifyResultDto verifyResult;
        try {
            // 1. Gọi Face AI Microservice đối chiếu khuôn mặt trực tiếp
            verifyResult = faceAiClientService.verifyFace(student.getStudentCode(), faceImage);
        } catch (Exception e) {
            log.error("Lỗi khi kết nối Face AI microservice: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối dịch vụ Face AI: " + e.getMessage());
        }

        // 2. Lưu nhật ký giám sát vào bảng face_attendance_logs
        FaceAttendanceLog.VerificationStatus status = verifyResult.isMatched()
                ? FaceAttendanceLog.VerificationStatus.SUCCESS
                : FaceAttendanceLog.VerificationStatus.FAILED;

        FaceAttendanceLog logEntry = FaceAttendanceLog.builder()
                .student(student)
                .lesson(lesson)
                .verificationStatus(status)
                .similarityScore(verifyResult.getSimilarity())
                .livenessScore(verifyResult.getLivenessScore())
                .note(verifyResult.isMatched()
                        ? "Điểm danh ngẫu nhiên thành công trong giờ học video"
                        : "Phát hiện không khớp khuôn mặt khi xem video: " + (verifyResult.getReasons() != null ? String.join(", ", verifyResult.getReasons()) : ""))
                .build();

        faceAttendanceLogRepository.save(logEntry);
        log.info("Đã ghi nhận nhật ký điểm danh video bài học {}: Sinh viên {}, Kết quả: {}",
                lessonId, student.getStudentCode(), status);

        return verifyResult;
    }

    private ProgressResponse mapToResponse(LearningProgress p) {
        return ProgressResponse.builder()
                .lessonId(p.getLesson().getId())
                .courseId(p.getCourse() != null ? p.getCourse().getId() : null)
                .lastWatchedSecond(p.getLastWatchedSecond())
                .maxWatchedSecond(p.getMaxWatchedSecond())
                .totalTimeSpentSeconds(p.getTotalTimeSpentSeconds())
                .completionPercentage(p.getCompletionPercentage())
                .isCompleted(p.getIsCompleted())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
