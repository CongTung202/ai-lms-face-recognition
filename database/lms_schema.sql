-- ==============================================================================
-- ĐỒ ÁN TỐT NGHIỆP: HỆ THỐNG QUẢN LÝ HỌC TẬP & ÔN TẬP TRỰC TUYẾN (LMS)
-- TÍCH HỢP XEM VIDEO STREAMING & THI TRẮC NGHIỆM GIÁM SÁT AI (FACE RECOGNITION)
-- Hệ quản trị cơ sở dữ liệu: MySQL 8.0+
-- Kiến trúc: Java Spring Boot + MySQL + MongoDB (RAG) + FastAPI (AI Face)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `lms_online_db`
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `lms_online_db`;

-- Tắt kiểm tra khóa ngoại tạm thời để tránh xung đột khi tạo mới/drop bảng
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. PHÂN HỆ CƠ CẤU TỔ CHỨC & NGƯỜI DÙNG (ORGANIZATION & RBAC)
-- ------------------------------------------------------------------------------

-- 1.1. Bảng Khoa (Faculties)
DROP TABLE IF EXISTS `faculties`;
CREATE TABLE `faculties` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `faculty_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã khoa, ví dụ: CNTT, DTVT, KTXD',
    `name` VARCHAR(255) NOT NULL COMMENT 'Tên khoa, ví dụ: Khoa Công nghệ Thông tin',
    `description` TEXT COMMENT 'Mô tả khoa',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục Khoa';

-- 1.2. Bảng Chuyên ngành (Majors)
DROP TABLE IF EXISTS `majors`;
CREATE TABLE `majors` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `faculty_id` BIGINT NOT NULL COMMENT 'Thuộc khoa nào',
    `major_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã ngành, ví dụ: SE, AI, IS',
    `name` VARCHAR(255) NOT NULL COMMENT 'Tên ngành, ví dụ: Kỹ thuật Phần mềm',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_majors_faculty` FOREIGN KEY (`faculty_id`) REFERENCES `faculties` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục Chuyên ngành đào tạo';

-- 1.3. Bảng Lớp sinh hoạt / Lớp niên chế (Classes)
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `major_id` BIGINT NOT NULL COMMENT 'Thuộc ngành nào',
    `class_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã lớp, ví dụ: K23CNT3, D20CQCN01',
    `name` VARCHAR(150) NOT NULL COMMENT 'Tên lớp',
    `academic_year` VARCHAR(20) NOT NULL COMMENT 'Khóa/Niên khóa, ví dụ: 2022-2026',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_classes_major` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh sách Lớp sinh hoạt niên chế';

-- 1.4. Bảng Tài khoản người dùng (Users) - Trung tâm định danh (UID số)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'UID số duy nhất trên toàn hệ thống',
    `username` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Tên đăng nhập',
    `email` VARCHAR(150) NOT NULL UNIQUE COMMENT 'Địa chỉ email',
    `phone` VARCHAR(20) UNIQUE COMMENT 'Số điện thoại',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'Mật khẩu đã băm BCrypt',
    `full_name` VARCHAR(150) NOT NULL COMMENT 'Họ và tên đầy đủ',
    `avatar_url` VARCHAR(500) COMMENT 'Đường dẫn ảnh đại diện',
    `role` ENUM('ADMIN', 'LECTURER', 'STUDENT') NOT NULL DEFAULT 'STUDENT' COMMENT 'Vai trò chính',
    `status` ENUM('ACTIVE', 'INACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Trạng thái tài khoản',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_role` (`role`),
    INDEX `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng người dùng tập trung (Quản trị viên, Giảng viên, Sinh viên)';

-- 1.5. Bảng Thông tin Sinh viên (Students) - Quan hệ 1-1 với Users
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
    `user_id` BIGINT PRIMARY KEY COMMENT 'UID số (trùng với users.id)',
    `student_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã số sinh viên (MSSV)',
    `class_id` BIGINT NOT NULL COMMENT 'Lớp niên chế',
    `major_id` BIGINT NOT NULL COMMENT 'Chuyên ngành theo học',
    `date_of_birth` DATE COMMENT 'Ngày sinh',
    `gender` ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'MALE' COMMENT 'Giới tính',
    `face_registered` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã đăng ký khuôn mặt AI chưa',
    `face_registered_at` DATETIME COMMENT 'Thời điểm đăng ký khuôn mặt',
    `face_image_url` VARCHAR(500) COMMENT 'Ảnh chân dung mẫu đã chuẩn hóa',
    `face_data_id` VARCHAR(100) COMMENT 'Mã tham chiếu vector nhận diện bên FastAPI/MongoDB',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_students_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_students_major` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_students_code` (`student_code`),
    INDEX `idx_students_face` (`face_registered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hồ sơ chi tiết của Sinh viên';

-- 1.6. Bảng Thông tin Giảng viên (Lecturers) - Quan hệ 1-1 với Users
DROP TABLE IF EXISTS `lecturers`;
CREATE TABLE `lecturers` (
    `user_id` BIGINT PRIMARY KEY COMMENT 'UID số của giảng viên (trùng với users.id)',
    `lecturer_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã số giảng viên',
    `faculty_id` BIGINT NOT NULL COMMENT 'Thuộc khoa nào',
    `academic_degree` VARCHAR(100) COMMENT 'Học vị (Thạc sĩ, Tiến sĩ, PGS, GS...)',
    `bio` TEXT COMMENT 'Giới thiệu ngắn gọn',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lecturers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_lecturers_faculty` FOREIGN KEY (`faculty_id`) REFERENCES `faculties` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hồ sơ chi tiết của Giảng viên';

-- ------------------------------------------------------------------------------
-- 2. PHÂN HỆ MÔN HỌC, KHÓA HỌC & CẤP/MUA KHÓA HỌC (COURSES & ENROLLMENT)
-- ------------------------------------------------------------------------------

-- 2.1. Bảng Môn học (Subjects)
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã môn học, ví dụ: CS101, IT302',
    `name` VARCHAR(255) NOT NULL COMMENT 'Tên môn học, ví dụ: Lập trình Hướng đối tượng',
    `credits` INT NOT NULL DEFAULT 3 COMMENT 'Số tín chỉ',
    `description` TEXT COMMENT 'Mô tả môn học',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Khung chương trình môn học';

-- 2.2. Bảng Khóa học / Lớp học phần trực tuyến (Courses)
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` BIGINT NOT NULL COMMENT 'Thuộc môn học nào',
    `lecturer_id` BIGINT NOT NULL COMMENT 'Giảng viên phụ trách giảng dạy (UID số)',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề khóa học, ví dụ: Lập trình Java Spring Boot Nâng cao',
    `slug` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Đường dẫn thân thiện URL',
    `description` LONGTEXT COMMENT 'Nội dung chi tiết giới thiệu khóa học',
    `thumbnail_url` VARCHAR(500) COMMENT 'Ảnh bìa khóa học',
    `price` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Giá khóa học (0 nếu miễn phí/được cấp)',
    `is_free` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đánh dấu khóa học miễn phí công khai',
    `enrollment_type` ENUM('ASSIGNED_BY_MAJOR', 'OPEN_FREE', 'PURCHASED') NOT NULL DEFAULT 'ASSIGNED_BY_MAJOR' 
        COMMENT 'Hình thức: Cấp theo chuyên ngành, Mở tự do, hoặc Phải mua',
    `status` ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT 'Trạng thái phát hành',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_courses_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_courses_lecturer` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_courses_status` (`status`),
    INDEX `idx_courses_type` (`enrollment_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Khóa học / Lớp học phần trực tuyến';

-- 2.3. Bảng Phân bổ Khóa học theo Ngành (Course_Majors)
-- Phục vụ bài toán: Khóa học được tự động cấp cho sinh viên thuộc các chuyên ngành tương ứng
DROP TABLE IF EXISTS `course_majors`;
CREATE TABLE `course_majors` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT 'Khóa học',
    `major_id` BIGINT NOT NULL COMMENT 'Ngành được cấp',
    `is_mandatory` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Học phần bắt buộc hay tự chọn theo ngành',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_course_majors_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_course_majors_major` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY `uk_course_major` (`course_id`, `major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cấu hình cấp khóa học tự động theo chuyên ngành';

-- 2.4. Bảng Đăng ký & Sở hữu khóa học của sinh viên (Course_Enrollments)
DROP TABLE IF EXISTS `course_enrollments`;
CREATE TABLE `course_enrollments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT 'UID số của sinh viên',
    `course_id` BIGINT NOT NULL COMMENT 'Khóa học được học',
    `grant_type` ENUM('ASSIGNED_BY_MAJOR', 'PURCHASED', 'ADMIN_GRANTED') NOT NULL DEFAULT 'ASSIGNED_BY_MAJOR' 
        COMMENT 'Nguồn cấp quyền học: Theo ngành, Tự mua, hoặc Admin gán thủ công',
    `status` ENUM('ACTIVE', 'SUSPENDED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Trạng thái truy cập',
    `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Ngày bắt đầu kích hoạt',
    `expired_at` DATETIME NULL COMMENT 'Ngày hết hạn truy cập (nếu có)',
    CONSTRAINT `fk_enrollments_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_enrollments_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
    INDEX `idx_enrollments_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh sách quyền học của sinh viên đối với từng khóa';

-- 2.5. Bảng Đơn hàng & Thanh toán mua khóa học (Orders)
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã đơn hàng, ví dụ: ORD20261012001',
    `student_id` BIGINT NOT NULL COMMENT 'UID sinh viên thanh toán',
    `course_id` BIGINT NOT NULL COMMENT 'Khóa học được mua',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT 'Số tiền thanh toán',
    `payment_method` ENUM('PAYOS', 'VNPAY', 'MOMO', 'BANK_TRANSFER', 'FREE') NOT NULL COMMENT 'Cổng thanh toán',
    `payment_status` ENUM('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT 'Trạng thái',
    `transaction_id` VARCHAR(100) COMMENT 'Mã giao dịch từ cổng thanh toán đối tác',
    `paid_at` DATETIME NULL COMMENT 'Thời gian thanh toán thành công',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_orders_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_orders_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_orders_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử đơn hàng mua khóa học';

-- ------------------------------------------------------------------------------
-- 3. PHÂN HỆ CHƯƠNG MỤC, BÀI GIẢNG VIDEO & TÀI LIỆU (CURRICULUM & VIDEO)
-- ------------------------------------------------------------------------------

-- 3.1. Bảng Chương học (Course_Chapters)
DROP TABLE IF EXISTS `course_chapters`;
CREATE TABLE `course_chapters` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT 'Khóa học cha',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tên chương, ví dụ: Chương 1: Tổng quan Kiến trúc',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự sắp xếp chương trong khóa',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chapters_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_chapters_order` (`course_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chương mục bài giảng trong khóa học';

-- 3.2. Bảng Bài học (Lessons)
DROP TABLE IF EXISTS `lessons`;
CREATE TABLE `lessons` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `chapter_id` BIGINT NOT NULL COMMENT 'Thuộc chương nào',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tên bài học',
    `lesson_type` ENUM('VIDEO', 'DOCUMENT', 'QUIZ') NOT NULL DEFAULT 'VIDEO' COMMENT 'Loại bài học',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự sắp xếp trong chương',
    `duration_seconds` INT NOT NULL DEFAULT 0 COMMENT 'Thời lượng bài học (giây)',
    `is_preview` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép xem thử miễn phí không cần mua',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lessons_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `course_chapters` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_lessons_order` (`chapter_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bài học chi tiết';

-- 3.3. Bảng Quản lý Video Streaming (Video_Assets)
-- Tối ưu hóa truy xuất video & tải lên nhanh chóng (HLS .m3u8, đa độ phân giải, MinIO/S3)
DROP TABLE IF EXISTS `video_assets`;
CREATE TABLE `video_assets` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lesson_id` BIGINT NOT NULL UNIQUE COMMENT 'Gắn liền 1-1 với bài học',
    `original_file_name` VARCHAR(255) NOT NULL COMMENT 'Tên file gốc khi upload',
    `storage_provider` ENUM('LOCAL', 'MINIO', 'S3', 'CLOUDINARY') NOT NULL DEFAULT 'LOCAL' COMMENT 'Nơi lưu trữ',
    `raw_video_url` VARCHAR(500) COMMENT 'Link video mp4 gốc (dùng nội bộ)',
    `hls_playlist_url` VARCHAR(500) COMMENT 'Link file .m3u8 HLS streaming (tối ưu phát nhanh, chống lag, load chunk)',
    `dash_manifest_url` VARCHAR(500) COMMENT 'Link file .mpd (nếu dùng DASH streaming)',
    `thumbnail_url` VARCHAR(500) COMMENT 'Ảnh thumbnail video bài giảng',
    `duration_seconds` INT NOT NULL DEFAULT 0 COMMENT 'Tổng thời lượng video (giây)',
    `resolution` VARCHAR(50) DEFAULT '1080p' COMMENT 'Độ phân giải hỗ trợ (720p, 1080p, Adaptive Bitrate)',
    `file_size_bytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'Kích thước file (bytes)',
    `encode_status` ENUM('UPLOADING', 'PROCESSING', 'READY', 'FAILED') NOT NULL DEFAULT 'READY' 
        COMMENT 'Trạng thái chuyển mã (Transcoding ffmpeg)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_video_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý lưu trữ & công nghệ streaming video';

-- 3.4. Bảng Tài liệu Môn học (Course_Documents) - Cầu nối tích hợp MongoDB RAG
DROP TABLE IF EXISTS `course_documents`;
CREATE TABLE `course_documents` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT 'Thuộc khóa học nào',
    `lesson_id` BIGINT NULL COMMENT 'Gắn vào bài học cụ thể (nếu có)',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề tài liệu / Slide / Giáo trình',
    `file_type` VARCHAR(20) NOT NULL COMMENT 'Định dạng file: PDF, DOCX, PPTX',
    `file_url` VARCHAR(500) NOT NULL COMMENT 'Đường dẫn tải tài liệu',
    `file_size_bytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'Kích thước file tài liệu (bytes)',
    `mongo_doc_id` VARCHAR(100) NULL COMMENT 'ObjectId bên MongoDB để query Vector/Chunks phục vụ RAG Chatbot',
    `rag_indexed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã được nhúng Vector và nạp vào RAG chưa',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_documents_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_documents_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_doc_mongo` (`mongo_doc_id`),
    INDEX `idx_doc_rag` (`rag_indexed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Metadata tài liệu môn học liên kết MongoDB';

-- ------------------------------------------------------------------------------
-- 4. PHÂN HỆ TIẾN TRÌNH HỌC TẬP & ĐIỂM DANH KHUÔN MẶT (PROGRESS & ATTENDANCE)
-- ------------------------------------------------------------------------------

-- 4.1. Bảng Lịch sử & Tiến trình học (Learning_Progress)
DROP TABLE IF EXISTS `learning_progress`;
CREATE TABLE `learning_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT 'UID số sinh viên',
    `lesson_id` BIGINT NOT NULL COMMENT 'Bài học đang/đã xem',
    `course_id` BIGINT NOT NULL COMMENT 'Khóa học cha',
    `last_watched_second` INT NOT NULL DEFAULT 0 COMMENT 'Giây dừng lại gần nhất (Resume playing)',
    `max_watched_second` INT NOT NULL DEFAULT 0 COMMENT 'Mốc giây xa nhất đã xem (chống tua có chủ đích)',
    `total_time_spent_seconds` INT NOT NULL DEFAULT 0 COMMENT 'Tổng thời gian thực tế đã mở xem bài giảng',
    `completion_percentage` INT NOT NULL DEFAULT 0 COMMENT 'Tỷ lệ phần trăm hoàn thành (0 - 100%)',
    `is_completed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã hoàn thành bài học chưa (ví dụ đạt >= 85% thời lượng)',
    `completed_at` DATETIME NULL COMMENT 'Thời điểm hoàn thành',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_progress_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_progress_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_progress_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY `uk_student_lesson_progress` (`student_id`, `lesson_id`),
    INDEX `idx_progress_course` (`student_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tiến trình và lịch sử học tập bài giảng';

-- 4.2. Bảng Điểm danh khuôn mặt ngẫu nhiên khi học video (Face_Attendance_Logs)
-- FastAPI gọi endpoint nhận diện hoặc Spring Boot gửi ảnh chụp định kỳ sang FastAPI
DROP TABLE IF EXISTS `face_attendance_logs`;
CREATE TABLE `face_attendance_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT 'UID số sinh viên',
    `lesson_id` BIGINT NOT NULL COMMENT 'Bài học đang mở khi kiểm tra',
    `capture_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm chụp webcam ngẫu nhiên',
    `verification_status` ENUM('SUCCESS', 'FAILED', 'NO_FACE', 'MULTIPLE_FACES', 'SPOOF_DETECTED') NOT NULL 
        COMMENT 'Kết quả thẩm định từ mô hình AI',
    `similarity_score` FLOAT NOT NULL DEFAULT 0 COMMENT 'Độ khớp Cosine Similarity (ngưỡng >= 0.50)',
    `liveness_score` FLOAT NOT NULL DEFAULT 0 COMMENT 'Điểm chống giả mạo khuôn mặt',
    `snapshot_url` VARCHAR(500) COMMENT 'Ảnh chụp bằng chứng khoảnh khắc kiểm tra',
    `note` VARCHAR(255) COMMENT 'Ghi chú bổ sung (ví dụ: Quay mặt đi hướng khác, Phát hiện giả mạo ảnh in)',
    CONSTRAINT `fk_face_att_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_face_att_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_face_att_status` (`verification_status`),
    INDEX `idx_face_att_time` (`capture_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký điểm danh khuôn mặt ngẫu nhiên trong giờ học trực tuyến';

-- ------------------------------------------------------------------------------
-- 5. PHÂN HỆ THI TRẮC NGHIỆM ONLINE & NGÂN HÀNG CÂU HỎI (EXAMS & QUESTION BANK)
-- ------------------------------------------------------------------------------

-- 5.1. Bảng Ngân hàng câu hỏi (Question_Bank)
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` BIGINT NOT NULL COMMENT 'Câu hỏi thuộc môn học nào',
    `creator_id` BIGINT NOT NULL COMMENT 'Giảng viên biên soạn câu hỏi (UID số)',
    `question_text` LONGTEXT NOT NULL COMMENT 'Nội dung câu hỏi (hỗ trợ văn bản/HTML/LaTeX)',
    `question_type` ENUM('SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE') NOT NULL DEFAULT 'SINGLE_CHOICE' 
        COMMENT 'Loại câu hỏi trắc nghiệm',
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'EASY' COMMENT 'Mức độ nhận thức',
    `explanation` TEXT COMMENT 'Lời giải thích đáp án chi tiết khi công bố điểm',
    `default_point` DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Điểm mặc định',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_questions_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_questions_creator` FOREIGN KEY (`creator_id`) REFERENCES `lecturers` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_questions_diff` (`difficulty`),
    INDEX `idx_questions_type` (`question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ngân hàng câu hỏi trắc nghiệm';

-- 5.2. Bảng Lựa chọn đáp án (Question_Options)
DROP TABLE IF EXISTS `question_options`;
CREATE TABLE `question_options` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL COMMENT 'Thuộc câu hỏi nào',
    `option_text` TEXT NOT NULL COMMENT 'Nội dung phương án lựa chọn (A, B, C, D...)',
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đáp án đúng (TRUE) hay sai (FALSE)',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự lựa chọn',
    CONSTRAINT `fk_options_question` FOREIGN KEY (`question_id`) REFERENCES `question_bank` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_options_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Các phương án lựa chọn của câu hỏi trắc nghiệm';

-- 5.3. Bảng Kỳ thi / Bài kiểm tra trắc nghiệm Online (Exams)
DROP TABLE IF EXISTS `exams`;
CREATE TABLE `exams` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT 'Thuộc khóa học nào',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tên bài thi (ví dụ: Thi giữa kỳ Lập trình Web, Thi thử Ôn tập)',
    `description` TEXT COMMENT 'Quy chế và hướng dẫn làm bài thi',
    `duration_minutes` INT NOT NULL COMMENT 'Thời lượng làm bài (phút)',
    `pass_percentage` INT NOT NULL DEFAULT 50 COMMENT 'Ngưỡng đạt (% điểm số)',
    `max_attempts` INT NOT NULL DEFAULT 1 COMMENT 'Số lần làm bài tối đa cho phép',
    `shuffle_questions` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Đảo ngẫu nhiên thứ tự câu hỏi cho từng thí sinh',
    `shuffle_options` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Đảo ngẫu nhiên thứ tự các đáp án trong câu hỏi',
    `require_face_verification` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bắt buộc xác thực khuôn mặt trước & trong khi thi',
    `tab_lock_enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bật chế độ khóa toàn màn hình & phát hiện chuyển Tab',
    `max_violations_allowed` INT NOT NULL DEFAULT 3 COMMENT 'Số lần vi phạm gian lận tối đa trước khi tự động thu bài',
    `start_time` DATETIME NULL COMMENT 'Thời gian mở đề',
    `end_time` DATETIME NULL COMMENT 'Thời gian đóng đề thi',
    `status` ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT' COMMENT 'Trạng thái đề thi',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_exams_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_exams_status` (`status`),
    INDEX `idx_exams_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cấu hình bài thi trắc nghiệm trực tuyến';

-- 5.4. Bảng Câu hỏi trong đề thi (Exam_Questions)
DROP TABLE IF EXISTS `exam_questions`;
CREATE TABLE `exam_questions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL COMMENT 'Đề thi',
    `question_id` BIGINT NOT NULL COMMENT 'Câu hỏi được đưa vào đề',
    `custom_point` DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Thang điểm riêng cho câu này trong bài thi',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự câu hỏi trong đề gốc',
    CONSTRAINT `fk_exam_questions_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_exam_questions_question` FOREIGN KEY (`question_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY `uk_exam_question` (`exam_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh sách câu hỏi được gán vào đề thi';

-- ------------------------------------------------------------------------------
-- 6. PHÂN HỆ LƯỢT THI, KẾT QUẢ & GIÁM SÁT VI PHẠM (ATTEMPTS & PROCTORING)
-- ------------------------------------------------------------------------------

-- 6.1. Bảng Lượt làm bài thi (Exam_Attempts)
DROP TABLE IF EXISTS `exam_attempts`;
CREATE TABLE `exam_attempts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL COMMENT 'Bài thi',
    `student_id` BIGINT NOT NULL COMMENT 'Thí sinh (UID số)',
    `attempt_number` INT NOT NULL DEFAULT 1 COMMENT 'Lần thi thứ mấy',
    `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm bấm Bắt đầu làm bài',
    `submitted_at` DATETIME NULL COMMENT 'Thời điểm nộp bài',
    `total_score` DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tổng điểm đạt được',
    `total_violations` INT NOT NULL DEFAULT 0 COMMENT 'Tổng số lần vi phạm ghi nhận trong ca thi',
    `is_passed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Kết quả Đạt / Chưa đạt',
    `status` ENUM('IN_PROGRESS', 'SUBMITTED', 'DISQUALIFIED_BY_VIOLATION') NOT NULL DEFAULT 'IN_PROGRESS' 
        COMMENT 'Trạng thái bài thi: Đang làm, Đã nộp, hoặc Bị đình chỉ do gian lận',
    `ip_address` VARCHAR(45) COMMENT 'Địa chỉ IP của thí sinh khi thi',
    `user_agent` TEXT COMMENT 'Thông tin trình duyệt / hệ điều hành',
    CONSTRAINT `fk_attempts_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_attempts_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_attempts_student_exam` (`student_id`, `exam_id`),
    INDEX `idx_attempts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lượt thi trắc nghiệm của thí sinh';

-- 6.2. Bảng Chi tiết câu trả lời của thí sinh (Attempt_Answers)
DROP TABLE IF EXISTS `attempt_answers`;
CREATE TABLE `attempt_answers` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` BIGINT NOT NULL COMMENT 'Lượt thi',
    `question_id` BIGINT NOT NULL COMMENT 'Câu hỏi tương ứng',
    `selected_option_ids` JSON NOT NULL COMMENT 'Mảng chứa ID các phương án thí sinh đã tick, ví dụ [12, 14]',
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Trả lời đúng hay sai',
    `earned_point` DECIMAL(4, 2) NOT NULL DEFAULT 0.00 COMMENT 'Điểm số đạt được của câu này',
    `answered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_answers_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_answers_question` FOREIGN KEY (`question_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lựa chọn chi tiết của thí sinh cho từng câu hỏi';

-- 6.3. Bảng Ghi nhận Vi phạm Gian lận trong phòng thi (Exam_Violations)
-- Hỗ trợ: Tab lock, F12 inspect lock, Window blur, Fullscreen exit, và AI Face Proctoring
DROP TABLE IF EXISTS `exam_violations`;
CREATE TABLE `exam_violations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` BIGINT NOT NULL COMMENT 'Lượt thi tương ứng',
    `student_id` BIGINT NOT NULL COMMENT 'UID số thí sinh vi phạm',
    `violation_type` ENUM(
        'TAB_SWITCH',            -- Chuyển đổi tab trình duyệt (Page Visibility API hidden)
        'WINDOW_BLUR',           -- Mất tiêu điểm chuột / click ra ngoài ứng dụng
        'INSPECT_KEY_PRESS',     -- Cố ý bấm F12, Ctrl+Shift+I, Ctrl+U, chuột phải ContextMenu
        'FULLSCREEN_EXIT',       -- Thoát khỏi chế độ toàn màn hình bắt buộc
        'NO_FACE_DETECTED',      -- AI không thấy khuôn mặt thí sinh trong khung hình
        'MULTIPLE_FACES',        -- AI phát hiện có 2 người trở lên xuất hiện trước camera
        'FACE_MISMATCH',         -- Khuôn mặt trước webcam không khớp sinh viên đăng ký (thi hộ)
        'HEAD_POSE_LOOKING_AWAY' -- AI phát hiện quay đầu quá mức nghi vấn nhìn tài liệu/người bên cạnh
    ) NOT NULL COMMENT 'Hành vi vi phạm',
    `violation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm xảy ra vi phạm',
    `details` TEXT COMMENT 'Chi tiết bổ sung (ví dụ: Pitch=32, Yaw=45, phím nhấn F12)',
    `evidence_snapshot_url` VARCHAR(500) COMMENT 'Link ảnh chụp webcam bằng chứng vi phạm',
    `severity_level` ENUM('WARNING', 'CRITICAL') NOT NULL DEFAULT 'WARNING' COMMENT 'Mức độ nghiêm trọng',
    CONSTRAINT `fk_violations_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_violations_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_violations_attempt` (`attempt_id`),
    INDEX `idx_violations_type` (`violation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký bắt quả tang hành vi gian lận trong lúc thi trực tuyến';

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- DỮ LIỆU KHỞI TẠO MẪU (SEED DATA) ĐỂ KIỂM THỬ HỆ THỐNG
-- ------------------------------------------------------------------------------

-- 1. Khoa & Ngành
INSERT INTO `faculties` (`id`, `faculty_code`, `name`, `description`) VALUES
(1, 'CNTT', 'Khoa Công nghệ Thông tin', 'Đào tạo kỹ sư công nghệ thông tin và phần mềm'),
(2, 'DTVT', 'Khoa Điện tử Viễn thông', 'Đào tạo kỹ sư viễn thông và IoT');

INSERT INTO `majors` (`id`, `faculty_id`, `major_code`, `name`) VALUES
(1, 1, 'SE', 'Kỹ thuật Phần mềm'),
(2, 1, 'AI', 'Trí tuệ Nhân tạo & Khoa học Dữ liệu'),
(3, 2, 'IOT', 'Internet vạn vật & Hệ thống nhúng');

-- 2. Lớp sinh hoạt
INSERT INTO `classes` (`id`, `major_id`, `class_code`, `name`, `academic_year`) VALUES
(1, 1, 'K23CNT3', 'Lớp K23 Công nghệ Phần mềm 3', '2023-2027'),
(2, 2, 'K23AI1', 'Lớp K23 Trí tuệ Nhân tạo 1', '2023-2027');

-- 3. Tài khoản người dùng (Mật khẩu mặc định băm BCrypt '123456': $2a$10$w8.1Uu1gJb1kUu1gJb1kU.XYZ...)
-- Ở đây ta dùng chuỗi hash chuẩn của '123456'
INSERT INTO `users` (`id`, `username`, `email`, `phone`, `password_hash`, `full_name`, `role`, `status`) VALUES
(1, 'admin', 'admin@lms.edu.vn', '0901234567', '$2a$10$7Z2eU26W3q4G3lA4T8Q4sOFoD206Oiqe5qZ00mZ6sO82O2tX.kZfe', 'Quản trị viên Hệ thống', 'ADMIN', 'ACTIVE'),
(2, 'giangvien_tung', 'tung.nc@lms.edu.vn', '0912345678', '$2a$10$7Z2eU26W3q4G3lA4T8Q4sOFoD206Oiqe5qZ00mZ6sO82O2tX.kZfe', 'ThS. Nguyễn Công Tùng', 'LECTURER', 'ACTIVE'),
(3, 'sv_2310900051', '2310900051@student.edu.vn', '0987654321', '$2a$10$7Z2eU26W3q4G3lA4T8Q4sOFoD206Oiqe5qZ00mZ6sO82O2tX.kZfe', 'Nguyễn Công Tùng (Sinh viên)', 'STUDENT', 'ACTIVE'),
(4, 'sv_2310900052', '2310900052@student.edu.vn', '0987654322', '$2a$10$7Z2eU26W3q4G3lA4T8Q4sOFoD206Oiqe5qZ00mZ6sO82O2tX.kZfe', 'Trần Thị Mai', 'STUDENT', 'ACTIVE');

-- 4. Thông tin Giảng viên & Sinh viên
INSERT INTO `lecturers` (`user_id`, `lecturer_code`, `faculty_id`, `academic_degree`, `bio`) VALUES
(2, 'GV001', 1, 'Thạc sĩ Khoa học Máy tính', 'Chuyên gia giảng dạy Kiến trúc Vi dịch vụ Spring Boot và AI');

INSERT INTO `students` (`user_id`, `student_code`, `class_id`, `major_id`, `date_of_birth`, `gender`, `face_registered`, `face_data_id`) VALUES
(3, '2310900051', 1, 1, '2005-01-01', 'MALE', TRUE, 'FACE_2310900051_VEC'),
(4, '2310900052', 1, 1, '2005-05-15', 'FEMALE', FALSE, NULL);

-- 5. Môn học & Khóa học
INSERT INTO `subjects` (`id`, `subject_code`, `name`, `credits`, `description`) VALUES
(1, 'INT3306', 'Phát triển ứng dụng Web chuyên sâu', 3, 'Học về Spring Boot, RESTful API, Microservices và Frontend hiện đại'),
(2, 'INT3401', 'Trí tuệ Nhân tạo & Thị giác Máy tính', 3, 'OpenCV, Nhận diện khuôn mặt ArcFace, PyTorch/ONNX');

INSERT INTO `courses` (`id`, `subject_id`, `lecturer_id`, `title`, `slug`, `description`, `thumbnail_url`, `price`, `is_free`, `enrollment_type`, `status`) VALUES
(1, 1, 2, 'Khóa học Spring Boot 3 & Microservice Fullstack', 'spring-boot-3-fullstack', 'Khóa học hoàn chỉnh xây dựng hệ thống quản lý học tập LMS.', '/uploads/thumbnails/springboot.png', 0.00, FALSE, 'ASSIGNED_BY_MAJOR', 'PUBLISHED'),
(2, 2, 2, 'Chuyên đề Thị giác máy tính với Python & FastAPI', 'fastapi-computer-vision', 'Ứng dụng ArcFace và YuNet vào giám sát thi trực tuyến.', '/uploads/thumbnails/fastapi.png', 499000.00, FALSE, 'PURCHASED', 'PUBLISHED');

-- 6. Phân bổ khóa học theo chuyên ngành (Cấp tự động cho ngành SE)
INSERT INTO `course_majors` (`course_id`, `major_id`, `is_mandatory`) VALUES
(1, 1, TRUE); -- Khóa Spring Boot tự động cấp cho sinh viên ngành Kỹ thuật Phần mềm (SE)

-- 7. Cấp quyền truy cập khóa học cho sinh viên 2310900051
INSERT INTO `course_enrollments` (`student_id`, `course_id`, `grant_type`, `status`) VALUES
(3, 1, 'ASSIGNED_BY_MAJOR', 'ACTIVE');

-- 8. Chương mục & Bài học & Video
INSERT INTO `course_chapters` (`id`, `course_id`, `title`, `order_index`) VALUES
(1, 1, 'Chương 1: Thiết kế Cơ sở Dữ liệu & Kiến trúc Hệ thống', 1),
(2, 1, 'Chương 2: Xây dựng RESTful API với Spring Boot 3', 2);

INSERT INTO `lessons` (`id`, `chapter_id`, `title`, `lesson_type`, `order_index`, `duration_seconds`, `is_preview`) VALUES
(1, 1, 'Bài 1: Phân tích Thực thể & Thiết kế CSDL LMS', 'VIDEO', 1, 1800, TRUE),
(2, 1, 'Bài 2: Hướng dẫn cấu hình Video Streaming với HLS m3u8', 'VIDEO', 2, 2400, FALSE);

INSERT INTO `video_assets` (`lesson_id`, `original_file_name`, `storage_provider`, `hls_playlist_url`, `duration_seconds`, `resolution`, `encode_status`) VALUES
(1, 'lesson1_db_design.mp4', 'LOCAL', '/videos/hls/lesson1/master.m3u8', 1800, '1080p', 'READY'),
(2, 'lesson2_hls_stream.mp4', 'LOCAL', '/videos/hls/lesson2/master.m3u8', 2400, '1080p', 'READY');

-- 9. Ngân hàng câu hỏi trắc nghiệm & Đề thi
INSERT INTO `question_bank` (`id`, `subject_id`, `creator_id`, `question_text`, `question_type`, `difficulty`, `explanation`, `default_point`) VALUES
(1, 1, 2, 'Trong kiến trúc Spring Boot, Annotation nào dùng để đánh dấu một lớp là REST Controller?', 'SINGLE_CHOICE', 'EASY', '@RestController là kết hợp của @Controller và @ResponseBody', 2.50),
(2, 1, 2, 'Giao thức nào thường được sử dụng phổ biến nhất để truyền phát video thích ứng băng thông trên Web?', 'SINGLE_CHOICE', 'EASY', 'HLS (HTTP Live Streaming) và MPEG-DASH là hai giao thức thích ứng băng thông phổ biến nhất.', 2.50);

INSERT INTO `question_options` (`id`, `question_id`, `option_text`, `is_correct`, `order_index`) VALUES
(1, 1, '@Controller', FALSE, 1),
(2, 1, '@RestController', TRUE, 2),
(3, 1, '@Service', FALSE, 3),
(4, 1, '@Component', FALSE, 4),
(5, 2, 'FTP', FALSE, 1),
(6, 2, 'HLS (HTTP Live Streaming)', TRUE, 2),
(7, 2, 'SMTP', FALSE, 3),
(8, 2, 'Telnet', FALSE, 4);

INSERT INTO `exams` (`id`, `course_id`, `title`, `description`, `duration_minutes`, `pass_percentage`, `max_attempts`, `shuffle_questions`, `shuffle_options`, `require_face_verification`, `tab_lock_enabled`, `max_violations_allowed`, `status`) VALUES
(1, 1, 'Bài kiểm tra trắc nghiệm số 1 - Khảo sát kiến trúc', 'Thí sinh bật webcam để xác thực khuôn mặt. Nghiêm cấm chuyển tab hoặc mở F12.', 15, 60, 2, TRUE, TRUE, TRUE, TRUE, 3, 'PUBLISHED');

INSERT INTO `exam_questions` (`exam_id`, `question_id`, `custom_point`, `order_index`) VALUES
(1, 1, 5.00, 1),
(1, 2, 5.00, 2);
