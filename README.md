# AI LMS & Face Recognition System (Hệ Thống LMS Tích Hợp Nhận Diện Khuôn Mặt)

Hệ thống quản lý học tập trực tuyến (LMS) tích hợp công nghệ AI nhận diện khuôn mặt phục vụ điểm danh, kiểm tra danh tính và giám sát thi cử trực tuyến chống gian lận.

---

## 🌟 Kiến trúc tổng thể

Dự án gồm 3 thành phần chính:
1. **`lms_backend/`**: Hệ thống máy chủ dịch vụ trung tâm viết bằng **Java 17 / Spring Boot 3.2**:
   - Quản lý người dùng, phân quyền (RBAC: Admin, Instructor, Student).
   - Quản lý khóa học, bài học, video bài giảng và tài liệu học tập.
   - Quản lý bài thi trắc nghiệm & tự luận, chấm điểm tự động.
   - Tích hợp cổng thanh toán VietQR qua PayOS.
   - Lưu trữ media qua Cloudinary & Local Storage.
   - Giao tiếp với AI Service qua REST API / HMAC Security.
2. **`face_ai/`**: Dịch vụ thị giác máy tính AI viết bằng **Python FastAPI**:
   - Phát hiện khuôn mặt thời gian thực bằng mô hình **OpenCV YuNet ONNX**.
   - Trích xuất đặc trưng khuôn mặt 512D bằng **ArcFace ResNet100 ONNX**.
   - Đánh giá chất lượng khuôn mặt (độ sáng, góc nghiêng 3D Head Pose, độ mờ).
   - Kiểm tra phát hiện giả mạo khuôn mặt (Liveness Detection / Anti-spoofing).
   - Quản lý vector khuôn mặt sinh viên và đối soát điểm danh.
3. **`database/`**: Cơ sở dữ liệu quan hệ **MySQL** (`lms_schema.sql`) lưu trữ toàn bộ nghiệp vụ LMS và **MongoDB** phục vụ RAG Chatbot & Logs Telemetry.

---

## 📁 Cấu trúc thư mục

```text
├── database/                    # Kịch bản khởi tạo CSDL MySQL
│   └── lms_schema.sql
├── face_ai/                     # Dịch vụ Python AI Face Recognition (Port 8000/8001)
│   ├── app/                     # Mã nguồn chính (FastAPI, Core AI, Schemas, Services)
│   ├── data/                    # Models ONNX, vector database & ảnh khuôn mặt mẫu
│   ├── www/                     # Web UI demo kiểm thử nhận diện khuôn mặt
│   ├── requirements.txt         # Thư viện Python phụ thuộc
│   └── main.py
├── lms_backend/                 # Dịch vụ Spring Boot Backend (Port 8080)
│   ├── src/                     # Mã nguồn Java (Controllers, Services, Repositories, Security)
│   └── pom.xml
├── uploads/                     # Thư mục lưu trữ tài liệu & video bài học
├── PROJECT_PROGRESS_AND_ROADMAP.md # Lộ trình và tiến độ đồ án
├── run_backend.bat              # Script khởi chạy nhanh Backend
└── start_face_ai.bat            # Script khởi chạy nhanh Face AI
```

---

## 🚀 Hướng dẫn cài đặt & Khởi chạy

### 1. Cơ sở dữ liệu (MySQL & MongoDB)
- Cài đặt MySQL 8.0+ và MongoDB 6.0+.
- Import file schema:
  ```bash
  mysql -u root -p < database/lms_schema.sql
  ```

### 2. Dịch vụ AI (face_ai)
- Yêu cầu Python 3.10+:
  ```bash
  cd face_ai
  pip install -r requirements.txt
  python download_models.py
  python main.py
  ```
  Hoặc chạy trực tiếp file `start_face_ai.bat`.

### 3. Backend (lms_backend)
- Yêu cầu JDK 17 & Maven 3.8+:
  ```bash
  cd lms_backend
  mvn clean spring-boot:run
  ```
  Hoặc chạy trực tiếp file `run_backend.bat`.

---

## 🔒 Bảo mật & Công nghệ
- **Bảo mật**: Spring Security 6 + JWT Authentication, xác thực HMAC cho AI API.
- **AI Models**: OpenCV YuNet ONNX, ArcFace ResNet100, PnP 3D Head Pose.
- **Thanh toán**: Tích hợp PayOS Webhook tự động kích hoạt khóa học.
