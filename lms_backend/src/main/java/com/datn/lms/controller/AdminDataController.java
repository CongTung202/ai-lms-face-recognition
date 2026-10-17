package com.datn.lms.controller;

import com.datn.lms.dto.*;
import com.datn.lms.entity.mysql.*;
import com.datn.lms.repository.mongodb.CourseDocumentMongoRepository;
import com.datn.lms.repository.mysql.*;
import com.datn.lms.service.VideoStreamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final CourseMajorRepository courseMajorRepository;
    private final ClassEntityRepository classEntityRepository;
    private final VideoStreamingService videoStreamingService;
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
            LecturerRepository lecturerRepository,
            FacultyRepository facultyRepository,
            MajorRepository majorRepository,
            CourseMajorRepository courseMajorRepository,
            ClassEntityRepository classEntityRepository,
            VideoStreamingService videoStreamingService) {
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
        this.facultyRepository = facultyRepository;
        this.majorRepository = majorRepository;
        this.courseMajorRepository = courseMajorRepository;
        this.classEntityRepository = classEntityRepository;
        this.videoStreamingService = videoStreamingService;
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
            result.add(buildCourseDetailMap(c));
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách khóa học và buổi học thành công"));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseById(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có ID: " + id));
        return ResponseEntity.ok(ApiResponse.success(buildCourseDetailMap(course), "Lấy chi tiết khóa học thành công"));
    }

    private Map<String, Object> buildCourseDetailMap(Course c) {
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

        // Lấy danh sách các buổi học (Chapters) theo thứ tự
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(c.getId());

        List<Map<String, Object>> chaptersList = new ArrayList<>();
        int totalLessonsCount = 0;

        for (CourseChapter ch : chapters) {
            Map<String, Object> chMap = new HashMap<>();
            chMap.put("id", ch.getId());
            chMap.put("courseId", c.getId());
            chMap.put("title", ch.getTitle());
            chMap.put("orderIndex", ch.getOrderIndex());

            // Lấy các bài học trong buổi
            List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(ch.getId());
            totalLessonsCount += lessons.size();

            List<Map<String, Object>> lessonsList = new ArrayList<>();
            for (Lesson l : lessons) {
                Map<String, Object> lMap = new HashMap<>();
                lMap.put("id", l.getId());
                lMap.put("chapterId", ch.getId());
                lMap.put("title", l.getTitle());
                lMap.put("lessonType", l.getLessonType().name());
                lMap.put("durationSeconds", l.getDurationSeconds());
                lMap.put("isPreview", l.getIsPreview());
                lMap.put("orderIndex", l.getOrderIndex());

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

        // Phân bổ Chuyên ngành đào tạo (Course Majors)
        List<CourseMajor> cms = courseMajorRepository.findByCourseId(c.getId());
        if (cms.isEmpty()) {
            ensureDefaultFacultiesAndClassesSeeded();
            String titleLower = (c.getTitle() != null ? c.getTitle() : "").toLowerCase();
            String slugLower = (c.getSlug() != null ? c.getSlug() : "").toLowerCase();
            if (titleLower.contains("ai") || titleLower.contains("fastapi") || slugLower.contains("fastapi") || titleLower.contains("th?")) {
                majorRepository.findByMajorCode("AI").ifPresent(m -> courseMajorRepository.save(CourseMajor.builder().course(c).major(m).build()));
                majorRepository.findByMajorCode("CNTT").ifPresent(m -> courseMajorRepository.save(CourseMajor.builder().course(c).major(m).build()));
            } else {
                majorRepository.findByMajorCode("SE").ifPresent(m -> courseMajorRepository.save(CourseMajor.builder().course(c).major(m).build()));
                majorRepository.findByMajorCode("CNTT").ifPresent(m -> courseMajorRepository.save(CourseMajor.builder().course(c).major(m).build()));
            }
            cms = courseMajorRepository.findByCourseId(c.getId());
        }

        List<Map<String, Object>> majorsList = new ArrayList<>();
        List<Long> majorIds = new ArrayList<>();
        for (CourseMajor cm : cms) {
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("id", cm.getMajor().getId());
            mMap.put("majorCode", cm.getMajor().getMajorCode());
            mMap.put("name", cm.getMajor().getName());
            mMap.put("isMandatory", cm.getIsMandatory());
            majorsList.add(mMap);
            majorIds.add(cm.getMajor().getId());
        }

        cMap.put("chaptersCount", chapters.size());
        cMap.put("lessonsCount", totalLessonsCount);
        cMap.put("chapters", chaptersList);
        cMap.put("documents", docsList);
        cMap.put("majors", majorsList);
        cMap.put("majorIds", majorIds);

        return cMap;
    }

    // --- 3.1. CRUD KHÓA HỌC (COURSES) ---

    @GetMapping("/majors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllMajors() {
        ensureDefaultFacultiesAndClassesSeeded();
        List<Major> majors = majorRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Major m : majors) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("majorCode", m.getMajorCode());
            item.put("name", m.getName());
            item.put("facultyId", m.getFaculty() != null ? m.getFaculty().getId() : null);
            item.put("facultyCode", m.getFaculty() != null ? m.getFaculty().getFacultyCode() : "");
            item.put("facultyName", m.getFaculty() != null ? m.getFaculty().getName() : "");
            list.add(item);
        }
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách chuyên ngành thành công"));
    }

    // =========================================================================
    // 3.5. CRUD KHOA ĐÀO TẠO (FACULTIES)
    // =========================================================================

    @GetMapping("/faculties")
    public ResponseEntity<ApiResponse<List<FacultyDto>>> getAllFaculties() {
        ensureDefaultFacultiesAndClassesSeeded();
        List<Faculty> faculties = facultyRepository.findAll();
        List<FacultyDto> list = new ArrayList<>();
        for (Faculty f : faculties) {
            list.add(buildFacultyDto(f));
        }
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách Khoa đào tạo thành công"));
    }

    @GetMapping("/faculties/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFacultyDetail(@PathVariable("id") Long id) {
        Faculty faculty = facultyRepository.findById(id).orElse(null);
        if (faculty == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Khoa"));
        }
        FacultyDto fd = buildFacultyDto(faculty);
        Map<String, Object> map = new HashMap<>();
        map.put("id", faculty.getId());
        map.put("facultyCode", faculty.getFacultyCode());
        map.put("name", faculty.getName());
        map.put("facultyName", faculty.getName());
        map.put("description", faculty.getDescription());
        map.put("majors", faculty.getMajors());
        map.put("majorList", fd.getMajorList());
        map.put("majorsCount", fd.getMajorsCount());
        map.put("classesCount", fd.getClassesCount());
        map.put("createdAt", faculty.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success(map, "Lấy chi tiết Khoa thành công"));
    }

    private FacultyDto buildFacultyDto(Faculty f) {
        String majorsStr = f.getMajors();
        List<String> majorList = new ArrayList<>();
        if (majorsStr != null && !majorsStr.isBlank()) {
            for (String m : majorsStr.split(",")) {
                if (!m.trim().isEmpty()) {
                    majorList.add(m.trim());
                }
            }
        } else {
            List<Major> fMajors = majorRepository.findByFacultyId(f.getId());
            for (Major m : fMajors) {
                majorList.add(m.getName());
            }
            if (!majorList.isEmpty()) {
                majorsStr = String.join(", ", majorList);
                f.setMajors(majorsStr);
                facultyRepository.save(f);
            }
        }

        int classesCount = 0;
        try {
            classesCount = classEntityRepository.findByFacultyId(f.getId()).size();
        } catch (Exception ignored) {
            List<Major> fMajors = majorRepository.findByFacultyId(f.getId());
            for (Major m : fMajors) {
                classesCount += classEntityRepository.findByMajorId(m.getId()).size();
            }
        }

        return FacultyDto.builder()
                .id(f.getId())
                .facultyCode(f.getFacultyCode())
                .name(f.getName())
                .facultyName(f.getName())
                .description(f.getDescription())
                .majors(majorsStr)
                .majorList(majorList)
                .majorsCount(majorList.size())
                .classesCount(classesCount)
                .createdAt(f.getCreatedAt())
                .build();
    }

    private ClassDto buildClassDto(ClassEntity ce) {
        Faculty f = ce.getFaculty();
        Major m = ce.getMajor();
        if (f == null && m != null) {
            f = m.getFaculty();
        }
        long stdCount = studentRepository.countByClassId(ce.getId());

        String majorName = ce.getMajorName();
        if ((majorName == null || majorName.isBlank()) && m != null) {
            majorName = m.getName();
        }

        return ClassDto.builder()
                .id(ce.getId())
                .classCode(ce.getClassCode())
                .name(ce.getName())
                .className(ce.getName())
                .academicYear(ce.getAcademicYear())
                .homeroomTeacher(ce.getHomeroomTeacher())
                .majorId(m != null ? m.getId() : null)
                .majorCode(m != null ? m.getMajorCode() : "")
                .majorName(majorName != null ? majorName : "")
                .facultyId(f != null ? f.getId() : null)
                .facultyCode(f != null ? f.getFacultyCode() : "")
                .facultyName(f != null ? f.getName() : "")
                .studentsCount((int) stdCount)
                .totalStudents((int) stdCount)
                .createdAt(ce.getCreatedAt())
                .build();
    }

    @PostMapping("/faculties")
    @Transactional
    public ResponseEntity<ApiResponse<FacultyDto>> createFaculty(@RequestBody FacultyDto dto) {
        String code = dto.getFacultyCode() != null ? dto.getFacultyCode().trim().toUpperCase() : "";
        String name = dto.getFacultyName() != null && !dto.getFacultyName().isBlank()
                ? dto.getFacultyName().trim()
                : (dto.getName() != null ? dto.getName().trim() : "");

        if (code.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Khoa không được để trống"));
        }
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên Khoa không được để trống"));
        }
        if (facultyRepository.existsByFacultyCode(code)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Khoa '" + code + "' đã tồn tại!"));
        }

        String majorsStr = dto.getMajors() != null ? dto.getMajors().trim() : "";
        if (dto.getMajorList() != null && !dto.getMajorList().isEmpty()) {
            majorsStr = String.join(", ", dto.getMajorList());
        }

        Faculty f = Faculty.builder()
                .facultyCode(code)
                .name(name)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .majors(majorsStr)
                .build();
        f = facultyRepository.save(f);
        return ResponseEntity.ok(ApiResponse.success(buildFacultyDto(f), "Tạo Khoa mới thành công"));
    }

    @PutMapping("/faculties/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<FacultyDto>> updateFaculty(
            @PathVariable("id") Long id,
            @RequestBody FacultyDto dto) {
        Faculty f = facultyRepository.findById(id).orElse(null);
        if (f == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Khoa"));
        }
        String name = dto.getFacultyName() != null && !dto.getFacultyName().isBlank()
                ? dto.getFacultyName().trim()
                : (dto.getName() != null ? dto.getName().trim() : null);
        if (name != null && !name.isBlank()) {
            f.setName(name);
        }
        if (dto.getFacultyCode() != null && !dto.getFacultyCode().trim().isBlank()) {
            String newCode = dto.getFacultyCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(f.getFacultyCode()) && facultyRepository.existsByFacultyCode(newCode)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Khoa '" + newCode + "' đã tồn tại!"));
            }
            f.setFacultyCode(newCode);
        }
        if (dto.getDescription() != null) {
            f.setDescription(dto.getDescription().trim());
        }
        if (dto.getMajors() != null) {
            f.setMajors(dto.getMajors().trim());
        } else if (dto.getMajorList() != null) {
            f.setMajors(String.join(", ", dto.getMajorList()));
        }

        f = facultyRepository.save(f);
        return ResponseEntity.ok(ApiResponse.success(buildFacultyDto(f), "Cập nhật Khoa thành công"));
    }

    @DeleteMapping("/faculties/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteFaculty(@PathVariable("id") Long id) {
        Faculty f = facultyRepository.findById(id).orElse(null);
        if (f == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Khoa"));
        }
        long majorsCount = majorRepository.countByFacultyId(id);
        if (majorsCount > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không thể xóa Khoa này vì đang có " + majorsCount + " chuyên ngành trực thuộc!"));
        }
        facultyRepository.delete(f);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa Khoa thành công"));
    }

    // =========================================================================
    // 3.6. CRUD LỚP HỌC (CLASSES)
    // =========================================================================

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<ClassDto>>> getAllClasses(
            @RequestParam(value = "facultyId", required = false) Long facultyId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "search", required = false) String search) {

        ensureDefaultFacultiesAndClassesSeeded();
        List<ClassEntity> classes = classEntityRepository.findAll();
        List<ClassDto> list = new ArrayList<>();

        for (ClassEntity ce : classes) {
            Major m = ce.getMajor();
            Faculty f = m != null ? m.getFaculty() : null;

            if (facultyId != null && (f == null || !f.getId().equals(facultyId))) {
                continue;
            }
            if (majorId != null && (m == null || !m.getId().equals(majorId))) {
                continue;
            }
            if (academicYear != null && !academicYear.isBlank() && !academicYear.equalsIgnoreCase("ALL")
                    && !academicYear.equalsIgnoreCase(ce.getAcademicYear())) {
                continue;
            }
            if (search != null && !search.trim().isBlank()) {
                String q = search.trim().toLowerCase();
                boolean matchCode = ce.getClassCode() != null && ce.getClassCode().toLowerCase().contains(q);
                boolean matchName = ce.getName() != null && ce.getName().toLowerCase().contains(q);
                if (!matchCode && !matchName) continue;
            }

            list.add(buildClassDto(ce));
        }
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách Lớp học thành công"));
    }

    @GetMapping("/classes/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> getClassDetail(@PathVariable("id") Long id) {
        ClassEntity ce = classEntityRepository.findById(id).orElse(null);
        if (ce == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Lớp học"));
        }
        return ResponseEntity.ok(ApiResponse.success(buildClassDto(ce), "Lấy chi tiết Lớp học thành công"));
    }

    @PostMapping("/classes")
    @Transactional
    public ResponseEntity<ApiResponse<ClassDto>> createClass(@RequestBody ClassDto dto) {
        String classCode = dto.getClassCode() != null ? dto.getClassCode().trim().toUpperCase() : "";
        String className = dto.getClassName() != null && !dto.getClassName().isBlank()
                ? dto.getClassName().trim()
                : (dto.getName() != null ? dto.getName().trim() : "");

        if (classCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Lớp không được để trống"));
        }
        if (className.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên Lớp không được để trống"));
        }
        if (classEntityRepository.existsByClassCode(classCode)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Lớp '" + classCode + "' đã tồn tại!"));
        }

        Faculty faculty = null;
        if (dto.getFacultyId() != null) {
            faculty = facultyRepository.findById(dto.getFacultyId()).orElse(null);
        }

        Major major = null;
        if (dto.getMajorId() != null) {
            major = majorRepository.findById(dto.getMajorId()).orElse(null);
            if (faculty == null && major != null) {
                faculty = major.getFaculty();
            }
        }

        ClassEntity ce = ClassEntity.builder()
                .classCode(classCode)
                .name(className)
                .faculty(faculty)
                .major(major)
                .majorName(dto.getMajorName() != null ? dto.getMajorName().trim() : (major != null ? major.getName() : ""))
                .academicYear(dto.getAcademicYear() != null && !dto.getAcademicYear().isBlank() ? dto.getAcademicYear().trim() : "2023-2027")
                .homeroomTeacher(dto.getHomeroomTeacher() != null ? dto.getHomeroomTeacher().trim() : "")
                .build();

        ce = classEntityRepository.save(ce);
        return ResponseEntity.ok(ApiResponse.success(buildClassDto(ce), "Tạo Lớp học mới thành công"));
    }

    @PutMapping("/classes/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<ClassDto>> updateClass(
            @PathVariable("id") Long id,
            @RequestBody ClassDto dto) {
        ClassEntity ce = classEntityRepository.findById(id).orElse(null);
        if (ce == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Lớp học"));
        }

        if (dto.getClassCode() != null && !dto.getClassCode().trim().isBlank()) {
            String newCode = dto.getClassCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(ce.getClassCode()) && classEntityRepository.existsByClassCode(newCode)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã Lớp '" + newCode + "' đã tồn tại!"));
            }
            ce.setClassCode(newCode);
        }
        String className = dto.getClassName() != null && !dto.getClassName().isBlank()
                ? dto.getClassName().trim()
                : (dto.getName() != null ? dto.getName().trim() : null);
        if (className != null && !className.isBlank()) {
            ce.setName(className);
        }
        if (dto.getAcademicYear() != null && !dto.getAcademicYear().trim().isBlank()) {
            ce.setAcademicYear(dto.getAcademicYear().trim());
        }
        if (dto.getHomeroomTeacher() != null) {
            ce.setHomeroomTeacher(dto.getHomeroomTeacher().trim());
        }
        if (dto.getMajorName() != null) {
            ce.setMajorName(dto.getMajorName().trim());
        }
        if (dto.getFacultyId() != null) {
            Faculty faculty = facultyRepository.findById(dto.getFacultyId()).orElse(null);
            if (faculty != null) {
                ce.setFaculty(faculty);
            }
        }
        if (dto.getMajorId() != null) {
            Major major = majorRepository.findById(dto.getMajorId()).orElse(null);
            if (major != null) {
                ce.setMajor(major);
            }
        }

        ce = classEntityRepository.save(ce);
        return ResponseEntity.ok(ApiResponse.success(buildClassDto(ce), "Cập nhật Lớp học thành công"));
    }

    @DeleteMapping("/classes/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable("id") Long id) {
        ClassEntity ce = classEntityRepository.findById(id).orElse(null);
        if (ce == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Lớp học"));
        }
        long stdCount = studentRepository.countByClassId(id);
        if (stdCount > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không thể xóa Lớp học này vì đang có " + stdCount + " sinh viên thuộc lớp!"));
        }
        classEntityRepository.delete(ce);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa Lớp học thành công"));
    }

    // =========================================================================
    // 3.7. QUẢN LÝ SINH VIÊN TRONG LỚP HỌC (CLASS STUDENTS)
    // =========================================================================

    @GetMapping("/classes/{id}/students")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassStudents(@PathVariable("id") Long id) {
        ClassEntity ce = classEntityRepository.findById(id).orElse(null);
        if (ce == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Lớp học"));
        }
        List<Student> students = studentRepository.findByClassId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            User u = s.getUser();
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getUserId());
            m.put("userId", s.getUserId());
            m.put("studentCode", s.getStudentCode());
            m.put("fullName", u != null ? u.getFullName() : "");
            m.put("username", u != null ? u.getUsername() : "");
            m.put("email", u != null ? u.getEmail() : "");
            m.put("phone", u != null ? u.getPhone() : "");
            m.put("phoneNumber", u != null ? u.getPhone() : "");
            m.put("avatarUrl", u != null ? u.getAvatarUrl() : "");
            m.put("gender", s.getGender() != null ? s.getGender().name() : "MALE");
            m.put("faceRegistered", s.getFaceRegistered() != null && s.getFaceRegistered());
            m.put("faceRegisteredAt", s.getFaceRegisteredAt());
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách sinh viên của lớp thành công"));
    }

    @GetMapping("/classes/{id}/available-students")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableStudentsForClass(@PathVariable("id") Long id) {
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        List<Student> currentInClass = studentRepository.findByClassId(id);
        Set<Long> inClassUserIds = currentInClass.stream().map(Student::getUserId).collect(java.util.stream.Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : studentUsers) {
            if (inClassUserIds.contains(u.getId())) continue;

            Student s = studentRepository.findById(u.getId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("userId", u.getId());
            m.put("studentCode", s != null ? s.getStudentCode() : "");
            m.put("fullName", u.getFullName());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("phoneNumber", u.getPhone());
            m.put("currentClassId", s != null ? s.getClassId() : null);
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách sinh viên sẵn sàng thêm vào lớp thành công"));
    }

    @PostMapping("/classes/{id}/students")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> addStudentToClass(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> payload) {
        ClassEntity ce = classEntityRepository.findById(id).orElse(null);
        if (ce == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy Lớp học"));
        }

        Long userId = null;
        if (payload.get("userId") != null) {
            userId = Long.valueOf(payload.get("userId").toString());
        } else if (payload.get("studentId") != null) {
            userId = Long.valueOf(payload.get("studentId").toString());
        }

        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Vui lòng chọn sinh viên cần thêm vào lớp"));
        }

        User u = userRepository.findById(userId).orElse(null);
        if (u == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không tìm thấy tài khoản người dùng"));
        }

        Student s = studentRepository.findById(userId).orElseGet(() -> {
            Student newS = Student.builder()
                    .user(u)
                    .userId(u.getId())
                    .studentCode("SV" + (u.getId() < 100 ? "2026" + String.format("%03d", u.getId()) : u.getId()))
                    .classId(id)
                    .facultyId(ce.getFaculty() != null ? ce.getFaculty().getId() : null)
                    .faceRegistered(false)
                    .build();
            return studentRepository.save(newS);
        });

        s.setClassId(id);
        if (ce.getFaculty() != null) {
            s.setFacultyId(ce.getFaculty().getId());
        }
        studentRepository.save(s);

        Map<String, Object> res = new HashMap<>();
        res.put("classId", id);
        res.put("userId", userId);
        res.put("studentCode", s.getStudentCode());
        res.put("fullName", u.getFullName());
        return ResponseEntity.ok(ApiResponse.success(res, "Đã thêm sinh viên vào lớp thành công"));
    }

    @DeleteMapping("/classes/{id}/students/{studentId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeStudentFromClass(
            @PathVariable("id") Long id,
            @PathVariable("studentId") Long studentId) {
        Student s = studentRepository.findById(studentId).orElse(null);
        if (s == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy hồ sơ sinh viên"));
        }
        if (s.getClassId() != null && s.getClassId().equals(id)) {
            s.setClassId(null);
            studentRepository.save(s);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa sinh viên khỏi lớp thành công"));
    }

    private void ensureDefaultFacultiesAndClassesSeeded() {
        Faculty facultyFit = facultyRepository.findByFacultyCode("CNTT").orElseGet(() ->
                facultyRepository.findByFacultyCode("FIT").orElseGet(() ->
                        facultyRepository.save(Faculty.builder()
                                .facultyCode("CNTT")
                                .name("Khoa Công nghệ thông tin")
                                .description("Khoa Đào tạo Kỹ sư & Cử nhân CNTT, Trí tuệ nhân tạo, Kỹ thuật phần mềm")
                                .majors("Kỹ thuật phần mềm, Trí tuệ nhân tạo, Khoa học máy tính, An toàn thông tin")
                                .build())));

        if (facultyFit.getMajors() == null || facultyFit.getMajors().isBlank()) {
            facultyFit.setMajors("Kỹ thuật phần mềm, Trí tuệ nhân tạo, Khoa học máy tính, An toàn thông tin");
            facultyRepository.save(facultyFit);
        }

        Faculty facultyDtvt = facultyRepository.findByFacultyCode("DTVT").orElseGet(() ->
                facultyRepository.save(Faculty.builder()
                        .facultyCode("DTVT")
                        .name("Khoa Điện tử - Viễn thông & IoT")
                        .description("Đào tạo Kỹ sư Thiết kế vi mạch, Hệ thống nhúng và IoT")
                        .majors("Kỹ thuật Điện tử - Viễn thông, Công nghệ IoT, Hệ thống nhúng")
                        .build()));

        if (facultyDtvt.getMajors() == null || facultyDtvt.getMajors().isBlank()) {
            facultyDtvt.setMajors("Kỹ thuật Điện tử - Viễn thông, Công nghệ IoT, Hệ thống nhúng");
            facultyRepository.save(facultyDtvt);
        }

        Faculty facultyKkt = facultyRepository.findByFacultyCode("KKT").orElseGet(() ->
                facultyRepository.save(Faculty.builder()
                        .facultyCode("KKT")
                        .name("Khoa Kinh tế & Quản trị kinh doanh")
                        .description("Đào tạo Cử nhân Tài chính, Kế toán và Thương mại điện tử")
                        .majors("Quản trị kinh doanh, Tài chính ngân hàng, Kế toán số, Thương mại điện tử")
                        .build()));

        if (facultyKkt.getMajors() == null || facultyKkt.getMajors().isBlank()) {
            facultyKkt.setMajors("Quản trị kinh doanh, Tài chính ngân hàng, Kế toán số, Thương mại điện tử");
            facultyRepository.save(facultyKkt);
        }

        Map<String, String> defaultMajors = new LinkedHashMap<>();
        defaultMajors.put("CNTT", "Công nghệ thông tin");
        defaultMajors.put("AI", "Trí tuệ nhân tạo & Khoa học dữ liệu");
        defaultMajors.put("SE", "Kỹ thuật phần mềm");
        defaultMajors.put("ATTT", "An toàn thông tin");
        defaultMajors.put("KHMT", "Khoa học máy tính");
        defaultMajors.put("IOT", "Internet vạn vật & Hệ thống nhúng");

        for (Map.Entry<String, String> entry : defaultMajors.entrySet()) {
            if (majorRepository.findByMajorCode(entry.getKey()).isEmpty()) {
                majorRepository.save(Major.builder()
                        .faculty(facultyFit)
                        .majorCode(entry.getKey())
                        .name(entry.getValue())
                        .build());
            }
        }

        // Seed default Classes if empty
        if (classEntityRepository.count() == 0) {
            Major mCntt = majorRepository.findByMajorCode("CNTT").orElse(null);
            Major mSe = majorRepository.findByMajorCode("SE").orElse(null);
            Major mAi = majorRepository.findByMajorCode("AI").orElse(null);
            Major mIot = majorRepository.findByMajorCode("IOT").orElse(null);
            Major mAttt = majorRepository.findByMajorCode("ATTT").orElse(null);

            if (mCntt != null) {
                classEntityRepository.save(ClassEntity.builder()
                        .classCode("CNTT-K18A")
                        .name("Lớp Công nghệ thông tin K18A")
                        .major(mCntt)
                        .academicYear("2022-2026")
                        .build());
            }
            if (mSe != null) {
                classEntityRepository.save(ClassEntity.builder()
                        .classCode("SE-K19A")
                        .name("Lớp Kỹ thuật Phần mềm K19A")
                        .major(mSe)
                        .academicYear("2023-2027")
                        .build());
            }
            if (mAi != null) {
                classEntityRepository.save(ClassEntity.builder()
                        .classCode("AI-K20A")
                        .name("Lớp Trí tuệ Nhân tạo K20A")
                        .major(mAi)
                        .academicYear("2024-2028")
                        .build());
            }
            if (mIot != null) {
                classEntityRepository.save(ClassEntity.builder()
                        .classCode("IOT-K19B")
                        .name("Lớp Internet vạn vật K19B")
                        .major(mIot)
                        .academicYear("2023-2027")
                        .build());
            }
            if (mAttt != null) {
                classEntityRepository.save(ClassEntity.builder()
                        .classCode("ATTT-K18B")
                        .name("Lớp An toàn Thông tin K18B")
                        .major(mAttt)
                        .academicYear("2022-2026")
                        .build());
            }
        }
    }

    @PostMapping("/courses")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCourse(@RequestBody CourseDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tiêu đề khóa học không được để trống"));
        }

        Subject subject = null;
        if (dto.getSubjectId() != null) {
            subject = subjectRepository.findById(dto.getSubjectId()).orElse(null);
        }
        if (subject == null) {
            subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                    subjectRepository.save(Subject.builder().name("Kỹ thuật Phần mềm & AI").subjectCode("SE-AI-" + System.currentTimeMillis() % 1000).credits(4).build()));
        }

        Lecturer lecturer = null;
        if (dto.getLecturerId() != null) {
            lecturer = lecturerRepository.findById(dto.getLecturerId()).orElse(null);
        }
        if (lecturer == null) {
            lecturer = lecturerRepository.findAll().stream().findFirst().orElse(null);
        }

        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug().trim()
                : generateSlug(dto.getTitle());

        Course course = Course.builder()
                .title(dto.getTitle().trim())
                .slug(slug)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .thumbnailUrl(dto.getThumbnailUrl() != null && !dto.getThumbnailUrl().isBlank()
                        ? dto.getThumbnailUrl().trim()
                        : "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80")
                .price(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO)
                .isFree(Boolean.TRUE.equals(dto.getIsFree()) || (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) == 0))
                .status(dto.getStatus() != null ? Course.CourseStatus.valueOf(dto.getStatus().toUpperCase()) : Course.CourseStatus.PUBLISHED)
                .enrollmentType(dto.getEnrollmentType() != null ? Course.EnrollmentType.valueOf(dto.getEnrollmentType().toUpperCase()) : Course.EnrollmentType.ASSIGNED_BY_MAJOR)
                .subject(subject)
                .lecturer(lecturer)
                .build();

        course = courseRepository.save(course);

        // Lưu phân bổ chuyên ngành nếu được chọn
        final Course savedCourse = course;
        if (dto.getMajorIds() != null && !dto.getMajorIds().isEmpty()) {
            for (Long mId : dto.getMajorIds()) {
                majorRepository.findById(mId).ifPresent(m -> {
                    courseMajorRepository.save(CourseMajor.builder()
                            .course(savedCourse)
                            .major(m)
                            .isMandatory(true)
                            .build());
                });
            }
        }

        // Tự động tạo Buổi 1 mặc định
        CourseChapter defaultChapter = CourseChapter.builder()
                .course(course)
                .title("Buổi 1: Giới thiệu & Định hướng bài giảng")
                .orderIndex(1)
                .build();
        chapterRepository.save(defaultChapter);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", course.getId());
        resp.put("title", course.getTitle());
        resp.put("slug", course.getSlug());
        return ResponseEntity.ok(ApiResponse.success(resp, "Tạo mới khóa học thành công!"));
    }

    @PutMapping("/courses/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có ID: " + id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            course.setTitle(dto.getTitle().trim());
        }
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            course.setSlug(dto.getSlug().trim());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription().trim());
        }
        if (dto.getThumbnailUrl() != null && !dto.getThumbnailUrl().isBlank()) {
            course.setThumbnailUrl(dto.getThumbnailUrl().trim());
        }
        if (dto.getPrice() != null) {
            course.setPrice(dto.getPrice());
            course.setIsFree(dto.getPrice().compareTo(BigDecimal.ZERO) == 0);
        }
        if (dto.getIsFree() != null) {
            course.setIsFree(dto.getIsFree());
        }
        if (dto.getStatus() != null) {
            try {
                course.setStatus(Course.CourseStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (dto.getEnrollmentType() != null) {
            try {
                course.setEnrollmentType(Course.EnrollmentType.valueOf(dto.getEnrollmentType().toUpperCase()));
            } catch (Exception ignored) {}
        }

        course = courseRepository.save(course);

        // Cập nhật phân bổ chuyên ngành nếu có truyền majorIds
        if (dto.getMajorIds() != null) {
            final Course updatedCourse = course;
            courseMajorRepository.deleteByCourseId(id);
            for (Long mId : dto.getMajorIds()) {
                majorRepository.findById(mId).ifPresent(m -> {
                    courseMajorRepository.save(CourseMajor.builder()
                            .course(updatedCourse)
                            .major(m)
                            .isMandatory(true)
                            .build());
                });
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", course.getId());
        resp.put("title", course.getTitle());
        return ResponseEntity.ok(ApiResponse.success(resp, "Cập nhật khóa học thành công"));
    }

    @DeleteMapping("/courses/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có ID: " + id));

        // 1. Tìm các chapters và lessons để xóa video asset
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(id);
        for (CourseChapter ch : chapters) {
            List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(ch.getId());
            for (Lesson l : lessons) {
                videoAssetRepository.deleteByLessonId(l.getId());
            }
            lessonRepository.deleteByChapterId(ch.getId());
        }
        chapterRepository.deleteByCourseId(id);

        // 2. Xóa tài liệu gắn vào khóa học
        try {
            documentRepository.deleteByCourseId(id);
        } catch (Exception ignored) {}

        // 3. Xóa phân bổ ngành
        courseMajorRepository.deleteByCourseId(id);

        courseRepository.delete(course);
        return ResponseEntity.ok(ApiResponse.success("Xóa khóa học thành công", "Đã xóa toàn bộ khóa học " + course.getTitle()));
    }

    // --- 3.2. CRUD BUỔI HỌC / CHƯƠNG MỤC (CHAPTERS) ---

    @PostMapping("/chapters")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createChapter(@RequestBody ChapterDto dto) {
        if (dto.getCourseId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã khóa học không được để trống"));
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên buổi học không được để trống"));
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có ID: " + dto.getCourseId()));

        int orderIndex = dto.getOrderIndex() != null ? dto.getOrderIndex()
                : chapterRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).size() + 1;

        CourseChapter chapter = CourseChapter.builder()
                .course(course)
                .title(dto.getTitle().trim())
                .orderIndex(orderIndex)
                .build();

        chapter = chapterRepository.save(chapter);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", chapter.getId());
        resp.put("title", chapter.getTitle());
        resp.put("orderIndex", chapter.getOrderIndex());
        return ResponseEntity.ok(ApiResponse.success(resp, "Tạo buổi học mới thành công"));
    }

    @PutMapping("/chapters/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateChapter(
            @PathVariable Long id,
            @RequestBody ChapterDto dto) {
        CourseChapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học có ID: " + id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            chapter.setTitle(dto.getTitle().trim());
        }
        if (dto.getOrderIndex() != null) {
            chapter.setOrderIndex(dto.getOrderIndex());
        }

        chapter = chapterRepository.save(chapter);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", chapter.getId());
        resp.put("title", chapter.getTitle());
        return ResponseEntity.ok(ApiResponse.success(resp, "Cập nhật buổi học thành công"));
    }

    @DeleteMapping("/chapters/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteChapter(@PathVariable Long id) {
        CourseChapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học có ID: " + id));

        List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(id);
        for (Lesson l : lessons) {
            videoAssetRepository.deleteByLessonId(l.getId());
        }
        lessonRepository.deleteByChapterId(id);
        chapterRepository.delete(chapter);

        return ResponseEntity.ok(ApiResponse.success("Xóa buổi học thành công", "Đã xóa buổi học " + chapter.getTitle()));
    }

    @PutMapping("/chapters/reorder")
    @Transactional
    public ResponseEntity<ApiResponse<String>> reorderChapters(@RequestBody List<Map<String, Object>> reorderList) {
        if (reorderList != null) {
            for (Map<String, Object> item : reorderList) {
                if (item.get("id") != null && item.get("orderIndex") != null) {
                    Long chId = Long.valueOf(item.get("id").toString());
                    Integer order = Integer.valueOf(item.get("orderIndex").toString());
                    chapterRepository.findById(chId).ifPresent(ch -> {
                        ch.setOrderIndex(order);
                        chapterRepository.save(ch);
                    });
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success("OK", "Đã cập nhật thứ tự các buổi học thành công"));
    }

    // --- 3.3. CRUD BÀI HỌC (LESSONS) ---

    @PostMapping("/lessons")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLesson(@RequestBody LessonDto dto) {
        if (dto.getChapterId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Mã buổi học không được để trống"));
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Tên bài học không được để trống"));
        }

        CourseChapter chapter = chapterRepository.findById(dto.getChapterId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học có ID: " + dto.getChapterId()));

        int orderIndex = dto.getOrderIndex() != null ? dto.getOrderIndex()
                : lessonRepository.findByChapterIdOrderByOrderIndexAsc(chapter.getId()).size() + 1;

        Lesson.LessonType lessonType = Lesson.LessonType.VIDEO;
        if (dto.getLessonType() != null) {
            try {
                lessonType = Lesson.LessonType.valueOf(dto.getLessonType().toUpperCase());
            } catch (Exception ignored) {}
        }

        Lesson lesson = Lesson.builder()
                .chapter(chapter)
                .title(dto.getTitle().trim())
                .lessonType(lessonType)
                .durationSeconds(dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 0)
                .isPreview(Boolean.TRUE.equals(dto.getIsPreview()))
                .orderIndex(orderIndex)
                .build();

        lesson = lessonRepository.save(lesson);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", lesson.getId());
        resp.put("title", lesson.getTitle());
        resp.put("lessonType", lesson.getLessonType().name());
        return ResponseEntity.ok(ApiResponse.success(resp, "Tạo bài học mới thành công"));
    }

    @PutMapping("/lessons/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonDto dto) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học có ID: " + id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            lesson.setTitle(dto.getTitle().trim());
        }
        if (dto.getLessonType() != null) {
            try {
                lesson.setLessonType(Lesson.LessonType.valueOf(dto.getLessonType().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (dto.getDurationSeconds() != null) {
            lesson.setDurationSeconds(dto.getDurationSeconds());
        }
        if (dto.getIsPreview() != null) {
            lesson.setIsPreview(dto.getIsPreview());
        }
        if (dto.getOrderIndex() != null) {
            lesson.setOrderIndex(dto.getOrderIndex());
        }

        lesson = lessonRepository.save(lesson);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", lesson.getId());
        resp.put("title", lesson.getTitle());
        return ResponseEntity.ok(ApiResponse.success(resp, "Cập nhật bài học thành công"));
    }

    @DeleteMapping("/lessons/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteLesson(@PathVariable Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học có ID: " + id));

        videoAssetRepository.deleteByLessonId(id);
        lessonRepository.delete(lesson);

        return ResponseEntity.ok(ApiResponse.success("Xóa bài học thành công", "Đã xóa bài học " + lesson.getTitle()));
    }

    @PutMapping("/lessons/reorder")
    @Transactional
    public ResponseEntity<ApiResponse<String>> reorderLessons(@RequestBody List<Map<String, Object>> reorderList) {
        if (reorderList != null) {
            for (Map<String, Object> item : reorderList) {
                if (item.get("id") != null && item.get("orderIndex") != null) {
                    Long lId = Long.valueOf(item.get("id").toString());
                    Integer order = Integer.valueOf(item.get("orderIndex").toString());
                    lessonRepository.findById(lId).ifPresent(l -> {
                        l.setOrderIndex(order);
                        if (item.get("chapterId") != null) {
                            Long chId = Long.valueOf(item.get("chapterId").toString());
                            chapterRepository.findById(chId).ifPresent(l::setChapter);
                        }
                        lessonRepository.save(l);
                    });
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success("OK", "Đã cập nhật thứ tự các bài học thành công"));
    }

    // --- 3.4. QUẢN LÝ VIDEO (UPLOAD / DELETE VIDEO CHO BÀI HỌC) ---

    @PostMapping("/lessons/{id}/video")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadLessonVideo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            VideoAsset asset = videoStreamingService.uploadLessonVideo(id, file);

            // Cập nhật lại thời lượng bài học
            Lesson lesson = lessonRepository.findById(id).orElse(null);
            if (lesson != null && asset.getDurationSeconds() > 0) {
                lesson.setDurationSeconds(asset.getDurationSeconds());
                lessonRepository.save(lesson);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", asset.getId());
            resp.put("fileName", asset.getOriginalFileName());
            resp.put("videoUrl", asset.getRawVideoUrl());
            resp.put("hlsUrl", asset.getHlsPlaylistUrl());
            resp.put("provider", asset.getStorageProvider().name());
            resp.put("durationSeconds", asset.getDurationSeconds());
            resp.put("resolution", asset.getResolution());
            return ResponseEntity.ok(ApiResponse.success(resp, "Tải lên và đồng bộ video thành công!"));
        } catch (Exception e) {
            log.error("Lỗi upload video bài học {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi upload video: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lessons/{id}/video")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteLessonVideo(@PathVariable Long id) {
        Optional<VideoAsset> vOpt = videoAssetRepository.findByLessonId(id);
        if (vOpt.isPresent()) {
            videoAssetRepository.delete(vOpt.get());
            log.info("Đã xóa bỏ video của bài học ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("Xóa video thành công", "Đã xóa bỏ video của bài học ID " + id));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Không có video", "Bài học này chưa có video"));
        }
    }

    private String generateSlug(String text) {
        if (text == null) return "khoa-hoc-" + System.currentTimeMillis();
        String slug = java.text.Normalizer.normalize(text.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return slug + "-" + (System.currentTimeMillis() % 10000);
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
        long totalFaculties = facultyRepository.count();
        long totalClasses = classEntityRepository.count();
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
        stats.put("totalFaculties", totalFaculties);
        stats.put("totalClasses", totalClasses);
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
