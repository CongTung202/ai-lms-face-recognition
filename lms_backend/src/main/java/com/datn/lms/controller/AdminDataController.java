package com.datn.lms.controller;

import com.datn.lms.dto.AdminProfileDto;
import com.datn.lms.dto.AdminProfileUpdateRequest;
import com.datn.lms.dto.AdminUserDto;
import com.datn.lms.dto.ApiResponse;
import com.datn.lms.entity.mysql.*;
import com.datn.lms.repository.mongodb.CourseDocumentMongoRepository;
import com.datn.lms.repository.mysql.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/api/admin", "/admin/api"})
@Slf4j
public class AdminDataController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final CourseDocumentRepository documentRepository;
    private final FaceAttendanceLogRepository attendanceLogRepository;
    private final CourseDocumentMongoRepository mongoDocumentRepository;
    private final SubjectRepository subjectRepository;
    private final LecturerRepository lecturerRepository;
    private final RestTemplate restTemplate;

    public AdminDataController(
            UserRepository userRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            CourseChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            VideoAssetRepository videoAssetRepository,
            CourseDocumentRepository documentRepository,
            FaceAttendanceLogRepository attendanceLogRepository,
            CourseDocumentMongoRepository mongoDocumentRepository,
            SubjectRepository subjectRepository,
            LecturerRepository lecturerRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.documentRepository = documentRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.mongoDocumentRepository = mongoDocumentRepository;
        this.subjectRepository = subjectRepository;
        this.lecturerRepository = lecturerRepository;
        this.restTemplate = new RestTemplate();
    }

    // =========================================================================
    // 1. PROFILE API
    // =========================================================================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AdminProfileDto>> getProfile(
            @RequestParam(value = "username", required = false, defaultValue = "nct30000") String username) {

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .email("nct30000@gmail.com")
                            .phone("0334402527")
                            .fullName("NGUYỄN CÔNG TÙNG")
                            .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80")
                            .passwordHash("123456")
                            .role(User.Role.ADMIN)
                            .status(User.UserStatus.ACTIVE)
                            .build();
                    return userRepository.save(newUser);
                });

        Optional<Student> studentOpt = studentRepository.findById(user.getId());

        AdminProfileDto dto = AdminProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone() != null ? user.getPhone() : "0334402527")
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80")
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .cccd("037205001191")
                .gender(studentOpt.map(s -> s.getGender().name().equals("FEMALE") ? "Nữ" : "Nam").orElse("Nam"))
                .dateOfBirth(studentOpt.map(s -> s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "2005-02-20").orElse("2005-02-20"))
                .address("Lạc Thiện, Xã Gia Lạc, Ninh Bình, Việt Nam")
                .faceRegistered(studentOpt.map(Student::getFaceRegistered).orElse(true))
                .faceImageUrl(studentOpt.map(Student::getFaceImageUrl).orElse("https://res.cloudinary.com/dhefmthim/image/upload/v1728900000/lms_datn/faces/profile_nct.jpg"))
                .memberType(user.getRole() == User.Role.ADMIN ? "Quản trị viên" : "Học viên VIP")
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto, "Lấy thông tin tài khoản thành công"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AdminProfileDto>> updateProfile(
            @RequestParam(value = "username", required = false, defaultValue = "nct30000") String username,
            @RequestBody AdminProfileUpdateRequest request) {

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản để cập nhật"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        userRepository.save(user);

        Optional<Student> studentOpt = studentRepository.findById(user.getId());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (request.getGender() != null) {
                student.setGender("Nữ".equalsIgnoreCase(request.getGender()) ? Student.Gender.FEMALE : Student.Gender.MALE);
            }
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
                try {
                    student.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
                } catch (Exception ignored) {}
            }
            studentRepository.save(student);
        }

        log.info("Cập nhật hồ sơ thành công: {}", user.getUsername());
        return getProfile(user.getUsername());
    }

    // =========================================================================
    // 2. USERS MANAGEMENT (CRUD: LIST, CREATE, UPDATE, DELETE)
    // =========================================================================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone() != null ? u.getPhone() : "-");
            map.put("role", u.getRole().name());
            map.put("status", u.getStatus().name());
            map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");

            Optional<Student> sOpt = studentRepository.findById(u.getId());
            if (sOpt.isPresent()) {
                Student s = sOpt.get();
                map.put("studentCode", s.getStudentCode());
                map.put("classId", s.getClassId());
                map.put("faceRegistered", s.getFaceRegistered());
                map.put("faceImageUrl", s.getFaceImageUrl());
                map.put("gender", s.getGender().name());
                map.put("dateOfBirth", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "");
            } else {
                map.put("studentCode", "N/A");
                map.put("classId", null);
                map.put("faceRegistered", false);
                map.put("faceImageUrl", null);
                map.put("gender", "MALE");
                map.put("dateOfBirth", "");
            }
            result.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách người dùng thành công"));
    }

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@RequestBody AdminUserDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên đăng nhập không được để trống"));
        }
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên đăng nhập đã tồn tại trong hệ thống"));
        }

        User user = User.builder()
                .username(dto.getUsername().trim())
                .fullName(dto.getFullName() != null ? dto.getFullName().trim() : dto.getUsername().trim())
                .email(dto.getEmail() != null ? dto.getEmail().trim() : dto.getUsername().trim() + "@lms.edu.vn")
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                .passwordHash(dto.getPassword() != null && !dto.getPassword().isBlank() ? dto.getPassword() : "123456")
                .role(dto.getRole() != null ? User.Role.valueOf(dto.getRole().toUpperCase()) : User.Role.STUDENT)
                .status(dto.getStatus() != null ? User.UserStatus.valueOf(dto.getStatus().toUpperCase()) : User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        if (user.getRole() == User.Role.STUDENT) {
            String studentCode = dto.getStudentCode() != null && !dto.getStudentCode().isBlank()
                    ? dto.getStudentCode().trim()
                    : "SV" + System.currentTimeMillis() % 1000000;

            Student student = Student.builder()
                    .user(user)
                    .studentCode(studentCode)
                    .classId(dto.getClassId() != null ? dto.getClassId() : 1L)
                    .majorId(1L)
                    .gender("FEMALE".equalsIgnoreCase(dto.getGender()) ? Student.Gender.FEMALE : Student.Gender.MALE)
                    .dateOfBirth(dto.getDateOfBirth() != null && !dto.getDateOfBirth().isBlank() ? LocalDate.parse(dto.getDateOfBirth()) : LocalDate.of(2005, 1, 1))
                    .faceRegistered(Boolean.TRUE.equals(dto.getFaceRegistered()))
                    .faceImageUrl(dto.getFaceImageUrl())
                    .build();
            studentRepository.save(student);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("message", "Đã tạo tài khoản người dùng thành công");
        return ResponseEntity.ok(ApiResponse.success(resp, "Tạo người dùng thành công"));
    }

    @PutMapping("/users/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserDto dto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng có ID: " + id));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getRole() != null) {
            user.setRole(User.Role.valueOf(dto.getRole().toUpperCase()));
        }
        if (dto.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(dto.getStatus().toUpperCase()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(dto.getPassword().trim());
        }

        userRepository.save(user);

        // Cập nhật thông tin học viên nếu có
        Optional<Student> sOpt = studentRepository.findById(id);
        if (sOpt.isPresent()) {
            Student s = sOpt.get();
            if (dto.getStudentCode() != null && !dto.getStudentCode().isBlank()) {
                s.setStudentCode(dto.getStudentCode().trim());
            }
            if (dto.getGender() != null) {
                s.setGender("FEMALE".equalsIgnoreCase(dto.getGender()) ? Student.Gender.FEMALE : Student.Gender.MALE);
            }
            if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isBlank()) {
                try {
                    s.setDateOfBirth(LocalDate.parse(dto.getDateOfBirth()));
                } catch (Exception ignored) {}
            }
            if (dto.getFaceRegistered() != null) {
                s.setFaceRegistered(dto.getFaceRegistered());
            }
            if (dto.getFaceImageUrl() != null) {
                s.setFaceImageUrl(dto.getFaceImageUrl().trim());
            }
            studentRepository.save(s);
        } else if (user.getRole() == User.Role.STUDENT) {
            // Nếu đổi role sang STUDENT mà chưa có bản ghi student
            Student newStudent = Student.builder()
                    .user(user)
                    .studentCode(dto.getStudentCode() != null && !dto.getStudentCode().isBlank() ? dto.getStudentCode().trim() : "SV" + user.getId())
                    .classId(1L)
                    .majorId(1L)
                    .gender("FEMALE".equalsIgnoreCase(dto.getGender()) ? Student.Gender.FEMALE : Student.Gender.MALE)
                    .dateOfBirth(LocalDate.of(2005, 1, 1))
                    .faceRegistered(Boolean.TRUE.equals(dto.getFaceRegistered()))
                    .build();
            studentRepository.save(newStudent);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("fullName", user.getFullName());
        return ResponseEntity.ok(ApiResponse.success(resp, "Cập nhật thông tin người dùng thành công"));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng có ID: " + id));

        // Xóa các dữ liệu phụ thuộc
        try {
            attendanceLogRepository.deleteByStudentUserId(id);
        } catch (Exception ignored) {}

        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        }

        userRepository.delete(user);
        log.info("Đã xóa người dùng: ID={}, Username={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", "Đã xóa tài khoản " + user.getUsername()));
    }

    // =========================================================================
    // 3. COURSES & CHAPTERS (BUỔI HỌC) & LESSONS & CLOUDINARY MEDIA
    // =========================================================================

    /**
     * Lấy toàn bộ danh sách khóa học kèm các buổi học, bài học, video Cloudinary và tài liệu
     * KHÔNG GÂY LỖI 500 do Jackson proxy
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourses() {
        List<Course> courses = courseRepository.findAll();

        // Tự động khởi tạo dữ liệu mẫu nếu chưa có khóa học nào
        if (courses.isEmpty()) {
            seedSampleCoursesAndMedia();
            courses = courseRepository.findAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Course c : courses) {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", c.getId());
            cMap.put("title", c.getTitle());
            cMap.put("slug", c.getSlug());
            cMap.put("description", c.getDescription() != null ? c.getDescription() : "");
            cMap.put("thumbnailUrl", c.getThumbnailUrl() != null ? c.getThumbnailUrl() : "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80");
            cMap.put("price", c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO);
            cMap.put("isFree", c.getIsFree());
            cMap.put("status", c.getStatus().name());
            cMap.put("enrollmentType", c.getEnrollmentType().name());
            cMap.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");

            // Lấy danh sách các buổi học (Chapters)
            List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(c.getId());

            // Tự động tạo buổi học mẫu nếu khóa học chưa có
            if (chapters.isEmpty()) {
                CourseChapter ch1 = CourseChapter.builder()
                        .course(c)
                        .title("Buổi 1: Tổng quan & Thiết lập môi trường dự án")
                        .orderIndex(1)
                        .build();
                CourseChapter ch2 = CourseChapter.builder()
                        .course(c)
                        .title("Buổi 2: Kiến trúc Cốt lõi & Tích hợp Cloudinary CDN")
                        .orderIndex(2)
                        .build();
                chapters = chapterRepository.saveAll(List.of(ch1, ch2));
            }

            List<Map<String, Object>> chaptersList = new ArrayList<>();
            int totalLessonsCount = 0;

            for (CourseChapter ch : chapters) {
                Map<String, Object> chMap = new HashMap<>();
                chMap.put("id", ch.getId());
                chMap.put("title", ch.getTitle());
                chMap.put("orderIndex", ch.getOrderIndex());

                // Lấy các bài học trong buổi
                List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(ch.getId());

                if (lessons.isEmpty()) {
                    Lesson l1 = Lesson.builder()
                            .chapter(ch)
                            .title("Bài học 1: Video Bài giảng lý thuyết (Cloudinary H.264)")
                            .lessonType(Lesson.LessonType.VIDEO)
                            .orderIndex(1)
                            .durationSeconds(1200)
                            .isPreview(true)
                            .build();
                    lessons = lessonRepository.saveAll(List.of(l1));

                    // Tạo video asset trên Cloudinary
                    VideoAsset va = VideoAsset.builder()
                            .lesson(l1)
                            .originalFileName("video_bai_giang_" + ch.getId() + ".mp4")
                            .storageProvider(VideoAsset.StorageProvider.CLOUDINARY)
                            .rawVideoUrl("https://res.cloudinary.com/dhefmthim/video/upload/v1728900000/lms_datn/videos/spring_boot_core.mp4")
                            .hlsPlaylistUrl("https://res.cloudinary.com/dhefmthim/video/upload/sp_auto/v1728900000/lms_datn/videos/spring_boot_core.m3u8")
                            .thumbnailUrl("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400&auto=format&fit=crop&q=80")
                            .durationSeconds(1200)
                            .resolution("1080p Full HD")
                            .fileSizeBytes(45800000L)
                            .encodeStatus(VideoAsset.EncodeStatus.READY)
                            .build();
                    videoAssetRepository.save(va);
                }

                totalLessonsCount += lessons.size();

                List<Map<String, Object>> lessonsList = new ArrayList<>();
                for (Lesson l : lessons) {
                    Map<String, Object> lMap = new HashMap<>();
                    lMap.put("id", l.getId());
                    lMap.put("title", l.getTitle());
                    lMap.put("lessonType", l.getLessonType().name());
                    lMap.put("durationSeconds", l.getDurationSeconds());
                    lMap.put("isPreview", l.getIsPreview());

                    // Video Cloudinary
                    Optional<VideoAsset> vOpt = videoAssetRepository.findByLessonId(l.getId());
                    if (vOpt.isPresent()) {
                        VideoAsset v = vOpt.get();
                        Map<String, Object> vMap = new HashMap<>();
                        vMap.put("id", v.getId());
                        vMap.put("fileName", v.getOriginalFileName());
                        vMap.put("provider", v.getStorageProvider().name());
                        vMap.put("videoUrl", v.getRawVideoUrl());
                        vMap.put("hlsUrl", v.getHlsPlaylistUrl());
                        vMap.put("thumbnailUrl", v.getThumbnailUrl());
                        vMap.put("durationSeconds", v.getDurationSeconds());
                        vMap.put("resolution", v.getResolution());
                        vMap.put("encodeStatus", v.getEncodeStatus().name());
                        lMap.put("videoAsset", vMap);
                    } else {
                        lMap.put("videoAsset", null);
                    }

                    lessonsList.add(lMap);
                }

                chMap.put("lessons", lessonsList);
                chaptersList.add(chMap);
            }

            // Tài liệu khóa học trên Cloudinary
            List<CourseDocument> docs = documentRepository.findByCourseId(c.getId());
            if (docs.isEmpty()) {
                CourseDocument doc1 = CourseDocument.builder()
                        .course(c)
                        .title("Giáo trình thực hành " + c.getTitle() + ".pdf")
                        .fileType("PDF")
                        .fileUrl("https://res.cloudinary.com/dhefmthim/raw/upload/v1728900000/lms_datn/docs/Giao_Trinh_LMS.pdf")
                        .fileSizeBytes(15420000L)
                        .ragIndexed(true)
                        .build();
                docs = documentRepository.saveAll(List.of(doc1));
            }

            List<Map<String, Object>> docsList = new ArrayList<>();
            for (CourseDocument d : docs) {
                Map<String, Object> dMap = new HashMap<>();
                dMap.put("id", d.getId());
                dMap.put("title", d.getTitle());
                dMap.put("fileType", d.getFileType());
                dMap.put("fileUrl", d.getFileUrl());
                dMap.put("fileSizeBytes", d.getFileSizeBytes());
                dMap.put("ragIndexed", d.getRagIndexed());
                docsList.add(dMap);
            }

            cMap.put("chaptersCount", chapters.size());
            cMap.put("lessonsCount", totalLessonsCount);
            cMap.put("chapters", chaptersList);
            cMap.put("documents", docsList);

            result.add(cMap);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách khóa học và buổi học thành công"));
    }

    private void seedSampleCoursesAndMedia() {
        try {
            Subject subject = Subject.builder()
                    .name("Kỹ thuật Phần mềm & AI")
                    .subjectCode("SE-AI-2026")
                    .credits(4)
                    .build();
            subject = subjectRepository.save(subject);

            Optional<Lecturer> lecturerOpt = lecturerRepository.findAll().stream().findFirst();
            Lecturer lecturer = lecturerOpt.orElse(null);
            if (lecturer == null && !userRepository.findAll().isEmpty()) {
                User lecUser = userRepository.findByUsername("giangvien_tung")
                        .orElseGet(() -> userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.LECTURER).findFirst()
                        .orElse(userRepository.findAll().get(0)));
                lecturer = Lecturer.builder()
                        .user(lecUser)
                        .lecturerCode("GV" + lecUser.getId())
                        .facultyId(1L)
                        .academicDegree("ThS. Giảng viên")
                        .bio("Chuyên gia hệ thống phân tán & thị giác máy tính")
                        .build();
                lecturer = lecturerRepository.save(lecturer);
            }

            Course c1 = Course.builder()
                    .title("Lập trình Java Nâng cao & Spring Boot 3 Chuyên Sâu")
                    .slug("lap-trinh-spring-boot-3-chuyen-sau")
                    .description("Xây dựng kiến trúc Microservices hiện đại, tối ưu hiệu năng với MySQL, MongoDB và tích hợp Cloudinary CDN.")
                    .thumbnailUrl("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80")
                    .price(new BigDecimal("799000"))
                    .isFree(false)
                    .status(Course.CourseStatus.PUBLISHED)
                    .subject(subject)
                    .lecturer(lecturer)
                    .build();

            Course c2 = Course.builder()
                    .title("Thị giác Máy tính Face AI & Nhận diện Khuôn mặt ArcFace 512D")
                    .slug("thi-giac-may-tinh-face-ai-arcface")
                    .description("Ứng dụng OpenCV YuNet ONNX phát hiện góc mặt và ArcFace ResNet100 đối soát sinh trắc học điểm danh chống gian lận.")
                    .thumbnailUrl("https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=600&auto=format&fit=crop&q=80")
                    .price(new BigDecimal("990000"))
                    .isFree(false)
                    .status(Course.CourseStatus.PUBLISHED)
                    .subject(subject)
                    .lecturer(lecturer)
                    .build();

            courseRepository.saveAll(List.of(c1, c2));
        } catch (Exception e) {
            log.warn("Không thể tự động seed dữ liệu khóa học: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 4. STATS, VIDEOS & LOGS
    // =========================================================================

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalStudents = studentRepository.count();
        long faceRegisteredCount = studentRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getFaceRegistered()))
                .count();

        long totalCourses = courseRepository.count();
        long totalVideos = videoAssetRepository.count();
        long totalMongoDocs = 0;
        try {
            totalMongoDocs = mongoDocumentRepository.count();
        } catch (Exception ignored) {}

        boolean faceAiOnline = false;
        try {
            ResponseEntity<String> res = restTemplate.getForEntity("http://localhost:8001/health", String.class);
            faceAiOnline = res.getStatusCode().is2xxSuccessful();
        } catch (Exception ignored) {}

        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("faceRegisteredCount", faceRegisteredCount);
        stats.put("totalCourses", totalCourses);
        stats.put("totalVideos", totalVideos);
        stats.put("totalMongoDocs", totalMongoDocs);
        stats.put("faceAiOnline", faceAiOnline);
        stats.put("serverTime", LocalDateTime.now().toString());

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê hệ thống thành công"));
    }

    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<List<VideoAsset>>> getVideos() {
        List<VideoAsset> list = videoAssetRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách video thành công"));
    }

    @GetMapping("/attendance-logs")
    public ResponseEntity<ApiResponse<List<FaceAttendanceLog>>> getAttendanceLogs() {
        List<FaceAttendanceLog> list = attendanceLogRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy nhật ký điểm danh khuôn mặt thành công"));
    }
}
