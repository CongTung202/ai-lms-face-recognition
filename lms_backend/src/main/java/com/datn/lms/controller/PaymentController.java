package com.datn.lms.controller;

import com.datn.lms.dto.ApiResponse;
import com.datn.lms.entity.mysql.*;
import com.datn.lms.repository.mysql.*;
import com.datn.lms.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PayOSService payOSService;
    private final OrderRepository orderRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * 1. Tạo đơn hàng thanh toán PayOS (hoặc ghi danh miễn phí nếu price = 0)
     * Hỗ trợ tự động tạo tài khoản học viên tạm thời (INACTIVE) trước khi thanh toán
     */
    @PostMapping({"/create-order", "/register-and-order"})
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentOrder(@RequestBody Map<String, Object> req) {
        Long courseId = req.get("courseId") != null ? Long.valueOf(req.get("courseId").toString()) : null;
        Long studentId = req.get("studentId") != null ? Long.valueOf(req.get("studentId").toString()) : null;
        String studentCodeParam = req.get("studentCode") != null ? req.get("studentCode").toString() : null;

        String fullName = req.get("fullName") != null ? req.get("fullName").toString().trim() : null;
        String email = req.get("email") != null ? req.get("email").toString().trim().toLowerCase() : null;
        String phone = req.get("phone") != null ? req.get("phone").toString().trim() : null;
        String password = req.get("password") != null ? req.get("password").toString().trim() : null;

        if (courseId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Vui lòng cung cấp courseId"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy khóa học"));
        }

        Student student = null;

        // A. NẾU CÓ ĐIỀN EMAIL/HỌ TÊN: TẠO TÀI KHOẢN HỌC VIÊN TẠM THỜI (TRẠNG THÁI INACTIVE)
        if (email != null && !email.isBlank()) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                student = studentRepository.findById(user.getId()).orElse(null);
                if (student == null) {
                    // Nếu user đã có nhưng chưa có hồ sơ student
                    int year = LocalDateTime.now().getYear() % 100;
                    String scode = "SV" + year + String.format("%04d", (int) (Math.random() * 9000 + 1000));
                    student = Student.builder()
                            .user(user)
                            .studentCode(scode)
                            .classId(1L)
                            .facultyId(1L)
                            .majorId(1L)
                            .gender(Student.Gender.MALE)
                            .faceRegistered(false)
                            .build();
                    student = studentRepository.save(student);
                }
            } else {
                int year = LocalDateTime.now().getYear() % 100;
                String scode = "SV" + year + String.format("%04d", (int) (Math.random() * 9000 + 1000));
                while (studentRepository.existsByStudentCode(scode)) {
                    scode = "SV" + year + String.format("%04d", (int) (Math.random() * 9000 + 1000));
                }

                String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
                if (baseUsername.isBlank()) baseUsername = scode.toLowerCase();
                String uname = baseUsername;
                int counter = 1;
                while (userRepository.existsByUsername(uname)) {
                    uname = baseUsername + counter++;
                }

                String rawPassword = (password != null && !password.isBlank()) ? password
                        : (phone != null && !phone.isBlank() ? phone : "123456");

                user = User.builder()
                        .username(uname)
                        .fullName(fullName != null && !fullName.isBlank() ? fullName : "Học viên " + scode)
                        .email(email)
                        .phone(phone)
                        .role(User.Role.STUDENT)
                        .status(User.UserStatus.INACTIVE) // TÀI KHOẢN TẠM THỜI CHƯA KÍCH HOẠT!
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .build();
                user = userRepository.save(user);

                student = Student.builder()
                        .user(user)
                        .studentCode(scode)
                        .classId(1L)
                        .facultyId(1L)
                        .majorId(1L)
                        .gender(Student.Gender.MALE)
                        .faceRegistered(false)
                        .build();
                student = studentRepository.save(student);
                log.info("[Register Course] Khởi tạo tài khoản học viên tạm thời: {} (MSSV: {}, trạng thái: INACTIVE)", uname, scode);
            }
        }

        // B. NẾU KHÔNG ĐIỀN EMAIL: TÌM THEO STUDENT ID / CODE CŨ (BACKWARD COMPATIBLE)
        if (student == null) {
            if (studentId != null) {
                student = studentRepository.findById(studentId).orElse(null);
            }
            if (student == null && studentCodeParam != null) {
                student = studentRepository.findByStudentCode(studentCodeParam).orElse(null);
            }
            if (student == null) {
                student = studentRepository.findByStudentCode("2310900051")
                        .orElseGet(() -> studentRepository.findAll().stream().findFirst().orElse(null));
                if (student == null) {
                    return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy hồ sơ sinh viên"));
                }
            }
        }

        // Kiểm tra xem sinh viên đã kích hoạt khóa học này chưa
        boolean alreadyEnrolled = courseEnrollmentRepository.existsByStudentUserIdAndCourseIdAndStatus(
                student.getUserId(), course.getId(), CourseEnrollment.EnrollmentStatus.ACTIVE);
        if (alreadyEnrolled) {
            Map<String, Object> res = new HashMap<>();
            res.put("alreadyEnrolled", true);
            res.put("courseId", course.getId());
            res.put("studentCode", student.getStudentCode());
            res.put("username", student.getUser() != null ? student.getUser().getUsername() : student.getStudentCode());
            return ResponseEntity.ok(ApiResponse.success(res, "Bạn đã đăng ký khóa học này rồi! Vui lòng vào học."));
        }

        BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        boolean isFree = Boolean.TRUE.equals(course.getIsFree()) || price.compareTo(BigDecimal.ZERO) <= 0;

        // Nếu là khóa học miễn phí: Cấp quyền & Kích hoạt tài khoản ngay lập tức
        if (isFree) {
            activateEnrollment(student, course, CourseEnrollment.GrantType.PURCHASED);

            long freeOrderCode = System.currentTimeMillis();
            Order freeOrder = Order.builder()
                    .orderCode(String.valueOf(freeOrderCode))
                    .student(student)
                    .course(course)
                    .amount(BigDecimal.ZERO)
                    .paymentMethod(Order.PaymentMethod.FREE)
                    .paymentStatus(Order.PaymentStatus.SUCCESS)
                    .paidAt(LocalDateTime.now())
                    .transactionId("FREE_" + freeOrderCode)
                    .build();
            orderRepository.save(freeOrder);

            Map<String, Object> res = new HashMap<>();
            res.put("isFree", true);
            res.put("enrolled", true);
            res.put("activated", true);
            res.put("orderCode", freeOrderCode);
            res.put("courseId", course.getId());
            res.put("studentCode", student.getStudentCode());
            res.put("username", student.getUser() != null ? student.getUser().getUsername() : student.getStudentCode());
            res.put("fullName", student.getUser() != null ? student.getUser().getFullName() : student.getStudentCode());
            return ResponseEntity.ok(ApiResponse.success(res, "Đăng ký và kích hoạt khóa học miễn phí thành công!"));
        }

        // Khóa học có phí -> Tạo order và sinh VietQR PayOS (Tài khoản User giữ nguyên trạng thái INACTIVE)
        long numericOrderCode = (System.currentTimeMillis() % 10000000000L) + (long) (Math.random() * 899 + 100);
        String orderCodeStr = String.valueOf(numericOrderCode);

        Order order = Order.builder()
                .orderCode(orderCodeStr)
                .student(student)
                .course(course)
                .amount(price)
                .paymentMethod(Order.PaymentMethod.PAYOS)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();
        orderRepository.save(order);

        String buyerName = student.getUser() != null ? student.getUser().getFullName() : "Sinh Vien NTU";
        String buyerEmail = student.getUser() != null ? student.getUser().getEmail() : "student@daihocnguyentrai.edu.vn";
        String description = "HOCPHI " + numericOrderCode;

        Map<String, Object> payosResult = payOSService.createPaymentLink(
                numericOrderCode,
                price.intValue(),
                description,
                buyerName,
                buyerEmail
        );

        payosResult.put("courseId", course.getId());
        payosResult.put("courseTitle", course.getTitle());
        payosResult.put("studentName", buyerName);
        payosResult.put("studentCode", student.getStudentCode());
        payosResult.put("username", student.getUser() != null ? student.getUser().getUsername() : student.getStudentCode());
        payosResult.put("accountStatus", student.getUser() != null ? student.getUser().getStatus().name() : "INACTIVE");
        payosResult.put("orderId", order.getId());
        payosResult.put("isFree", false);

        return ResponseEntity.ok(ApiResponse.success(payosResult, "Khởi tạo tài khoản tạm thời & đơn hàng PayOS thành công"));
    }

    /**
     * 2. Tra cứu trạng thái đơn hàng (Polling từ frontend mỗi 2.5 giây, chủ động đối soát PayOS)
     */
    @GetMapping("/order-status/{orderCode}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatus(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy đơn hàng " + orderCode));
        }

        // Nếu đơn hàng đang PENDING, chủ động đối soát trực tiếp với API cổng thanh toán PayOS
        if (order.getPaymentStatus() == Order.PaymentStatus.PENDING) {
            try {
                Long numericCode = Long.valueOf(orderCode);
                Map<String, Object> payosInfo = payOSService.getPaymentLinkInformation(numericCode);
                if (payosInfo != null) {
                    String status = String.valueOf(payosInfo.get("status"));
                    if ("PAID".equalsIgnoreCase(status)) {
                        order.setPaymentStatus(Order.PaymentStatus.SUCCESS);
                        order.setPaidAt(LocalDateTime.now());
                        if (payosInfo.get("id") != null) {
                            order.setTransactionId(payosInfo.get("id").toString());
                        }
                        orderRepository.save(order);
                        activateEnrollment(order.getStudent(), order.getCourse(), CourseEnrollment.GrantType.PURCHASED);
                        log.info("[PayOS Polling] Payment confirmed via PayOS API for orderCode: {}", orderCode);
                    } else if ("CANCELLED".equalsIgnoreCase(status)) {
                        order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
                        orderRepository.save(order);
                    }
                }
            } catch (Exception e) {
                log.warn("[PayOS Polling] Error querying orderCode {}: {}", orderCode, e.getMessage());
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("orderCode", order.getOrderCode());
        map.put("status", order.getPaymentStatus().name());
        map.put("amount", order.getAmount());
        map.put("paidAt", order.getPaidAt());
        map.put("courseId", order.getCourse() != null ? order.getCourse().getId() : null);
        map.put("isSuccess", order.getPaymentStatus() == Order.PaymentStatus.SUCCESS);
        if (order.getStudent() != null) {
            map.put("studentCode", order.getStudent().getStudentCode());
            if (order.getStudent().getUser() != null) {
                map.put("username", order.getStudent().getUser().getUsername());
                map.put("fullName", order.getStudent().getUser().getFullName());
                map.put("accountStatus", order.getStudent().getUser().getStatus().name());
                map.put("isActivated", order.getStudent().getUser().getStatus() == User.UserStatus.ACTIVE);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(map, "Trạng thái đơn hàng: " + order.getPaymentStatus().name()));
    }

    /**
     * 3. Webhook nhận thông báo từ PayOS khi thanh toán thành công (Xác thực chữ ký số HMAC-SHA256)
     */
    @PostMapping("/payos-webhook")
    @Transactional
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Map<String, Object> webhookPayload) {
        log.info("[Webhook] Received webhook from PayOS: {}", webhookPayload);
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = payOSService.verifyWebhookSignature(webhookPayload);
            if (!isValid) {
                log.warn("[Webhook] Invalid PayOS signature - Rejecting request for security");
                response.put("code", "01");
                response.put("message", "Chữ ký webhook không hợp lệ (Invalid Signature)");
                return ResponseEntity.badRequest().body(response);
            }

            Object dataObj = webhookPayload.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object oc = data.get("orderCode");
                String orderCode = oc != null ? oc.toString() : null;

                if (orderCode != null) {
                    Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
                    if (order != null && order.getPaymentStatus() != Order.PaymentStatus.SUCCESS) {
                        order.setPaymentStatus(Order.PaymentStatus.SUCCESS);
                        order.setPaidAt(LocalDateTime.now());
                        if (data.get("reference") != null) {
                            order.setTransactionId(data.get("reference").toString());
                        }
                        orderRepository.save(order);

                        activateEnrollment(order.getStudent(), order.getCourse(), CourseEnrollment.GrantType.PURCHASED);
                        log.info("[Webhook] Successfully processed orderCode: {}, activated student: {}", orderCode, order.getStudent().getUserId());
                    }
                }
            }
            response.put("code", "00");
            response.put("message", "Success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[Webhook] Error processing webhook: {}", e.getMessage(), e);
            response.put("code", "99");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 4. Xác nhận thủ công / Giả lập thanh toán thành công tức thì (Dành cho demo / bảo vệ đồ án)
     */
    @PostMapping("/manual-confirm/{orderCode}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> manualConfirmOrder(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy đơn hàng " + orderCode));
        }

        order.setPaymentStatus(Order.PaymentStatus.SUCCESS);
        order.setPaidAt(LocalDateTime.now());
        order.setTransactionId("SIMULATED_" + System.currentTimeMillis());
        orderRepository.save(order);

        activateEnrollment(order.getStudent(), order.getCourse(), CourseEnrollment.GrantType.PURCHASED);

        Map<String, Object> res = new HashMap<>();
        res.put("orderCode", order.getOrderCode());
        res.put("status", "SUCCESS");
        res.put("courseId", order.getCourse() != null ? order.getCourse().getId() : null);
        if (order.getStudent() != null) {
            res.put("studentCode", order.getStudent().getStudentCode());
            if (order.getStudent().getUser() != null) {
                res.put("username", order.getStudent().getUser().getUsername());
                res.put("fullName", order.getStudent().getUser().getFullName());
                res.put("isActivated", true);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(res, "Đã xác nhận thanh toán thành công và mở quyền vào học!"));
    }

    /**
     * 5. Kiểm tra quyền truy cập khóa học của sinh viên
     */
    @GetMapping("/check-enrollment")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEnrollment(
            @RequestParam Long courseId,
            @RequestParam(required = false, defaultValue = "3") Long studentId) {

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy khóa học"));
        }

        boolean isFree = Boolean.TRUE.equals(course.getIsFree()) || (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) <= 0);
        boolean isEnrolled = courseEnrollmentRepository.existsByStudentUserIdAndCourseIdAndStatus(
                studentId, courseId, CourseEnrollment.EnrollmentStatus.ACTIVE);

        Map<String, Object> res = new HashMap<>();
        res.put("courseId", courseId);
        res.put("studentId", studentId);
        res.put("isFree", isFree);
        res.put("isEnrolled", isEnrolled);
        res.put("canAccess", isFree || isEnrolled);

        return ResponseEntity.ok(ApiResponse.success(res, "Kiểm tra quyền ghi danh hoàn tất"));
    }

    /**
     * 6. Lấy lịch sử đơn hàng đã mua của sinh viên
     */
    @GetMapping("/my-orders")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyOrders(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String username) {

        List<Order> orders = Collections.emptyList();

        if (studentId != null) {
            orders = orderRepository.findByStudentUserIdOrderByCreatedAtDesc(studentId);
        } else if (studentCode != null && !studentCode.isBlank()) {
            Student s = studentRepository.findByStudentCode(studentCode.trim()).orElse(null);
            if (s != null) {
                orders = orderRepository.findByStudentUserIdOrderByCreatedAtDesc(s.getUserId());
            }
        } else if (username != null && !username.isBlank()) {
            User u = userRepository.findByUsername(username.trim()).orElse(null);
            if (u != null) {
                orders = orderRepository.findByStudentUserIdOrderByCreatedAtDesc(u.getId());
            }
        } else {
            // Mặc định trả về toàn bộ đơn hàng mới nhất
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId());
            map.put("orderCode", o.getOrderCode());
            map.put("amount", o.getAmount());
            map.put("paymentMethod", o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "PAYOS");
            map.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "PENDING");
            map.put("transactionId", o.getTransactionId());
            map.put("paidAt", o.getPaidAt());
            map.put("createdAt", o.getCreatedAt());

            if (o.getCourse() != null) {
                map.put("courseId", o.getCourse().getId());
                map.put("courseTitle", o.getCourse().getTitle());
                map.put("courseThumb", o.getCourse().getThumbnailUrl());
            }

            if (o.getStudent() != null) {
                map.put("studentId", o.getStudent().getUserId());
                map.put("studentCode", o.getStudent().getStudentCode());
                if (o.getStudent().getUser() != null) {
                    map.put("studentName", o.getStudent().getUser().getFullName());
                    map.put("username", o.getStudent().getUser().getUsername());
                }
            }
            result.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy lịch sử đơn hàng thành công"));
    }

    /**
     * Kích hoạt tài khoản học viên và cấp mới quyền ghi danh khóa học
     */
    private void activateEnrollment(Student student, Course course, CourseEnrollment.GrantType grantType) {
        if (student == null || course == null) return;

        // 1. KÍCH HOẠT TÀI KHOẢN NGƯỜI DÙNG CHÍNH THỨC
        if (student.getUser() != null) {
            User user = student.getUser();
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                user.setStatus(User.UserStatus.ACTIVE);
                userRepository.save(user);
                log.info("[Payment] ĐÃ KÍCH HOẠT THÀNH CÔNG tài khoản: {} sau khi thanh toán", user.getUsername());
            }
        } else if (student.getUserId() != null) {
            userRepository.findById(student.getUserId()).ifPresent(u -> {
                if (u.getStatus() != User.UserStatus.ACTIVE) {
                    u.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(u);
                    log.info("[Payment] ĐÃ KÍCH HOẠT THÀNH CÔNG tài khoản: {} sau khi thanh toán", u.getUsername());
                }
            });
        }

        // 2. KÍCH HOẠT GHI DANH KHÓA HỌC
        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByStudentUserIdAndCourseId(student.getUserId(), course.getId())
                .orElse(null);

        if (enrollment == null) {
            enrollment = CourseEnrollment.builder()
                    .student(student)
                    .course(course)
                    .grantType(grantType)
                    .status(CourseEnrollment.EnrollmentStatus.ACTIVE)
                    .enrolledAt(LocalDateTime.now())
                    .build();
        } else {
            enrollment.setStatus(CourseEnrollment.EnrollmentStatus.ACTIVE);
            enrollment.setGrantType(grantType);
        }
        courseEnrollmentRepository.save(enrollment);
    }
}
