# CHƯƠNG 2. KHẢO SÁT VÀ PHÂN TÍCH HỆ THỐNG

Chương 2 tập trung khảo sát yêu cầu thực tế, phân tích nghiệp vụ và xây dựng mô hình chức năng cho đề tài **"Hệ thống học trực tuyến EduFace tích hợp AI nhận diện khuôn mặt"**. Nội dung chương bao gồm: xác định các chức năng mức cao của hệ thống; thiết lập các yêu cầu phi chức năng về hiệu năng và an toàn dữ liệu; phân tích chi tiết 4 nhóm tác nhân chính (**Khách vãng lai, Sinh viên, Giảng viên, Quản trị viên**); mô tả các luồng quy trình nghiệp vụ cốt lõi; và xây dựng ma trận phân quyền chức năng tương ứng nhằm định hình kiến trúc phần mềm cho các chương thiết kế tiếp theo.

---

## 2.1. Chức năng của hệ thống

Hệ thống học trực tuyến **EduFace** được xây dựng nhằm cung cấp một nền tảng quản lý đào tạo và học tập trực tuyến (LMS) toàn diện, tích hợp công nghệ Trí tuệ nhân tạo (AI) nhận diện khuôn mặt để tự động hóa khâu xác thực danh tính, điểm danh sinh viên và giám sát tính trung thực trong các kỳ thi trực tuyến.

### 2.1.1. Một số chức năng chính
Hệ thống đáp ứng đầy đủ các nhóm chức năng nghiệp vụ quản lý đào tạo truyền thống, đồng thời bổ sung phân hệ AI sinh trắc học hiện đại được phân bổ theo 4 nhóm đối tượng người dùng:

*Bảng 2.1. Một số chức năng chính của hệ thống EduFace*

| Nhóm chức năng | Tên chức năng | Mô tả tóm tắt mức cao |
| :--- | :--- | :--- |
| **Dành cho Khách (Guest)** | Xem trang chủ & Giới thiệu | Xem thông tin giới thiệu nền tảng EduFace, tin tức đào tạo, thông báo mới của nhà trường. |
| | Tra cứu Khóa học công khai | Tìm kiếm, xem danh mục các môn học/khóa học công khai, xem thông tin giảng viên và đề cương môn học. |
| | Đăng ký tài khoản người dùng | Tự đăng ký tài khoản học viên mới bằng email và thông tin cá nhân cơ bản. |
| | Gửi liên hệ & Hỗ trợ | Gửi biểu mẫu yêu cầu giải đáp thắc mắc hoặc hỗ trợ kỹ thuật tới ban quản trị. |
| **Xác thực & Bảo mật** | Đăng nhập hệ thống đa phương thức | Hỗ trợ đăng nhập bằng tài khoản (Email/Mật khẩu) hoặc đăng nhập nhanh bằng khuôn mặt (Face Login 1:N). |
| | Phân quyền truy cập | Phân cấp quyền chặt chẽ theo 4 vai trò: Khách (Guest), Sinh viên (Student), Giảng viên (Teacher), Quản trị viên (Admin). |
| **Nghiệp vụ Học tập (Sinh viên)** | Đăng ký sinh trắc học khuôn mặt | Thu thập và đăng ký 5 góc khuôn mặt (Chính diện, Trái, Phải, Ngước, Cúi) để thiết lập hồ sơ AI. |
| | Tham gia Khóa học & Xem bài giảng | Xem video bài giảng (HLS Streaming), tải tài liệu học tập; phản hồi điểm danh khuôn mặt ngẫu nhiên khi đang học. |
| | Thi trực tuyến có AI giám sát | Làm bài thi trắc nghiệm dưới sự giám sát liên tục của camera (cảnh báo quay cóp, rời màn hình, có người thứ hai, thi hộ). |
| | Tra cứu kết quả & Tiến độ | Theo dõi tiến độ hoàn thành khóa học, lịch sử làm bài kiểm tra và bảng điểm cá nhân. |
| **Nghiệp vụ Giảng dạy (Giảng viên)** | Quản lý Khóa học & Bài giảng | Khởi tạo khóa học, phân chia chương mục, tải lên tài liệu học tập (PDF, DOCX) và bài giảng video (Cloudinary). |
| | Quản lý Ngân hàng câu hỏi & Bài thi | Soạn thảo câu hỏi trắc nghiệm, tạo đề thi trực tuyến, thiết lập thời gian làm bài và bật chế độ giám sát AI. |
| | Giám sát & Quản lý điểm danh | Theo dõi tiến độ học tập của sinh viên, xem lịch sử điểm danh tự động qua video và phê duyệt kết quả. |
| | Xem báo cáo Gian lận thi cử | Xem nhật ký vi phạm (quay cóp, rời màn hình, thi hộ) kèm ảnh chụp bằng chứng từ AI để xử lý kỷ luật. |
| **Quản trị đào tạo (Admin)** | Quản lý Khoa & Ngành học | Thêm, sửa, xóa, tìm kiếm và phân loại cơ cấu tổ chức khoa/viện và các ngành đào tạo. |
| | Quản lý Lớp học & Môn học | Quản lý danh mục lớp niên chế, lớp học phần, môn học và phân công giảng viên phụ trách. |
| | Quản lý Người dùng | Quản lý hồ sơ giảng viên, sinh viên; cấp phát, khóa hoặc khôi phục quyền truy cập tài khoản. |
| | Cấu hình tham số AI & Hệ thống | Cài đặt các ngưỡng nhận diện, ngưỡng phát hiện gian lận và xem nhật ký giám sát toàn hệ thống. |

---

### 2.1.2. Yêu cầu phi chức năng
Hệ thống cần đảm bảo tính ổn định, tốc độ xử lý theo thời gian thực và an toàn thông tin sinh trắc học của người học.

*Bảng 2.2. Yêu cầu phi chức năng của hệ thống EduFace*

| Tiêu chí | Mô tả yêu cầu kỹ thuật |
| :--- | :--- |
| **Hiệu suất (Performance)** | - Thời gian phản hồi của các API nghiệp vụ thông thường $\le 200\text{ ms}$.<br>- Phân hệ Face AI Core đạt tốc độ xử lý khung hình thời gian thực từ $35 - 50\text{ ms/frame}$ trên CPU, không gây đơ lag trình duyệt.<br>- Khả năng phục vụ đồng thời tối thiểu 500 sinh viên truy cập làm bài kiểm tra cùng lúc. |
| **Bảo mật (Security)** | - Mã hóa mật khẩu người dùng theo chuẩn BCrypt một chiều.<br>- Xác thực phân quyền truy cập thông qua JSON Web Token (JWT) và xác thực giữa các Microservice bằng chữ ký số HMAC-SHA256.<br>- Tuyệt đối không lưu trữ ảnh khuôn mặt gốc ra đĩa cứng; chỉ lưu trữ vector số thực đặc trưng 512D đã mã hóa để bảo vệ quyền riêng tư theo quy định pháp lý. |
| **Khả năng tương thích (Compatibility)** | - Hoạt động tương thích tốt trên các trình duyệt web hiện đại (Google Chrome, Microsoft Edge, Firefox, Safari) hỗ trợ chuẩn HTML5 và WebRTC.<br>- Giao diện thích ứng (Responsive Web Design) với nhiều độ phân giải màn hình từ Laptop, PC đến thiết bị di động. |
| **Tính khả dụng (Usability)** | - Giao diện trực quan, thân thiện, dễ sử dụng cho cả người dùng mới (Khách), giảng viên và sinh viên.<br>- Hướng dẫn trực quan từng bước khi thu thập dữ liệu khuôn mặt 5 góc; thông báo lỗi và cảnh báo gian lận rõ ràng, minh bạch. |
| **Khả năng mở rộng (Scalability)** | - Kiến trúc phân tán phân tách giữa Backend nghiệp vụ (Spring Boot) và Backend AI (FastAPI), cho phép nâng cấp hoặc mở rộng cụm máy chủ AI độc lập khi số lượng sinh viên tăng cao. |

---

### 2.1.3. Các tác nhân tham gia hệ thống
Hệ thống bao gồm 4 tác nhân chính (Actors) là con người và 1 tác nhân hệ thống bên ngoài:
1. **Khách vãng lai (Guest):** Người dùng chưa đăng nhập vào hệ thống (học sinh, phụ huynh, người quan tâm). Mục đích là tìm hiểu thông tin về nhà trường, tra cứu danh mục khóa học và đăng ký tài khoản.
2. **Sinh viên (Student):** Người học chính thức, sử dụng tài khoản được cấp để tiếp thu kiến thức, xem bài giảng, thực hiện điểm danh và làm bài kiểm tra trực tuyến có camera giám sát.
3. **Giảng viên (Teacher):** Người trực tiếp quản lý nội dung môn học, tổ chức giảng dạy, xây dựng đề thi và đánh giá kết quả học tập của sinh viên.
4. **Quản trị viên (Admin):** Đại diện cho Phòng Đào tạo / Trung tâm Công nghệ thông tin của nhà trường, có quyền hạn cao nhất trong việc quản trị cơ cấu đào tạo, cấp phát tài khoản, cấu hình tham số AI và giám sát toàn bộ hoạt động của hệ thống.
5. **Phân hệ Face AI Service (External System Actor):** Dịch vụ vi mô thông minh chịu trách nhiệm tính toán thị giác máy tính, phát hiện khuôn mặt, trích xuất vector và đối soát gian lận.

---

### 2.1.4. Quy trình nghiệp vụ trong hệ thống

Hệ thống vận hành xoay quanh 5 quy trình nghiệp vụ trọng tâm sau:

#### a. Quy trình Tra cứu và Đăng ký của Khách vãng lai
1. Khách truy cập vào trang chủ hệ thống EduFace qua trình duyệt.
2. Khách duyệt danh mục các khóa học công khai, tìm kiếm môn học theo khoa/ngành hoặc theo tên giảng viên.
3. Nếu có nhu cầu tham gia học tập, Khách chọn "Đăng ký tài khoản" và điền thông tin cá nhân.
4. Hệ thống tạo tài khoản mới và gửi mã xác thực qua email; sau khi kích hoạt, Khách được chuyển đổi vai trò thành Sinh viên.

#### b. Quy trình Quản lý đào tạo và Cấp phát tài khoản
1. Quản trị viên khởi tạo danh mục Khoa, Ngành học và các Lớp học phần trong học kỳ.
2. Quản trị viên nhập danh sách giảng viên và sinh viên từ hệ thống quản lý đào tạo chung, hệ thống tự động cấp phát tài khoản và gửi email thông báo.
3. Quản trị viên phân công giảng viên phụ trách từng lớp môn học cụ thể.

#### c. Quy trình Đăng ký sinh trắc học khuôn mặt (Face Enrollment)
1. Sinh viên đăng nhập vào hệ thống lần đầu và truy cập trang "Đăng ký khuôn mặt".
2. Sinh viên bật webcam; hệ thống hướng dẫn sinh viên xoay khuôn mặt theo 5 hướng mục tiêu: **Chính diện $\to$ Nghiêng trái $\to$ Nghiêng phải $\to$ Ngước lên $\to$ Cúi xuống**.
3. Phân hệ AI kiểm tra chất lượng ánh sáng, độ nét và tự động trích xuất 5 vector đặc trưng 512 chiều lưu vào cơ sở dữ liệu để phục vụ cho các phiên điểm danh và thi cử sau này.

#### d. Quy trình Học tập và Điểm danh khuôn mặt tự động
1. Sinh viên vào lớp học và chọn video bài giảng cần theo dõi.
2. Trong quá trình phát video, hệ thống ngẫu nhiên kích hoạt cửa sổ điểm danh nhanh.
3. Sinh viên nhìn thẳng vào webcam; phân hệ AI xác thực khuôn mặt trong vòng 1 giây:
   - Nếu khớp danh tính: Tiếp tục mở khóa bài giảng và ghi nhận kết quả điểm danh.
   - Nếu không khớp hoặc phát hiện gian lận (dùng ảnh chụp): Đánh dấu vắng mặt và gửi thông báo tới giảng viên.

#### e. Quy trình Thi trực tuyến có AI giám sát (Online Exam Proctoring)
1. Giảng viên cấu hình bài thi trắc nghiệm, ấn định thời gian và bật tính năng "Giám sát AI".
2. Sinh viên truy cập bài thi $\to$ Thực hiện quét khuôn mặt xác thực ban đầu (1:1 Verification) để mở đề thi.
3. Trong suốt thời gian làm bài, webcam liên tục truyền luồng hình ảnh về phân hệ AI để phân tích:
   - Nếu không thấy mặt: Kích hoạt cảnh báo `FACE_NOT_FOUND` (rời khỏi vị trí).
   - Nếu có người ngồi cạnh: Kích hoạt cảnh báo `MULTIPLE_FACES` (nhờ người nhắc bài).
   - Nếu quay đầu ngó nghiêng tài liệu: Kích hoạt cảnh báo `TURN_LEFT` / `TURN_RIGHT` / `LOOK_DOWN`.
   - Nếu phát hiện mặt người khác ngồi làm bài: Kích hoạt cờ `IMPERSONATION_BLOCKED` (thi hộ).
4. Hệ thống tự động ghi nhật ký vi phạm kèm ảnh chụp bằng chứng; khi nộp bài, giảng viên sẽ có bảng tổng hợp mức độ vi phạm để đưa ra quyết định trừ điểm hoặc đình chỉ bài thi.

---

### 2.1.5. Bảng phân quyền chức năng theo tác nhân

*Bảng 2.3. Bảng phân quyền chức năng chi tiết theo 4 tác nhân*

| STT | Tên chức năng | Khách (Guest) | Sinh viên (Student) | Giảng viên (Teacher) | Quản trị viên (Admin) |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **1** | **Chức năng Công khai & Tiếp cận** | | | | |
| 1.1 | Xem trang chủ, tin tức, thông báo nhà trường | ✔ | ✔ | ✔ | ✔ |
| 1.2 | Tra cứu danh mục khóa học, môn học công khai | ✔ | ✔ | ✔ | ✔ |
| 1.3 | Xem đề cương chi tiết và thông tin giảng viên | ✔ | ✔ | ✔ | ✔ |
| 1.4 | Đăng ký tài khoản người học mới | ✔ | | | |
| 1.5 | Gửi biểu mẫu liên hệ, hỗ trợ kỹ thuật | ✔ | ✔ | ✔ | |
| **2** | **Quản lý Tài khoản & Định danh** | | | | |
| 2.1 | Đăng nhập tài khoản truyền thống / Đổi mật khẩu | | ✔ | ✔ | ✔ |
| 2.2 | Đăng nhập nhận diện khuôn mặt (Face Login 1:N) | | ✔ | ✔ | |
| 2.3 | Đăng ký sinh trắc học khuôn mặt 5 góc | | ✔ | | |
| 2.4 | Quản lý và phân quyền tài khoản người dùng | | | | ✔ |
| **3** | **Quản lý Cơ cấu Đào tạo** | | | | |
| 3.1 | Quản lý Khoa / Viện và Ngành đào tạo | | | | ✔ |
| 3.2 | Quản lý Lớp niên chế và Lớp học phần | | | | ✔ |
| 3.3 | Phân công giảng viên phụ trách môn học | | | | ✔ |
| **4** | **Quản lý Học liệu & Khóa học** | | | | |
| 4.1 | Khởi tạo và thiết lập thông tin khóa học | | | ✔ | ✔ |
| 4.2 | Tải lên và quản lý tài liệu học tập (PDF, Word) | | | ✔ | |
| 4.3 | Tải lên và phân phối bài giảng video (Streaming) | | | ✔ | |
| 4.4 | Xem bài giảng và tải tài liệu học tập | | ✔ | | |
| **5** | **Kiểm tra, Khảo thí & Giám sát AI** | | | | |
| 5.1 | Tạo ngân hàng câu hỏi và thiết lập đề thi trắc nghiệm | | | ✔ | |
| 5.2 | Bật/tắt chế độ giám sát thi AI và cấu hình ngưỡng | | | ✔ | |
| 5.3 | Tham gia làm bài kiểm tra trực tuyến có camera | | ✔ | | |
| 5.4 | Giám sát trực tiếp phiên thi và phát hiện gian lận tự động | | | ✔ | |
| 5.5 | Xem nhật ký vi phạm thi cử và xuất biên bản | | | ✔ | ✔ |
| 5.6 | Chấm điểm tự động và công bố kết quả | | | ✔ | |
| 5.7 | Tra cứu lịch sử học tập và điểm thi cá nhân | | ✔ | | |
| **6** | **Báo cáo & Cấu hình Hệ thống** | | | | |
| 6.1 | Xem thống kê số lượng sinh viên, khóa học | | | ✔ | ✔ |
| 6.2 | Quản trị cấu hình ngưỡng AI và dung lượng lưu trữ | | | | ✔ |
| 6.3 | Quản lý nhật ký hoạt động hệ thống (System Logs) | | | | ✔ |
