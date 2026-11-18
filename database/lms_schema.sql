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
    `face_embed` LONGTEXT COMMENT 'Chuỗi Base64 vector khuôn mặt đã mã hoá AES-256 (3 góc ArcFace 512D)',
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
    `start_date` DATE NULL COMMENT 'Ngày bắt đầu khóa học theo niên lịch',
    `end_date` DATE NULL COMMENT 'Ngày kết thúc khóa học',
    `is_sequential_learning` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bắt buộc học viên phải học tuần tự từng bài',
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
    `scheduled_date` DATE NULL COMMENT 'Ngày học dự kiến theo thời khóa biểu',
    `unlock_at` DATETIME NULL COMMENT 'Mốc ngày giờ mở bài học',
    `prerequisite_lesson_id` BIGINT NULL COMMENT 'Bài học tiên quyết bắt buộc hoàn thành trước',
    `is_sequential` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bật chế độ khóa tuần tự đối với bài này',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lessons_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `course_chapters` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_lessons_prerequisite` FOREIGN KEY (`prerequisite_lesson_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_lessons_order` (`chapter_id`, `order_index`),
    INDEX `idx_lessons_schedule` (`scheduled_date`, `unlock_at`)
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

-- 3.5. Bảng Quản lý Bài thi Trắc nghiệm sau bài học (Quizzes & Questions)
-- Chuẩn hoá 3NF hoàn chỉnh: Tách biệt đề quiz, câu hỏi, các lựa chọn và lịch sử làm bài
DROP TABLE IF EXISTS `quiz_attempt_answers`;
DROP TABLE IF EXISTS `quiz_attempts`;
DROP TABLE IF EXISTS `quiz_options`;
DROP TABLE IF EXISTS `quiz_questions`;
DROP TABLE IF EXISTS `quizzes`;

CREATE TABLE `quizzes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lesson_id` BIGINT NOT NULL UNIQUE COMMENT 'Gắn liền 1-1 với bài học tương ứng',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề bài trắc nghiệm',
    `description` TEXT COMMENT 'Hướng dẫn và lưu ý khi làm bài kiểm tra',
    `time_limit_minutes` INT NOT NULL DEFAULT 15 COMMENT 'Thời gian làm bài tối đa (phút)',
    `passing_score_percentage` DECIMAL(5,2) NOT NULL DEFAULT 80.00 COMMENT 'Ngưỡng điểm tối thiểu để đạt (%)',
    `max_attempts` INT NOT NULL DEFAULT 5 COMMENT 'Số lần làm bài tối đa cho phép',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Kích hoạt bài quiz',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_quizzes_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục bài thi trắc nghiệm sau bài học';

CREATE TABLE `quiz_questions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL COMMENT 'Thuộc bài quiz nào',
    `question_text` TEXT NOT NULL COMMENT 'Nội dung câu hỏi',
    `question_type` ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE') NOT NULL DEFAULT 'SINGLE_CHOICE',
    `explanation` TEXT COMMENT 'Giải thích đáp án chi tiết sau khi nộp bài',
    `order_index` INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự câu hỏi trong đề',
    `points` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT 'Thang điểm câu',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_questions_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_questions_quiz` (`quiz_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ngân hàng câu hỏi trắc nghiệm';

CREATE TABLE `quiz_options` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL COMMENT 'Thuộc câu hỏi nào',
    `option_text` TEXT NOT NULL COMMENT 'Nội dung phương án',
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đáp án đúng',
    `order_index` INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự lựa chọn (A, B, C, D)',
    CONSTRAINT `fk_options_question` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_options_question` (`question_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Các phương án trả lời trắc nghiệm (A, B, C, D)';

CREATE TABLE `quiz_attempts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL COMMENT 'Thuộc đề quiz nào',
    `student_id` BIGINT NOT NULL COMMENT 'UID học viên nộp bài',
    `lesson_id` BIGINT NOT NULL COMMENT 'Bài học tương ứng',
    `attempt_number` INT NOT NULL DEFAULT 1 COMMENT 'Lần thi thứ mấy',
    `score_percentage` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Điểm số đạt được (thang 100%)',
    `total_questions` INT NOT NULL DEFAULT 0 COMMENT 'Tổng số câu',
    `correct_answers` INT NOT NULL DEFAULT 0 COMMENT 'Số câu trả lời đúng',
    `is_passed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đạt bài kiểm tra hay không',
    `time_spent_seconds` INT NOT NULL DEFAULT 0 COMMENT 'Thời gian làm bài thực tế (giây)',
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm nộp bài',
    CONSTRAINT `fk_attempts_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_attempts_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_attempts_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_attempt_student` (`student_id`, `lesson_id`, `is_passed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử các lần làm bài trắc nghiệm của sinh viên';

CREATE TABLE `quiz_attempt_answers` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` BIGINT NOT NULL COMMENT 'Thuộc lượt làm bài nào',
    `question_id` BIGINT NOT NULL COMMENT 'Câu hỏi',
    `selected_option_id` BIGINT NULL COMMENT 'Phương án sinh viên đã chọn',
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Lựa chọn có chính xác không',
    CONSTRAINT `fk_att_ans_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `quiz_attempts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_att_ans_question` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_att_ans_option` FOREIGN KEY (`selected_option_id`) REFERENCES `quiz_options` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết từng câu trả lời trong lượt làm bài quiz';

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
    `quiz_passed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã thi đạt bài trắc nghiệm sau bài học chưa',
    `quiz_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Điểm trắc nghiệm cao nhất đạt được (%)',
    `quiz_attempts_count` INT NOT NULL DEFAULT 0 COMMENT 'Số lần đã làm bài trắc nghiệm',
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

-- 4.3. Bảng Tin tức, Thông báo & Hoạt động Sinh viên NTU (Announcements)
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề thông báo / hoạt động',
    `category` VARCHAR(50) NOT NULL COMMENT 'Mã phân loại: startup, charity, sports, honor, ...',
    `category_name` VARCHAR(100) NOT NULL COMMENT 'Tên chuyên mục hiển thị',
    `badge_class` VARCHAR(50) DEFAULT 'badge-startup' COMMENT 'CSS class hiển thị huy hiệu',
    `summary` TEXT COMMENT 'Tóm tắt ngắn hiển thị trên thẻ',
    `content` LONGTEXT COMMENT 'Nội dung chi tiết thông báo',
    `image_url` VARCHAR(500) COMMENT 'Đường dẫn ảnh lưu trữ trên Cloudinary hoặc máy chủ',
    `cloudinary_public_id` VARCHAR(200) COMMENT 'Mã nhận diện ảnh trên Cloudinary',
    `event_date` VARCHAR(100) COMMENT 'Thời gian sự kiện hiển thị trên nhãn',
    `location` VARCHAR(255) COMMENT 'Địa điểm tổ chức sự kiện',
    `author` VARCHAR(100) DEFAULT 'Ban Truyền Thông NTU' COMMENT 'Tác giả bài đăng',
    `is_featured` BOOLEAN DEFAULT FALSE COMMENT 'Đánh dấu bài viết nổi bật',
    `status` ENUM('PUBLISHED', 'DRAFT', 'ARCHIVED') DEFAULT 'PUBLISHED' COMMENT 'Trạng thái bài đăng',
    `view_count` INT DEFAULT 0 COMMENT 'Lượt xem bài viết',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_ann_category` (`category`),
    INDEX `idx_ann_status` (`status`),
    INDEX `idx_ann_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý tin tức, thông báo và hoạt động ngoại khóa trường';

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

INSERT INTO `students` (`user_id`, `student_code`, `class_id`, `major_id`, `date_of_birth`, `gender`, `face_registered`, `face_embed`) VALUES
(3, '2310900051', 1, 1, '2005-01-01', 'MALE', FALSE, NULL),
(4, '2310900052', 1, 1, '2005-05-15', 'FEMALE', FALSE, NULL);

-- 5. Môn học & Khóa học
INSERT INTO `subjects` (`id`, `subject_code`, `name`, `credits`, `description`) VALUES
(1, 'INT3306', 'Phát triển ứng dụng Web chuyên sâu', 3, 'Học về Spring Boot, RESTful API, Microservices và Frontend hiện đại'),
(2, 'INT3401', 'Trí tuệ Nhân tạo & Thị giác Máy tính', 3, 'OpenCV, Nhận diện khuôn mặt ArcFace, PyTorch/ONNX');

INSERT INTO `courses` (`id`, `subject_id`, `lecturer_id`, `title`, `slug`, `description`, `thumbnail_url`, `price`, `is_free`, `enrollment_type`, `status`) VALUES
(1, 1, 2, 'Khóa học Spring Boot 3 & Microservice Fullstack', 'spring-boot-3-fullstack', 'Khóa học hoàn chỉnh xây dựng hệ thống quản lý học tập LMS.', '/uploads/thumbnails/springboot.png', 0.00, FALSE, 'ASSIGNED_BY_MAJOR', 'PUBLISHED'),
(2, 2, 2, 'Chuyên đề Thị giác máy tính với Python & FastAPI', 'fastapi-computer-vision', 'Ứng dụng ArcFace và YuNet vào nhận diện và điểm danh trực tuyến.', '/uploads/thumbnails/fastapi.png', 499000.00, FALSE, 'PURCHASED', 'PUBLISHED');

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

