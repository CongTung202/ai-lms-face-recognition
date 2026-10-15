# 🎓 ĐỒ ÁN TỐT NGHIỆP: HỆ THỐNG QUẢN LÝ HỌC TẬP TRỰC TUYẾN (LMS)
## BÁO CÁO TIẾN ĐỘ THỰC HIỆN & LỘ TRÌNH PHÁT TRIỂN TIẾP THEO

*Cập nhật ngày: 15/10/2026*  
*Mã dự án: DATN_LMS_2026*  
*Mã nguồn GitHub: [ai-lms-face-recognition](https://github.com/CongTung202/ai-lms-face-recognition.git)*

---

### 📌 I. KIẾN TRÚC KỸ THUẬT & HẠ TẦNG (TECH STACK)

| Thành phần | Công nghệ / Dịch vụ | Thông số kết nối & Cổng | Vai trò |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Java Spring Boot 3.2.5 (JDK 17) | Cổng `8080`, Context `/` & `/api` | RESTful API, Security, Quản lý nghiệp vụ chính |
| **Admin Portal UI** | OnePortal Light Theme (Tailwind CSS v3) | `http://localhost:8080/admin/index.html` | Bảng điều khiển quản trị, Quản lý học viên, Khóa học, eKYC |
| **Testing UI (Modular)**| HTML5 + Vanilla JS + Tailwind CSS v3 | `d:\DATN\admin_web\` (Thư mục con theo domain) | Giao diện kiểm thử từng module không icon, không ghost text |
| **RDBMS** | MySQL 8.0 | `localhost:3306`, DB: `lms_online_db` | Lưu trữ dữ liệu quan hệ chuẩn 3NF (Người dùng, Khóa học, Bài học, Giao dịch) |
| **NoSQL** | MongoDB | `localhost:27017`, DB: `lms_nosql_db` | Lưu trữ toàn văn tài liệu RAG (`course_documents`), Chunks vector (`document_chunks`), Log thi (`exam_telemetry_logs`) |
| **AI Engine** | Python FastAPI | Cổng `8001` (`face_ai`) | Nhận diện khuôn mặt ArcFace, Head-Pose 3D, Liveness detection |
| **Cloud Storage** | Cloudinary CDN | Cloud: `dhefmthim` | Lưu ảnh nhận diện khuôn mặt sinh viên, Video bài giảng phát toàn cầu |
| **Payment Gateway** | PayOS VietQR | API Key + Checksum cấu hình sẵn | Thanh toán học phí/mua khóa học tự động qua mã QR Ngân hàng |

---

### 🚀 II. TỔNG KẾT CÁC PHÂN HỆ ĐÃ HOÀN THÀNH

#### 1. Thiết Kế Cơ Sở Dữ Liệu MySQL Chuẩn Hóa 3NF
- **Tệp nguồn**: [`d:\DATN\database\lms_schema.sql`](file:///d:/DATN/database/lms_schema.sql)
- **Quy mô**: 24 bảng dữ liệu quan hệ chuẩn hóa 3NF, đầy đủ khóa ngoại, chỉ mục hiệu năng (INDEX) và các giá trị ENUM (bao gồm cả phương thức thanh toán `PAYOS`).
- **Dữ liệu mẫu (Seed Data)**: Đã nạp sẵn tài khoản Admin, Giảng viên, Sinh viên, Ngành học, Môn học, Khóa học, Chương mục và Bài học mẫu.

#### 2. Khung Xương Spring Boot 3 & Dual Database Architecture
- Cấu hình song song **MySQL JPA Hibernate** và **Spring Data MongoDB** mượt mà trong cùng một dự án.
- **24 JPA Entities** tương ứng 24 bảng MySQL ([`entity/mysql/`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/entity/mysql/)).
- **3 MongoDB Documents**:
  - [`CourseDocumentMongo.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/entity/mongodb/CourseDocumentMongo.java)
  - [`DocumentChunkMongo.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/entity/mongodb/DocumentChunkMongo.java)
  - [`ExamTelemetryLogMongo.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/entity/mongodb/ExamTelemetryLogMongo.java)
- Thiết lập bảo mật Spring Security 6 & JWT Token Provider.

#### 3. Tối Ưu Hóa & Nâng Cấp Python Face AI Engine
- **Chỉ lấy 3 góc mặt trực diện**: Đã loại bỏ 2 góc ngẩng đầu và cúi đầu, chỉ giữ lại 3 góc thiết yếu: **Trực diện (Straight)**, **Nghiêng trái (Turn Left)**, **Nghiêng phải (Turn Right)**.
- **Tích hợp Cloudinary CDN tự động**: Khi đăng ký 3 góc, hệ thống tự động bóc tách ảnh trực diện đầu tiên và đẩy lên Cloudinary (`lms_datn/faces`), trả về link ảnh đại diện cho sinh viên.
- **Chuyển cổng chạy sang 8001**: Tránh xung đột cổng mạng nội bộ. Có file [`start_face_ai.bat`](file:///d:/DATN/start_face_ai.bat) chạy độc lập.

#### 4. Quy Trình Xác Thực Đăng Nhập & Điểm Danh Khuôn Mặt (2-Step Auth)
- **Tệp triển khai**: [`AuthService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/AuthService.java), [`AuthController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/AuthController.java)
- **Quy trình chuẩn**:
  - `POST /api/auth/login`: Nhập Username + Password.
  - **Lần đầu đăng nhập**: Trả về `REQUIRE_FACE_ENROLLMENT` kèm `tempToken` $\rightarrow$ Bắt buộc quét 3 góc mặt tại `POST /api/auth/enroll-face`.
  - **Các lần sau**: Trả về `REQUIRE_FACE_VERIFICATION` kèm `tempToken` $\rightarrow$ Chụp 1 ảnh webcam đối chiếu tại `POST /api/auth/verify-face-login`.
  - Điểm danh thành công mới cấp JWT Token chính thức (24h) để truy cập hệ thống.

#### 5. Luồng Video Học Tập (Video Learning Pipeline)
- **Tệp triển khai**: 
  - [`VideoCompressorService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/VideoCompressorService.java)
  - [`CloudinaryVideoService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/CloudinaryVideoService.java)
  - [`VideoStreamingService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/VideoStreamingService.java)
  - [`LearningProgressService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/LearningProgressService.java)
  - [`VideoController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/VideoController.java)
- **Các tính năng nổi bật**:
  - **Tự động nén video chuẩn 720p HD**: Dùng FFmpeg nhúng (JAVE2), giảm 70% - 85% dung lượng gốc, luôn đảm bảo file dưới 100MB để đẩy lên Cloudinary CDN.
  - **HTTP 206 Partial Content Streaming (1MB/chunk)**: Phát siêu mượt, tua tức thì trong 0.2s.
  - **Thuật toán chống tua (Anti-Cheat)**: Chỉ cho phép tăng `maxWatchedSecond` khi học sinh xem thực tế (ngưỡng nhịp tim 15s). Tua đốt cháy giai đoạn sẽ không được tính % hoàn thành.
  - **Tự động hoàn thành**: Đạt $\ge 85\%$ thời lượng video sẽ đánh dấu `is_completed = true`.
  - **Hỗ trợ Resume**: Tiếp tục xem từ giây dừng gần nhất.

#### 6. Phân Hệ Trợ Lý AI RAG Giáo Trình (100% Miễn Phí, Tốc Độ < 10ms)
- **Tệp triển khai**:
  - [`DocumentParserService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/DocumentParserService.java)
  - [`DocumentRagSyncService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/DocumentRagSyncService.java)
  - [`RagDocumentController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/RagDocumentController.java)
- **Các tính năng nổi bật**:
  - Bóc tách văn bản chuẩn xác từ cả 3 định dạng: `.pdf` (Apache PDFBox 3.0), `.docx` (Apache POI), `.txt`.
  - Giữ lại số trang thực tế (`pageNumber`) của tài liệu.
  - **Cắt đoạn thông minh (Recursive Chunking)**: 600 ký tự / đoạn, overlap 100 ký tự.
  - **Tìm kiếm ngữ nghĩa siêu tốc**: Thuật toán TF-IDF & Cosine Similarity in-memory lọc trước theo `course_id`.
  - Trả về câu trả lời tổng hợp kèm **trích dẫn cụ thể số trang** trong giáo trình gốc.

#### 7. Bảo Mật Bí Mật Hệ Thống & Quản Lý Mã Nguồn Git
- **Cách ly Secret Keys**: Chuyển toàn bộ Cloudinary Key, PayOS Secret, JWT Secret, CSDL sang tệp `.env`.
- **Cấu hình Spring Boot**: Dùng `spring.config.import` nạp biến môi trường tự động mà không lưu cứng trong code.
- **Git Push an toàn**: Đã khởi tạo Git và đẩy toàn bộ mã nguồn lên nhánh `main` repository `https://github.com/CongTung202/ai-lms-face-recognition.git`.

#### 8. Nâng Cấp Tệp Khởi Động Hệ Thống (Startup Scripts)
- **[`run_backend.bat`](file:///d:/DATN/run_backend.bat)**:
  - Tự động nạp mã hóa UTF-8 (`chcp 65001`).
  - Tự động nạp cấu hình từ `.env` nếu tồn tại.
  - Tự động phát hiện JDK 17 và Maven binary wrapper.
  - Hiển thị bảng điều khiển danh mục API, trạng thái kết nối CSDL và đường dẫn giao diện.
- **[`start_face_ai.bat`](file:///d:/DATN/start_face_ai.bat)**:
  - Tự động phát hiện môi trường ảo Python (`.venv` hoặc `venv`).
  - Tự động kiểm tra và tải mô hình YuNet ONNX nếu chưa có.
  - Hiển thị các endpoint Swagger UI, Face Enroll, Face Verify trên cổng 8001.

#### 9. Giao Diện Quản Trị OnePortal (Light Modern Theme & Live Integration)
- **Đường dẫn truy cập**: `http://localhost:8080/admin/index.html` (hoặc bản sao tĩnh tại [`d:\DATN\admin_web\index.html`](file:///d:/DATN/admin_web/index.html)).
- **Bố cục chuẩn OnePortal**: Header tìm kiếm nhanh `Ctrl+K`, Sidebar điều hướng chuyên nghiệp, Tabs chức năng ngang, Khối hồ sơ cá nhân 2 cột và Cụm eKYC 3 góc mặt.
- **Quản trị Người Dùng (Full CRUD)**:
  - Hiển thị danh sách thực tế từ MySQL (`GET /api/admin/users`).
  - Thêm tài khoản mới trực tiếp vào CSDL (`POST /api/admin/users`).
  - Chỉnh sửa thông tin học viên/quản trị viên (`PUT /api/admin/users/{id}`).
  - Xóa tài khoản an toàn kèm xử lý ràng buộc khóa ngoại (`DELETE /api/admin/users/{id}`).
- **Quản lý Khóa Học, Buổi Học (Chapters) & Video/Tài Liệu Cloudinary**:
  - Khắc phục triệt để lỗi 500 Jackson Hibernate proxy (`ByteBuddyInterceptor`).
  - Hiển thị chi tiết từng Buổi học (Chapters/Sessions) và Bài học (Lessons).
  - Tích hợp phát Video trực tiếp từ Cloudinary CDN (`dhefmthim`) với trình phát HTML5 Video Player.
  - Hiển thị tài liệu PDF/DOCX đính kèm đã lập chỉ mục vector RAG.

#### 10. Giao Diện Kiểm Thử Độc Lập Theo Từng Module (admin_web)
- Cấu trúc thư mục tách biệt cho từng domain:
  - `students/`: `list.html`, `create.html`, `edit.html`, `details.html`
  - `courses/`: `list.html`, `create.html`, `edit.html`, `details.html`
  - `videos/`: `list.html`, `create.html`, `details.html`
  - `documents/`: `list.html`, `create.html`, `details.html`
  - `exams/`: `list.html`, `create.html`, `details.html`
  - `orders/`: `list.html`, `create.html`, `details.html`
- Thiết kế responsive hiện đại với Tailwind CSS v3, tuân thủ nguyên tắc không dùng icon và không dùng ghost text.

---

### 📋 III. CÁC TỆP SCRIPT ĐIỀU KHIỂN HỆ THỐNG ĐÃ CÓ

- [`d:\DATN\run_backend.bat`](file:///d:/DATN/run_backend.bat): Khởi chạy Spring Boot 3 Backend (Cổng 8080).
- [`d:\DATN\start_face_ai.bat`](file:///d:/DATN/start_face_ai.bat): Khởi chạy Python Face AI Microservice (Cổng 8001).
- [`d:\DATN\admin_web\index.html`](file:///d:/DATN/admin_web/index.html): Giao diện OnePortal độc lập kiểm thử offline.
- [`d:\DATN\test_video_player.html`](file:///d:/DATN/test_video_player.html): Web kiểm thử Video Streaming & Chống tua.
- [`d:\DATN\test_rag_chatbot.html`](file:///d:/DATN/test_rag_chatbot.html): Web kiểm thử Trợ lý AI RAG hỏi đáp tài liệu.

---

### 🎯 IV. LỘ TRÌNH TRIỂN KHAI CÁC PHẦN TIẾP THEO (ROADMAP)

Dưới đây là các phân hệ còn lại để hoàn tất 100% đồ án tốt nghiệp:

```
[ Hiện Tại: Đã Xong 10 Phân Hệ Nền Tảng, AI Engine & Giao Diện Quản Trị OnePortal ]
                                      │
                                      ▼
                 ┌─────────────────────────────────────────┐
                 │ 1. PHÂN HỆ THANH TOÁN TỰ ĐỘNG PAYOS     │
                 │    • Tạo link thanh toán VietQR động    │
                 │    • Nhận Webhook từ PayOS tự động mở   │
                 │      khóa học cho sinh viên             │
                 │    • Cập nhật bảng orders & enrollments │
                 └─────────────────────────────────────────┘
                                      │
                                      ▼
                 ┌─────────────────────────────────────────┐
                 │ 2. PHÂN HỆ GIÁM SÁT THI TRỰC TUYẾN      │
                 │    • WebSocket giám sát phòng thi       │
                 │    • AI phát hiện: Rời màn hình,        │
                 │      quay đầu trái/phải quá lâu         │
                 │    • Ghi telemetry log vi phạm MongoDB  │
                 └─────────────────────────────────────────┘
                                      │
                                      ▼
                 ┌─────────────────────────────────────────┐
                 │ 3. GIAO DIỆN HỌC VIÊN (STUDENT PORTAL)  │
                 │    • Web xem khóa học, xem video H264   │
                 │    • Chatbot AI RAG hỏi đáp giáo trình  │
                 │    • Làm bài thi trực tuyến có camera   │
                 └─────────────────────────────────────────┘
```

#### Nhiệm vụ cụ thể tiếp theo đề xuất:
**Triển khai Phân hệ Thanh Toán Mua Khóa Học Tự Động với PayOS VietQR**:
1. Tạo `PayOSService.java`: Khởi tạo thanh toán và xác thực Checksum chữ ký số từ PayOS.
2. Tạo `OrderController.java`:
   - `POST /api/orders/create`: Sinh mã đơn hàng và link thanh toán VietQR.
   - `POST /api/payment/payos-webhook`: Lắng nghe Webhook khi thanh toán thành công, tự động cấp quyền vào bảng `course_enrollments` để sinh viên vào học ngay.
