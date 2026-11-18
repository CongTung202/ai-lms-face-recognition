package com.datn.lms.service;

import com.datn.lms.dto.QuizDto;
import com.datn.lms.dto.QuizOptionDto;
import com.datn.lms.dto.QuizQuestionDto;
import com.datn.lms.dto.QuizSubmissionRequest;
import com.datn.lms.dto.QuizSubmissionResultDto;
import com.datn.lms.entity.mysql.*;
import com.datn.lms.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final CourseChapterRepository chapterRepository;

    /**
     * Lấy nội dung đề thi trắc nghiệm của bài học để học viên làm bài
     */
    @Transactional
    public QuizDto getQuizForStudent(Long lessonId, Long studentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học có ID=" + lessonId));

        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseGet(() -> createDefaultQuizForLesson(lesson));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        List<QuizQuestionDto> questionDtos = new ArrayList<>();

        for (QuizQuestion q : questions) {
            List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
            List<QuizOptionDto> optDtos = options.stream().map(opt -> QuizOptionDto.builder()
                    .id(opt.getId())
                    .optionText(opt.getOptionText())
                    .orderIndex(opt.getOrderIndex())
                    .build()
            ).toList();

            questionDtos.add(QuizQuestionDto.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType().name())
                    .orderIndex(q.getOrderIndex())
                    .points(q.getPoints())
                    .options(optDtos)
                    .build());
        }

        // Kiểm tra lịch sử làm bài của học viên (nếu có studentId)
        boolean alreadyPassed = false;
        BigDecimal bestScore = BigDecimal.ZERO;
        int attemptsMade = 0;

        if (studentId != null) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByStudentUserIdAndLessonIdOrderBySubmittedAtDesc(studentId, lessonId);
            attemptsMade = attempts.size();
            for (QuizAttempt a : attempts) {
                if (Boolean.TRUE.equals(a.getIsPassed())) {
                    alreadyPassed = true;
                }
                if (a.getScorePercentage() != null && a.getScorePercentage().compareTo(bestScore) > 0) {
                    bestScore = a.getScorePercentage();
                }
            }
        }

        return QuizDto.builder()
                .id(quiz.getId())
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passingScorePercentage(quiz.getPassingScorePercentage())
                .maxAttempts(quiz.getMaxAttempts())
                .questionsCount(questionDtos.size())
                .questions(questionDtos)
                .alreadyPassed(alreadyPassed)
                .bestScore(bestScore)
                .attemptsMade(attemptsMade)
                .build();
    }

    /**
     * Chấm điểm và lưu kết quả bài trắc nghiệm vào cơ sở dữ liệu (3NF)
     */
    @Transactional
    public QuizSubmissionResultDto submitQuiz(Long lessonId, Long studentId, QuizSubmissionRequest req) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học có ID=" + lessonId));

        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học này chưa được cấu hình bài kiểm tra trắc nghiệm"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên có ID=" + studentId));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new RuntimeException("Đề kiểm tra chưa có câu hỏi nào!");
        }

        Map<Long, Long> studentAnswers = new HashMap<>();
        if (req != null && req.getAnswers() != null) {
            for (QuizSubmissionRequest.AnswerItem item : req.getAnswers()) {
                if (item.getQuestionId() != null && item.getSelectedOptionId() != null) {
                    studentAnswers.put(item.getQuestionId(), item.getSelectedOptionId());
                }
            }
        }

        int correctCount = 0;
        int totalQuestions = questions.size();
        List<QuizSubmissionResultDto.QuestionFeedbackDto> feedbacks = new ArrayList<>();
        List<QuizAttemptAnswer> attemptAnswers = new ArrayList<>();

        for (QuizQuestion q : questions) {
            List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
            QuizOption correctOption = options.stream().filter(QuizOption::getIsCorrect).findFirst().orElse(null);
            Long selectedOptionId = studentAnswers.get(q.getId());

            boolean isCorrect = false;
            QuizOption selectedOption = null;

            if (selectedOptionId != null) {
                selectedOption = options.stream().filter(o -> o.getId().equals(selectedOptionId)).findFirst().orElse(null);
                if (selectedOption != null && Boolean.TRUE.equals(selectedOption.getIsCorrect())) {
                    isCorrect = true;
                    correctCount++;
                }
            }

            attemptAnswers.add(QuizAttemptAnswer.builder()
                    .question(q)
                    .selectedOption(selectedOption)
                    .isCorrect(isCorrect)
                    .build());

            feedbacks.add(QuizSubmissionResultDto.QuestionFeedbackDto.builder()
                    .questionId(q.getId())
                    .questionText(q.getQuestionText())
                    .selectedOptionId(selectedOptionId)
                    .correctOptionId(correctOption != null ? correctOption.getId() : null)
                    .correctOptionText(correctOption != null ? correctOption.getOptionText() : "")
                    .isCorrect(isCorrect)
                    .explanation(q.getExplanation() != null ? q.getExplanation() : "Đáp án đúng theo chuẩn kiến thức bài giảng.")
                    .build());
        }

        // Tính điểm phần trăm
        BigDecimal scorePercentage = BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);

        boolean isPassed = scorePercentage.compareTo(quiz.getPassingScorePercentage()) >= 0;

        // Lưu lượt làm bài vào quiz_attempts
        long existingAttempts = quizAttemptRepository.countByStudentUserIdAndQuizId(studentId, quiz.getId());
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .lesson(lesson)
                .attemptNumber((int) existingAttempts + 1)
                .scorePercentage(scorePercentage)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctCount)
                .isPassed(isPassed)
                .timeSpentSeconds(req != null && req.getTimeSpentSeconds() != null ? req.getTimeSpentSeconds() : 0)
                .submittedAt(LocalDateTime.now())
                .build();

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // Lưu chi tiết từng câu trả lời vào quiz_attempt_answers
        for (QuizAttemptAnswer ans : attemptAnswers) {
            ans.setAttempt(savedAttempt);
            quizAttemptAnswerRepository.save(ans);
        }

        // Cập nhật LearningProgress
        LearningProgress progress = learningProgressRepository.findByStudentUserIdAndLessonId(studentId, lessonId)
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

        progress.setQuizPassed(Boolean.TRUE.equals(progress.getQuizPassed()) || isPassed);
        if (progress.getQuizScore() == null || scorePercentage.compareTo(progress.getQuizScore()) > 0) {
            progress.setQuizScore(scorePercentage);
        }
        progress.setQuizAttemptsCount((progress.getQuizAttemptsCount() != null ? progress.getQuizAttemptsCount() : 0) + 1);

        // Nếu đạt bài quiz và video đã xem >= 85%, đánh dấu hoàn thành bài học
        if (Boolean.TRUE.equals(progress.getQuizPassed()) && progress.getCompletionPercentage() >= 85) {
            progress.setIsCompleted(true);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        }
        learningProgressRepository.save(progress);

        // Tìm bài học tiếp theo trong khóa học
        Lesson nextLesson = findNextLessonInCourse(lesson);
        Long nextLessonId = nextLesson != null ? nextLesson.getId() : null;
        String nextLessonTitle = nextLesson != null ? nextLesson.getTitle() : null;

        String message = isPassed
                ? String.format("Chúc mừng bạn đã ĐẠT bài trắc nghiệm với điểm số %s%% (%d/%d câu đúng)! Đã đủ điều kiện mở khóa bài tiếp theo.", scorePercentage, correctCount, totalQuestions)
                : String.format("Bạn đạt %s%% (%d/%d câu đúng). Chưa đạt ngưỡng %s%% yêu cầu. Hãy ôn lại video và làm lại bài trắc nghiệm!", scorePercentage, correctCount, totalQuestions, quiz.getPassingScorePercentage());

        log.info("Sinh viên {} nộp bài quiz bài học {}: Kết quả {}%, Đạt: {}", student.getStudentCode(), lessonId, scorePercentage, isPassed);

        return QuizSubmissionResultDto.builder()
                .attemptId(savedAttempt.getId())
                .quizId(quiz.getId())
                .lessonId(lesson.getId())
                .scorePercentage(scorePercentage)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctCount)
                .isPassed(isPassed)
                .passingScorePercentage(quiz.getPassingScorePercentage())
                .canUnlockNext(isPassed)
                .nextLessonId(nextLessonId)
                .nextLessonTitle(nextLessonTitle)
                .message(message)
                .feedback(feedbacks)
                .build();
    }

    /**
     * Tìm bài học kế tiếp trong lộ trình khóa học
     */
    public Lesson findNextLessonInCourse(Lesson currentLesson) {
        CourseChapter currentChapter = currentLesson.getChapter();
        Course course = currentChapter.getCourse();

        // 1. Tìm bài học tiếp theo trong cùng chương
        List<Lesson> lessonsInChapter = lessonRepository.findByChapterIdOrderByOrderIndexAsc(currentChapter.getId());
        for (int i = 0; i < lessonsInChapter.size(); i++) {
            if (lessonsInChapter.get(i).getId().equals(currentLesson.getId())) {
                if (i + 1 < lessonsInChapter.size()) {
                    return lessonsInChapter.get(i + 1);
                }
                break;
            }
        }

        // 2. Nếu là bài cuối của chương, tìm bài đầu tiên của chương tiếp theo
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getId().equals(currentChapter.getId())) {
                if (i + 1 < chapters.size()) {
                    CourseChapter nextChapter = chapters.get(i + 1);
                    List<Lesson> nextChapLessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(nextChapter.getId());
                    if (!nextChapLessons.isEmpty()) {
                        return nextChapLessons.get(0);
                    }
                }
                break;
            }
        }

        return null;
    }

    /**
     * Tự động sinh đề kiểm tra mặc định nếu bài học chưa có quiz
     */
    private Quiz createDefaultQuizForLesson(Lesson lesson) {
        String title = "Bài trắc nghiệm: " + lesson.getTitle();
        Quiz quiz = Quiz.builder()
                .lesson(lesson)
                .title(title)
                .description("Bài kiểm tra trắc nghiệm củng cố kiến thức sau bài học. Yêu cầu đạt từ 75% để mở khóa bài tiếp theo.")
                .timeLimitMinutes(15)
                .passingScorePercentage(new BigDecimal("75.00"))
                .maxAttempts(5)
                .isActive(true)
                .build();
        Quiz savedQuiz = quizRepository.save(quiz);

        // Tạo 3 câu hỏi mẫu cơ bản
        QuizQuestion q1 = QuizQuestion.builder()
                .quiz(savedQuiz)
                .questionText("Mục tiêu trọng tâm của bài học '" + lesson.getTitle() + "' là gì?")
                .questionType(QuizQuestion.QuestionType.SINGLE_CHOICE)
                .explanation("Mục tiêu là nắm vững kiến thức lý thuyết và vận dụng giải quyết bài toán thực tế trong hệ thống LMS.")
                .orderIndex(1)
                .points(BigDecimal.ONE)
                .build();
        QuizQuestion savedQ1 = quizQuestionRepository.save(q1);
        quizOptionRepository.save(QuizOption.builder().question(savedQ1).optionText("Nắm vững lý thuyết và áp dụng chuẩn kiến trúc vào hệ thống LMS.").isCorrect(true).orderIndex(1).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ1).optionText("Chỉ xem lướt qua video mà không cần thực hành.").isCorrect(false).orderIndex(2).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ1).optionText("Bỏ qua các bước kiểm thử bảo mật.").isCorrect(false).orderIndex(3).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ1).optionText("Lưu trữ dữ liệu phi cấu trúc không cần chuẩn hoá.").isCorrect(false).orderIndex(4).build());

        QuizQuestion q2 = QuizQuestion.builder()
                .quiz(savedQuiz)
                .questionText("Trong bài học này, tiêu chí nào được dùng để xác nhận hoàn thành nội dung bài giảng?")
                .questionType(QuizQuestion.QuestionType.SINGLE_CHOICE)
                .explanation("Hệ thống yêu cầu xem tối thiểu 85% thời lượng video và vượt qua bài trắc nghiệm củng cố.")
                .orderIndex(2)
                .points(BigDecimal.ONE)
                .build();
        QuizQuestion savedQ2 = quizQuestionRepository.save(q2);
        quizOptionRepository.save(QuizOption.builder().question(savedQ2).optionText("Đạt tối thiểu 85% thời lượng video và vượt qua bài thi trắc nghiệm.").isCorrect(true).orderIndex(1).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ2).optionText("Tua video về giây cuối cùng trong 5 giây đầu.").isCorrect(false).orderIndex(2).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ2).optionText("Không cần mở video vẫn tự động đánh dấu xong.").isCorrect(false).orderIndex(3).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ2).optionText("Chỉ cần tải tài liệu đính kèm.").isCorrect(false).orderIndex(4).build());

        QuizQuestion q3 = QuizQuestion.builder()
                .quiz(savedQuiz)
                .questionText("Cơ chế học tập tuần tự (Sequential Learning) trong hệ thống đảm bảo điều gì?")
                .questionType(QuizQuestion.QuestionType.SINGLE_CHOICE)
                .explanation("Đảm bảo người học hoàn thành bài học trước (video + quiz) và đến đúng ngày học quy định mới mở khóa bài sau.")
                .orderIndex(3)
                .points(BigDecimal.ONE)
                .build();
        QuizQuestion savedQ3 = quizQuestionRepository.save(q3);
        quizOptionRepository.save(QuizOption.builder().question(savedQ3).optionText("Đảm bảo nắm vững kiến thức bài trước trước khi chuyển sang bài tiếp theo theo lộ trình sư phạm.").isCorrect(true).orderIndex(1).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ3).optionText("Cho phép nhảy cóc học bài cuối cùng ngay lập tức.").isCorrect(false).orderIndex(2).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ3).optionText("Chỉ mở bài học vào buổi tối.").isCorrect(false).orderIndex(3).build());
        quizOptionRepository.save(QuizOption.builder().question(savedQ3).optionText("Không cho phép làm lại bài tập trắc nghiệm.").isCorrect(false).orderIndex(4).build());

        return savedQuiz;
    }
}
