package com.datn.lms.service;

import com.datn.lms.dto.FaceAiVerifyResultDto;
import com.datn.lms.dto.LessonAccessStatusDto;
import com.datn.lms.dto.ProgressHeartbeatRequest;
import com.datn.lms.dto.ProgressResponse;
import com.datn.lms.entity.mysql.*;
import com.datn.lms.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningProgressService {

    private final LearningProgressRepository learningProgressRepository;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final FaceAttendanceLogRepository faceAttendanceLogRepository;
    private final FaceAiClientService faceAiClientService;
    private final QuizRepository quizRepository;
    private final CourseChapterRepository chapterRepository;

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

        // 5. Kiểm tra điều kiện hoàn thành bài học (>= 85%) và bài kiểm tra trắc nghiệm
        boolean hasQuiz = quizRepository.existsByLessonId(lesson.getId());
        boolean quizFulfilled = !hasQuiz || Boolean.TRUE.equals(progress.getQuizPassed());

        if (percentage >= COMPLETION_THRESHOLD_PERCENT && quizFulfilled && !Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            log.info("Chúc mừng sinh viên {} đã hoàn thành bài học '{}' (Đạt {}%, Quiz: {})",
                    student.getStudentCode(), lesson.getTitle(), percentage, progress.getQuizPassed());
        }

        LearningProgress saved = learningProgressRepository.save(progress);
        return mapToResponse(saved);
    }

    /**
     * Kiểm tra trạng thái truy cập, phân quyền và khóa tuần tự / ngày học của bài giảng
     */
    @Transactional(readOnly = true)
    public LessonAccessStatusDto checkLessonAccess(Long userId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học ID=" + lessonId));

        boolean hasQuiz = quizRepository.existsByLessonId(lessonId);
        LearningProgress progress = userId != null ? learningProgressRepository.findByStudentUserIdAndLessonId(userId, lessonId).orElse(null) : null;
        boolean isCompleted = progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
        boolean quizPassed = progress != null && Boolean.TRUE.equals(progress.getQuizPassed());

        Course course = lesson.getChapter() != null ? lesson.getChapter().getCourse() : null;
        boolean sequentialEnforced = (course == null || Boolean.TRUE.equals(course.getIsSequentialLearning()))
                && Boolean.TRUE.equals(lesson.getIsSequential());

        // 1. Kiểm tra ngày học (Setting ngày học)
        LocalDate today = LocalDate.now();
        if (lesson.getScheduledDate() != null && lesson.getScheduledDate().isAfter(today)) {
            return LessonAccessStatusDto.builder()
                    .lessonId(lessonId)
                    .isLocked(true)
                    .lockReason("Chưa đến ngày học theo lịch trình (Mở ngày " + lesson.getScheduledDate() + ")")
                    .scheduledDate(lesson.getScheduledDate())
                    .unlockAt(lesson.getUnlockAt())
                    .isCompleted(isCompleted)
                    .quizPassed(quizPassed)
                    .hasQuiz(hasQuiz)
                    .build();
        }

        if (lesson.getUnlockAt() != null && lesson.getUnlockAt().isAfter(LocalDateTime.now())) {
            return LessonAccessStatusDto.builder()
                    .lessonId(lessonId)
                    .isLocked(true)
                    .lockReason("Chưa tới giờ mở bài giảng (" + lesson.getUnlockAt() + ")")
                    .scheduledDate(lesson.getScheduledDate())
                    .unlockAt(lesson.getUnlockAt())
                    .isCompleted(isCompleted)
                    .quizPassed(quizPassed)
                    .hasQuiz(hasQuiz)
                    .build();
        }

        // 2. Kiểm tra học tuần tự (Sequential learning)
        if (sequentialEnforced) {
            Lesson prereq = lesson.getPrerequisiteLesson();
            // Nếu không có prereq chỉ định, kiểm tra bài đứng ngay trước trong chương
            if (prereq == null && lesson.getOrderIndex() != null && lesson.getOrderIndex() > 1 && lesson.getChapter() != null) {
                List<Lesson> siblings = lessonRepository.findByChapterIdOrderByOrderIndexAsc(lesson.getChapter().getId());
                for (int i = 0; i < siblings.size(); i++) {
                    if (siblings.get(i).getId().equals(lessonId) && i > 0) {
                        prereq = siblings.get(i - 1);
                        break;
                    }
                }
            }

            if (prereq != null && userId != null) {
                LearningProgress prereqProgress = learningProgressRepository.findByStudentUserIdAndLessonId(userId, prereq.getId()).orElse(null);
                boolean prereqVideoDone = prereqProgress != null && (Boolean.TRUE.equals(prereqProgress.getIsCompleted()) || prereqProgress.getCompletionPercentage() >= COMPLETION_THRESHOLD_PERCENT);
                boolean prereqHasQuiz = quizRepository.existsByLessonId(prereq.getId());
                boolean prereqQuizDone = !prereqHasQuiz || (prereqProgress != null && Boolean.TRUE.equals(prereqProgress.getQuizPassed()));

                if (!prereqVideoDone || !prereqQuizDone) {
                    String reason = !prereqVideoDone
                            ? "Khóa học tuần tự: Bạn cần xem video (>= 85%) bài '" + prereq.getTitle() + "' trước!"
                            : "Khóa học tuần tự: Bạn cần làm và ĐẠT bài trắc nghiệm của bài '" + prereq.getTitle() + "' để mở khóa!";
                    return LessonAccessStatusDto.builder()
                            .lessonId(lessonId)
                            .isLocked(true)
                            .lockReason(reason)
                            .scheduledDate(lesson.getScheduledDate())
                            .unlockAt(lesson.getUnlockAt())
                            .prerequisiteLessonId(prereq.getId())
                            .prerequisiteLessonTitle(prereq.getTitle())
                            .isCompleted(isCompleted)
                            .quizPassed(quizPassed)
                            .hasQuiz(hasQuiz)
                            .build();
                }
            }
        }

        return LessonAccessStatusDto.builder()
                .lessonId(lessonId)
                .isLocked(false)
                .lockReason(null)
                .scheduledDate(lesson.getScheduledDate())
                .unlockAt(lesson.getUnlockAt())
                .prerequisiteLessonId(lesson.getPrerequisiteLesson() != null ? lesson.getPrerequisiteLesson().getId() : null)
                .prerequisiteLessonTitle(lesson.getPrerequisiteLesson() != null ? lesson.getPrerequisiteLesson().getTitle() : null)
                .isCompleted(isCompleted)
                .quizPassed(quizPassed)
                .hasQuiz(hasQuiz)
                .build();
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
        boolean canUnlock = Boolean.TRUE.equals(p.getIsCompleted()) || 
                (p.getCompletionPercentage() != null && p.getCompletionPercentage() >= COMPLETION_THRESHOLD_PERCENT && Boolean.TRUE.equals(p.getQuizPassed()));

        return ProgressResponse.builder()
                .lessonId(p.getLesson().getId())
                .courseId(p.getCourse() != null ? p.getCourse().getId() : null)
                .lastWatchedSecond(p.getLastWatchedSecond())
                .maxWatchedSecond(p.getMaxWatchedSecond())
                .totalTimeSpentSeconds(p.getTotalTimeSpentSeconds())
                .completionPercentage(p.getCompletionPercentage())
                .isCompleted(p.getIsCompleted())
                .completedAt(p.getCompletedAt())
                .quizPassed(p.getQuizPassed())
                .quizScore(p.getQuizScore())
                .canUnlockNext(canUnlock)
                .build();
    }
}
