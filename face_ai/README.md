# Mô-đun Nhận diện khuôn mặt Python Face AI (legiapos.vn)

Thư mục này chứa toàn bộ mã nguồn Python phục vụ nhận diện khuôn mặt bằng AI (FastAPI + OpenCV YuNet + ArcFace ResNet100 + 3D Head Pose + Liveness Detection) để tích hợp vào hệ thống **legiapos.vn**.

## 1. Cấu trúc thư mục

- `main.py`: Điểm khởi chạy dịch vụ FastAPI Face AI (Port 8000).
- `app/`:
  - `config.py`: Cấu hình hệ thống, ngưỡng chất lượng, độ tương đồng, đường dẫn mô hình ONNX, kết nối MongoDB/Qdrant, HMAC Secret.
  - `api/routes.py`: Định nghĩa các API endpoints (đăng ký khuôn mặt, nhận diện/điểm danh, kiểm tra ảnh, v.v.).
  - `core/`:
    - `face_detector.py`: Phát hiện khuôn mặt bằng YuNet ONNX.
    - `face_embedder.py`: Trích xuất vector đặc trưng 512D bằng ArcFace ONNX.
    - `face_storage.py`: Quản lý lưu trữ vector khuôn mặt (MongoDB / Vector DB / Local).
    - `head_pose_3d.py`: Ước tính góc quay khuôn mặt 3D (Pitch, Yaw, Roll).
    - `liveness_checker.py`: Kiểm tra chống giả mạo khuôn mặt (Anti-spoofing).
    - `quality_checker.py`: Đánh giá chất lượng ảnh (độ sáng, độ mờ, góc nhìn, kích thước).
    - `hmac_auth.py`: Bảo mật giao tiếp giữa Node.js backend và Python AI service.
  - `schemas/models.py`: Định dạng dữ liệu đầu vào/ra (Pydantic).
  - `services/face_service.py`: Luồng nghiệp vụ xử lý nhận diện khuôn mặt tổng thể.
- `data/`:
  - `models/`: Chứa file mô hình AI ONNX (`face_detection_yunet.onnx`, `arcface_resnet100.onnx`).
  - `faces_vector_db.json`: Cơ sở dữ liệu vector khuôn mặt cục bộ dự phòng.
  - `registered_faces/`: Thư mục lưu ảnh khuôn mặt mẫu đã đăng ký.
- `www/`: Giao diện web UI demo & đăng ký khuôn mặt trực quan.
- `requirements.txt`: Danh sách các thư viện Python cần thiết.
- `download_models.py`: Kịch bản tải mô hình AI ONNX tự động nếu chưa có.
- `clear_face_cache.py`: Kịch bản xóa cache vector để đồng bộ lại.
- `test_system.py`: Kịch bản kiểm thử các tính năng nhận diện.

---

## 2. Hướng dẫn cài đặt & Khởi chạy

### Bước 1: Cài đặt thư viện Python
Trong thư mục `face_ai/`:
```bash
pip install -r requirements.txt
```

### Bước 2: Tải mô hình nhận diện (nếu cần)
```bash
python download_models.py
```

### Bước 3: Khởi chạy Python Face AI Service
```bash
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Dịch vụ sẽ hoạt động tại:
- **API Docs (Swagger UI)**: `http://localhost:8000/docs`
- **Health check**: `http://localhost:8000/health`
- **Giao diện Web AI Demo**: `http://localhost:8000/`
