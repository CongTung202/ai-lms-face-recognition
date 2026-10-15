@echo off
chcp 65001 >nul
title Python Face AI Microservice - ArcFace 512D (Port 8001)
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==============================================================================
echo        DỊCH VỤ NHẬN DIỆN KHUÔN MẶT & GIÁM SÁT THI (PYTHON FASTAPI)
echo ==============================================================================

:: 1. Tự động nạp biến môi trường từ .env
if exist "%~dp0..\.env" (
    echo [i] Đang nạp cấu hình bảo mật từ: %~dp0..\.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0..\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

:: 2. Kiểm tra Python
set "PY_CMD=python"
if exist "%~dp0.venv\Scripts\python.exe" (
    set "PY_CMD=%~dp0.venv\Scripts\python.exe"
) else if exist "%~dp0venv\Scripts\python.exe" (
    set "PY_CMD=%~dp0venv\Scripts\python.exe"
)

:: 3. Kiểm tra mô hình AI YuNet ONNX
if not exist "%~dp0data\models\face_detection_yunet.onnx" (
    echo [!] Chưa tìm thấy mô hình YuNet. Đang tự động tải về...
    %PY_CMD% download_models.py
)

echo ------------------------------------------------------------------------------
echo [THÔNG SỐ MÔ HÌNH THỊ GIÁC AI]:
echo - Thuật toán phát hiện : OpenCV YuNet ONNX (Face Detection)
echo - Vector đặc trưng     : ArcFace ResNet100 512 Chiều
echo - Kiểm tra góc 3D      : 3D Head Pose (Pitch, Yaw, Roll)
echo - Chống giả mạo        : Anti-Spoofing Liveness Detection
echo - Lưu trữ CDN ảnh      : Cloudinary (Thư mục: lms_datn/faces)
echo.
echo [ĐỊA CHỈ TRUY CẬP DỊCH VỤ AI]:
echo - Dịch vụ API gốc      : http://localhost:8001
echo - Tài liệu Swagger UI  : http://localhost:8001/docs
echo - Endpoint Đăng ký     : http://localhost:8001/api/v1/face/enroll
echo - Endpoint Đối soát    : http://localhost:8001/api/v1/face/verify
echo - Endpoint Head-Pose   : http://localhost:8001/api/v1/face/head-pose
echo ==============================================================================
echo [*] Đang khởi chạy Uvicorn server trên cổng 8001...
echo.

%PY_CMD% -m uvicorn main:app --host 0.0.0.0 --port 8001 --reload

pause
