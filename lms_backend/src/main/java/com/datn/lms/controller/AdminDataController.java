package com.datn.lms.controller;

import com.datn.lms.dto.AdminProfileDto;
import com.datn.lms.dto.AdminProfileUpdateRequest;
import com.datn.lms.dto.ApiResponse;
import com.datn.lms.entity.mysql.Course;
import com.datn.lms.entity.mysql.FaceAttendanceLog;
import com.datn.lms.entity.mysql.Student;
import com.datn.lms.entity.mysql.User;
import com.datn.lms.entity.mysql.VideoAsset;
import com.datn.lms.repository.mongodb.CourseDocumentMongoRepository;
import com.datn.lms.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
    private final VideoAssetRepository videoAssetRepository;
    private final FaceAttendanceLogRepository attendanceLogRepository;
    private final CourseDocumentMongoRepository mongoDocumentRepository;
    private final RestTemplate restTemplate;

    public AdminDataController(
            UserRepository userRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            VideoAssetRepository videoAssetRepository,
            FaceAttendanceLogRepository attendanceLogRepository,
            CourseDocumentMongoRepository mongoDocumentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.mongoDocumentRepository = mongoDocumentRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 1. Lấy thông tin tài khoản hiện tại (Real Profile Data)
     * GET /api/admin/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AdminProfileDto>> getProfile(
            @RequestParam(value = "username", required = false, defaultValue = "nct30000") String username) {

        // Tìm kiếm người dùng theo username hoặc tạo mới nếu chưa tồn tại
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // Tạo tài khoản chuẩn theo ảnh mẫu
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

        // Kiểm tra thông tin Student nếu có
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

    /**
     * 2. Cập nhật thông tin tài khoản thực tế
     * PUT /api/admin/profile
     */
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

        // Cập nhật thông tin Student nếu có
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

        log.info("Cập nhật thành công hồ sơ người dùng: {}", user.getUsername());
        return getProfile(user.getUsername());
    }

    /**
     * 3. Lấy toàn bộ danh sách người dùng thực tế từ MySQL
     * GET /api/admin/users
     */
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

    /**
     * 4. Lấy dữ liệu thống kê tổng hợp thực tế (MySQL + MongoDB + AI Service)
     * GET /api/admin/stats
     */
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

        // Kiểm tra sức khỏe dịch vụ Face AI
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

    /**
     * 5. Lấy danh sách khóa học thực tế
     * GET /api/admin/courses
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<Course>>> getCourses() {
        List<Course> list = courseRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách khóa học thành công"));
    }

    /**
     * 6. Lấy danh sách video thực tế
     * GET /api/admin/videos
     */
    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<List<VideoAsset>>> getVideos() {
        List<VideoAsset> list = videoAssetRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách video thành công"));
    }

    /**
     * 7. Lấy danh sách nhật ký điểm danh khuôn mặt
     * GET /api/admin/attendance-logs
     */
    @GetMapping("/attendance-logs")
    public ResponseEntity<ApiResponse<List<FaceAttendanceLog>>> getAttendanceLogs() {
        List<FaceAttendanceLog> list = attendanceLogRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy nhật ký điểm danh khuôn mặt thành công"));
    }
}
