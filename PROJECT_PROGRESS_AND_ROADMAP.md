# 🎓 ĐỒ ÁN TỐT NGHIỆP: HỆ THỐNG QUẢN LÝ HỌC TẬP TRỰC TUYẾN (LMS)

## BÁO CÁO TIẾN ĐỘ THỰC HIỆN & LỘ TRÌNH PHÁT TRIỂN TIẾP THEO

*Cập nhật ngày: 17/10/2026*  
*Mã dự án: DATN_LMS_2026*  
*Mã nguồn GitHub: [ai-lms-face-recognition](https://github.com/CongTung202/ai-lms-face-recognition.git)*

---

### 📌 I. KIẾN TRÚC KỸ THUẬT & HẠ TẦNG (TECH STACK)

| Thành phần | Công nghệ / Dịch vụ | Thông số kết nối & Cổng | Vai trò |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Java Spring Boot 3.2.5 (JDK 17) | Cổng `8080`, Context `/` & `/api` | RESTful API, Security, Quản lý nghiệp vụ chính |
| **Admin Portal UI** | Component-Driven Architecture (Vanilla JS + Tailwind CSS v3) | `http://localhost:8080/admin/index.html` | Dashboard điều khiển, Header & Sidebar tái sử dụng, Quản lý phân hệ chuyên biệt |
| **Testing UI (Modular)** | HTML5 + Component Shared Engine | `d:\DATN\admin_web\` | Bản sao giao diện tĩnh độc lập phục vụ kiểm thử và deploy tĩnh |
| **RDBMS** | MySQL 8.0 | `localhost:3306`, DB: `lms_online_db` | Dữ liệu quan hệ 3NF (Người dùng, Khoa, Lớp, Học viên, Khóa học, Bài học, Giao dịch) |
| **NoSQL** | MongoDB | `localhost:27017`, DB: `lms_nosql_db` | Lưu trữ toàn văn tài liệu RAG (`course_documents`), Chunks vector (`document_chunks`), Log thi (`exam_telemetry_logs`) |
| **AI Engine** | Python FastAPI | Cổng `8001` (`face_ai`) | Nhận diện khuôn mặt ArcFace, Head-Pose 3D, Liveness detection |
| **Cloud Storage** | Cloudinary CDN | Cloud: `dhefmthim` | Lưu ảnh nhận diện khuôn mặt sinh viên, Video bài giảng phát toàn cầu |
| **Payment Gateway** | PayOS VietQR | API Key + Checksum cấu hình sẵn | Thanh toán học phí/mua khóa học tự động qua mã QR Ngân hàng |

---

### 🚀 II. TỔNG KẾT CÁC PHÂN HỆ ĐÃ HOÀN THÀNH

#### 1. Thiết Kế Cơ Sở Dữ Liệu MySQL Chuẩn Hóa 3NF & Cập Nhật Quan Hệ Phân Cấp

- **Tệp nguồn**: [`d:\DATN\database\lms_schema.sql`](file:///d:/DATN/database/lms_schema.sql)
- **Quy mô**: 24 bảng dữ liệu quan hệ chuẩn hóa 3NF, đầy đủ khóa ngoại, chỉ mục hiệu năng (INDEX) và các giá trị ENUM.
- **Chuẩn hóa phân cấp mới**:
  - Tinh gọn mô hình đào tạo theo thực tế: **Khoa (Faculty) $\rightarrow$ Lớp Học (Class) $\rightarrow$ Sinh Viên (Student)**.
  - Bỏ hẳn sự phụ thuộc vào Chuyên ngành (`majors`).
  - Thiết lập khóa ngoại liên kết trực tiếp: `classes.faculty_id -> faculties.id`.
  - Thiết lập liên kết: `students.class_id -> classes.id` và `students.faculty_id -> faculties.id`.

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

#### 7. Tái Cấu Trúc Kiến Trúc Giao Diện Quản Trị (Component-Driven Architecture)

- **Tối ưu trang Dashboard chính ([`admin/index.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/index.html))**:
  - Loại bỏ hoàn toàn việc nhồi nhét tất cả các bảng quản trị vào trang chủ gây nặng code.
  - Trang index được quy hoạch chuyên biệt thành **Bảng điều khiển Analytics tổng quan**: Hiển thị thẻ chỉ số thống kê (Sinh viên, Giảng viên, Khóa học, Doanh thu, Khoa, Lớp), biểu đồ tăng trưởng, hoạt động gần đây và lối tắt truy cập nhanh.
- **Tách Component Header & Sidebar Tái Sử Dụng ([`components/`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/components/))**:
  - [`header.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/components/header.html): Thanh điều hướng trên cùng, tìm kiếm nhanh `Ctrl+K`, thông báo và thông tin quản trị viên.
  - [`sidebar.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/components/sidebar.html): Cột điều hướng dọc phân chia nhóm chức năng (Dashboard, Khoa & Lớp, Khóa học & Video, Học viên & Giảng viên, Đơn hàng, Hệ thống).
  - [`admin-components.js`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/assets/js/admin-components.js): Tự động nạp Header & Sidebar không đồng bộ, tự động highlight mục menu đang kích hoạt dựa trên thuộc tính `data-nav` của thẻ `<body>`, và cung cấp hàm `showToast()` dùng chung.
- **Nguyên tắc "Không Trùng Lặp Modal" & Tách File Nghiệp Vụ Chuyên Biệt**:
  - Các trang chính (`list.html`) chỉ tập trung vào hiển thị danh sách, phân trang, tìm kiếm và lọc.
  - Các thao tác can thiệp dữ liệu được tách thành các file chuyên trách:
    - `create.html`: Biểu mẫu thêm mới dữ liệu.
    - `edit.html?id=...`: Biểu mẫu chỉnh sửa dữ liệu theo ID.
    - `delete.html?id=...`: Trang xác nhận xóa an toàn kèm thông tin tóm tắt và kiểm tra ràng buộc.
    - `details.html?id=...`: Trang xem chi tiết sâu và quản lý các thành phần con.

#### 8. Phân Hệ Quản Lý Khoa & Lớp Học (Bỏ Hẳn Majors, Quản Lý Sinh Viên Trong Lớp)

- **Loại bỏ hoàn toàn Chuyên ngành trực thuộc (Majors)**:
  - Gỡ bỏ hoàn toàn cột Chuyên ngành trong [`faculties/list.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/faculties/list.html).
  - Gỡ bỏ trường nhập `facultyMajorsInput` và payload xử lý trong cả [`faculties/create.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/faculties/create.html) và [`faculties/edit.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/faculties/edit.html).
  - Lớp học trực tiếp gắn với Khoa Đào tạo tại [`classes/create.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/classes/create.html) và [`classes/edit.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/classes/edit.html).
- **Phân hệ Thêm & Quản lý Sinh viên vào Lớp học**:
  - **Backend APIs tại [`AdminDataController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/AdminDataController.java)**:
    - `GET /api/admin/classes/{id}/students`: Danh sách toàn bộ sinh viên trong lớp (MSSV, Họ tên, Email, SĐT, Trạng thái Face eKYC).
    - `GET /api/admin/classes/{id}/available-students`: Danh sách các sinh viên khả dụng trong hệ thống chưa thuộc lớp này để sẵn sàng thêm vào.
    - `POST /api/admin/classes/{id}/students`: Thêm sinh viên vào lớp theo `studentId` hoặc `userId`.
    - `DELETE /api/admin/classes/{id}/students/{studentId}`: Xóa sinh viên khỏi lớp an toàn (`class_id = null`), không ảnh hưởng đến tài khoản người dùng.
  - **Trang Chi Tiết & Quản Lý Sinh Viên ([`classes/details.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/classes/details.html))**:
    - Banner lớp học: Hiển thị Mã lớp, Tên lớp, Khoa đào tạo, Niên khóa, GVCN và Tổng sĩ số sinh viên.
    - Bảng danh sách sinh viên lớp học kèm trạng thái eKYC và nút *"Xóa khỏi lớp"* an toàn.
    - Tìm kiếm tức thì theo MSSV, Tên hoặc Email.
    - Nút & Modal **`+ Thêm Sinh Viên Vào Lớp`**: Tìm kiếm sinh viên khả dụng và thêm vào lớp tức thì chỉ với **1 cú click**. Sĩ số và danh sách tự động cập nhật ngay trên giao diện.
  - **Nâng cấp [`classes/list.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/classes/list.html)**:
    - Cột Sĩ số và nút hành động **`Quản lý SV`** chuyển trực tiếp tới trang `details.html?id=...`.

#### 9. Cổng Thông Tin & Ôn Tập Trực Tuyến Sinh Viên (Student Portal - Phong Cách Đại Học Nguyễn Trãi - NTU)

- **Cảm hứng & Ngôn ngữ thiết kế**:
  - Lấy cảm hứng từ cấu trúc, nhận diện và chuẩn mực của **Trường Đại học Nguyễn Trãi ([daihocnguyentrai.edu.vn](https://daihocnguyentrai.edu.vn/))**.
  - Triết lý cốt lõi: *"Sinh viên là số 1 — Đại học ứng dụng, khát vọng khởi nghiệp"*.
  - Gam màu chủ đạo: Xanh Dương Hoàng Gia NTU (`#00539F`), Đỏ Nhiệt Huyết NTU (`#DA291C`), Cam Khởi Nghiệp (`#F37021`), điểm xuyết trắng xám thanh lịch hiện đại.
  - Phông chữ quốc tế hiện đại: `Plus Jakarta Sans` / `Montserrat` cho tiêu đề và nội dung.
- **Tệp nguồn triển khai**:
  - Trang chủ cổng sinh viên: [`portal/index.html`](file:///d:/DATN/lms_backend/src/main/resources/static/portal/index.html) (và bản sao độc lập tại [`student_portal/index.html`](file:///d:/DATN/student_portal/index.html)).
  - Dữ liệu mô phỏng & thông tin trường: [`portal/data/portal-data.json`](file:///d:/DATN/lms_backend/src/main/resources/static/portal/data/portal-data.json) & [`landing-data.json`](file:///d:/DATN/landing-data.json).
  - Điều hướng Spring Boot: [`WebViewController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/WebViewController.java) (tự động route `/`, `/portal`, `/student` tới Cổng Sinh Viên).
- **Cấu trúc & Tính năng nổi bật**:
  - **Top Utility Bar**: Hotline tuyển sinh `0981 924 302 - 098 192 26 41`, cơ sở đào tạo Tòa nhà 28A Lê Trọng Tấn, La Khê, Hà Đông, Hà Nội, liên kết nhanh website chính thức `daihocnguyentrai.edu.vn`.
  - **Main Navigation Bar**: Logo Đại học Nguyễn Trãi (NTU Smart LMS) sắc nét, menu phân tầng đa cấp và nút "CỔNG ÔN TẬP / ĐĂNG NHẬP" nổi bật.
  - **Hero Interactive Slider**: Khẩu hiệu lớn *"SINH VIÊN LÀ SỐ 1 - KHÁT VỌNG THANH XUÂN, KHÁT VỌNG KHỞI NGHIỆP CÙNG TRÍ TUỆ NHÂN TẠO"*, nút CTA dẫn tới học phần ôn tập và điểm danh FaceID.
  - **Quick Portal Services Bar**: Phím tắt tiện ích nhanh (Cổng Ôn Tập, Điểm danh Face eKYC ArcFace, Trợ lý AI RAG Giáo trình, Lịch học & thi, Nộp học phí tự động VietQR Napas247, Hỗ trợ học vụ NTU).
  - **Kho Học Liệu & Khoa Đào Tạo NTU**:
    - Khoa Công nghệ Thông tin, Khoa Mỹ thuật Ứng dụng & Thiết kế, Khoa Kinh tế & Quản trị Kinh doanh, Khoa Ngôn ngữ & Văn hóa Quốc tế.
    - Tìm kiếm tức thì theo tên môn học, mã học phần, tên giảng viên.
    - Thẻ học phần hiển thị số tín chỉ, giảng viên, thời lượng, đánh giá 5 sao.
  - **Modal Chi Tiết Đề Cương Ôn Tập**: Hiển thị mô tả học phần, giảng viên phụ trách và 5 chương nội dung ôn tập chi tiết.
  - **Modal Đăng Nhập Sinh Viên & Thử Nghiệm Nhanh**:
    - Cung cấp tính năng 1-Click Login với tài khoản sinh viên mẫu NTU (`student_tung`, `student_mai`).
    - Nhập thủ công MSSV để vào phòng ôn tập.
    - Chuyển đổi trạng thái đăng nhập, hiển thị thông tin sinh viên trên thanh tiêu đề và ghi nhớ phiên học tập bằng `localStorage`.
  - **Khối Tin Tức - Sự Kiện & Đội Ngũ Giảng Viên**: Tin thông báo học vụ, lịch thi, hội thảo công nghệ và giới thiệu các Giáo sư, Tiến sĩ tiêu biểu.

#### 10. Không Gian Phòng Học & Ôn Tập Trực Tuyến Sinh Viên (study.html)

- **Trải nghiệm học tập mở (Tạm thời không bắt buộc đăng nhập)**:
  - Cho phép người học, sinh viên và giảng viên truy cập trực tiếp để trải nghiệm toàn diện hệ thống.
  - Tự động nạp danh sách toàn bộ khóa học thực tế đang có trong CSDL qua `GET /api/admin/courses`.
  - Bộ chọn nhanh khóa học (Course Switcher Dropdown) trên thanh Header để chuyển đổi qua lại giữa các môn học tức thì.
- **Tệp nguồn triển khai**:
  - Giao diện: [`portal/study.html`](file:///d:/DATN/lms_backend/src/main/resources/static/portal/study.html) (và bản sao độc lập tại [`student_portal/study.html`](file:///d:/DATN/student_portal/study.html)).
  - Điều hướng: Route `/study` và `/portal/study` tại [`WebViewController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/WebViewController.java).
- **Tính năng không gian học tập**:
  - **Trình phát Video chuẩn HTML5**: Tự động nhận diện bài giảng có video (`videoAsset`), phát video Cloudinary CDN hoặc stream HLS m3u8.
  - **Điều hướng bài học thông minh**: Nút "Bài trước", "Bài tiếp theo" và nút "Đánh dấu đã học" cập nhật thanh tiến độ % thời gian thực.
  - **Tabs Học Tập Tương Tác**:
    - **Tab Tổng quan & Ghi chú**: Mục tiêu học phần, lưu ý về thuật toán chống tua gian lận (ngưỡng nhịp học 15s).
    - **Tab Trợ Lý AI RAG Giáo Trình (<10ms)**: Khung chatbox hỏi đáp ngữ nghĩa tức thì, kết nối trực tiếp endpoint `/api/rag/query`, có sẵn các nút câu hỏi gợi ý nhanh.
    - **Tab Tài liệu đính kèm**: Liệt kê toàn bộ file PDF/DOCX của môn học lưu trong CSDL kèm nút tải/xem.
    - **Tab Đề Ôn Tập Trắc Nghiệm**: Bộ 3 câu hỏi trắc nghiệm củng cố kiến thức, có chấm điểm và hiển thị giải thích chi tiết đáp án.
  - **Cột Đề Cương Bài Học (Accordion Curriculum Sidebar)**: Liệt kê chi tiết từng Chương (Chapters) và các bài học con (Lessons), đánh dấu tick xanh khi hoàn thành.

#### 11. Tích Hợp Upload Ảnh Bìa Khóa Học Từ Máy Tính Lên Cloudinary CDN Toàn Cầu

- **Mục tiêu**: Cho phép quản trị viên và giảng viên tải ảnh bìa môn học trực tiếp từ máy tính lên Cloudinary CDN toàn cầu, loại bỏ sự phụ thuộc vào link ảnh ngoại vi không ổn định.
- **Backend Service & Controller**:
  - [`CloudinaryVideoService.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/service/CloudinaryVideoService.java):
    - Bổ sung hàm `uploadImageToCloudinary(MultipartFile file, String folder)`: Tự động chuẩn hóa tên tệp, sinh `public_id` độc nhất và đẩy trực tiếp lên Cloudinary thư mục `lms_datn/thumbnails`, trả về URL an toàn (HTTPS).
  - [`AdminDataController.java`](file:///d:/DATN/lms_backend/src/main/java/com/datn/lms/controller/AdminDataController.java):
    - `POST /api/admin/courses/upload-thumbnail`: Endpoint tải ảnh bìa độc lập phục vụ form tạo mới khóa học.
    - `POST /api/admin/courses/{id}/thumbnail`: Endpoint tải ảnh bìa và cập nhật ngay lập tức vào thuộc tính `thumbnailUrl` của khóa học trong MySQL.
- **Giao diện người dùng (Course Thumbnail Uploader)**:
  - Triển khai đồng bộ trên:
    - [`admin/courses/create.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/courses/create.html)
    - [`admin/courses/edit.html`](file:///d:/DATN/lms_backend/src/main/resources/static/admin/courses/edit.html)
    - Và thư mục tĩnh độc lập [`admin_web/courses/`](file:///d:/DATN/admin_web/courses/).
  - **Tính năng nổi bật của Widget Upload Ảnh Bìa**:
    - **Khung xem trước trực tiếp (Live Preview)**: Tỷ lệ 16:9 sắc nét, bo góc mềm mại, hiển thị ngay ảnh khi chọn tệp hoặc nhập URL.
    - **Huy hiệu Cloudinary CDN Ready**: Tự động nhận diện link Cloudinary và hiển thị chấm xanh phát sáng.
    - **Vùng kéo thả (Dropzone) & Chọn tệp**: Hỗ trợ kéo thả ảnh từ File Explorer hoặc click mở hộp thoại chọn tệp (hỗ trợ JPG, PNG, WEBP, GIF).
    - **Thanh trạng thái tải lên**: Hiển thị hiệu ứng xoay spinner và tên file đang truyền tải lên Cloudinary.
    - **Tự động điền URL**: Sau khi upload thành công, đường dẫn Cloudinary tự động được gán vào trường `thumbnailUrl` để sẵn sàng lưu vào CSDL.

---

### 📋 III. CÁC TỆP SCRIPT ĐIỀU KHIỂN HỆ THỐNG ĐÃ CÓ

- [`d:\DATN\start_all.bat`](file:///d:/DATN/start_all.bat): **BẢNG ĐIỀU KHIỂN & KHỞI CHẠY TẤT CẢ** — Tự động dọn dẹp port 8001 & 8080, kích hoạt song song 2 dịch vụ (Python Face AI + Spring Boot) trong 2 cửa sổ riêng và mở Dashboard điều khiển trực quan với các phím tắt nhanh.
- [`d:\DATN\stop_all.bat`](file:///d:/DATN/stop_all.bat): Dừng an toàn toàn bộ tiến trình Backend và Face AI, giải phóng hoàn toàn cổng 8080 và 8001.
- [`d:\DATN\run_backend.bat`](file:///d:/DATN/run_backend.bat): Khởi chạy / Khởi động lại Spring Boot 3 Backend (Cổng 8080, tự động kill PID cũ tránh lỗi Port in use).
- [`d:\DATN\start_face_ai.bat`](file:///d:/DATN/start_face_ai.bat): Khởi chạy / Khởi động lại Python Face AI Microservice (Cổng 8001, tự động giải phóng port 8001).
- [`d:\DATN\test_video_player.html`](file:///d:/DATN/test_video_player.html): Web kiểm thử Video Streaming & Chống tua.
- [`d:\DATN\test_rag_chatbot.html`](file:///d:/DATN/test_rag_chatbot.html): Web kiểm thử Trợ lý AI RAG hỏi đáp tài liệu.

---

### 🎯 IV. LỘ TRÌNH TRIỂN KHAI CÁC PHẦN TIẾP THEO (ROADMAP)

Sơ đồ tổng thể các chặng đường phát triển tiếp theo của dự án:

```
[ ĐÃ HOÀN TẤT: CSDL 3NF, Dual Database, Face AI, Video Learning, RAG, Dashboard Admin, Student Portal NTU & Phòng Ôn Tập study.html ]
                                       │
                                       ▼
                 ┌─────────────────────────────────────────┐
                 │ 1. PHÂN HỆ THANH TOÁN TỰ ĐỘNG PAYOS     │
                 │    • Tạo link thanh toán VietQR động    │
                 │    • Nhận Webhook từ PayOS tự động mở   │
                 │      khóa học cho sinh viên             │
                 │    • Quản lý đơn hàng (orders) &        │
                 │      lịch sử giao dịch (enrollments)    │
                 └─────────────────────────────────────────┘
                                       │
                                       ▼
                 ┌─────────────────────────────────────────┐
                 │ 2. PHÂN HỆ GIÁM SÁT THI TRỰC TUYẾN      │
                 │    • WebSocket kết nối phòng thi thời   │
                 │      gian thực                          │
                 │    • AI phát hiện gian lận: Rời màn     │
                 │      hình, quay đầu trái/phải quá lâu   │
                 │    • Ghi telemetry log vi phạm MongoDB  │
                 └─────────────────────────────────────────┘
```

#### Chi tiết các công việc ưu tiên tiếp theo

1. **Phân Hệ Phòng Ôn Tập Sinh Viên (Student Learning Space)**:
   - Gắn kết trang Student Portal với phòng học trực tuyến.
   - Khi bấm "Vào Ôn Tập", mở màn hình xem bài giảng video tương tác, tải tài liệu PDF và cửa sổ chatbox Trợ lý AI RAG giải đáp giáo trình.

2. **Phân hệ Thanh toán Mua Khóa Học Tự Động với PayOS VietQR**:
   - `PayOSService.java`: Tạo link thanh toán VietQR và xác thực chữ ký Webhook (HMAC-SHA256).
   - `OrderController.java`:
     - `POST /api/orders/create`: Sinh mã đơn hàng và link thanh toán VietQR.
     - `POST /api/payment/payos-webhook`: Xử lý Webhook khi thanh toán thành công, tự động mở khóa học trong `course_enrollments`.
   - Giao diện quản lý đơn hàng (`orders/list.html`, `orders/details.html`).

2. **Phân hệ Giám Sát Thi Trực Tuyến Bằng Face AI**:
   - Cấu hình Spring WebSocket STOMP (`/ws/exam-proctoring`).
   - Kết nối webcam học viên gửi frame liên tục về Face AI Engine.
   - Ghi telemetry logs các sự kiện gian lận vào MongoDB (`exam_telemetry_logs`).

3. **Cổng Học Viên (Student Learning Portal)**:
   - Giao diện đăng nhập, quét mặt điểm danh eKYC.
   - Không gian học tập cá nhân, theo dõi tiến độ bài học.
