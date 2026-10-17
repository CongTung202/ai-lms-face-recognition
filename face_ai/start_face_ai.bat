@echo off
title Python Face AI Microservice - ArcFace 512D [Port 8001]
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==============================================================================
echo        DICH VU NHAN DIEN KHUON MAT VA GIAM SAT THI [PYTHON FASTAPI]
echo ==============================================================================

:: 1. Tu dong kiem tra va giai phong cong 8001 neu dang bi chiem dung
set "PORT_8001_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8001" ^| findstr "LISTENING"') do (
    set "PORT_8001_PID=%%P"
)
if defined PORT_8001_PID (
    echo [!] Phat hien tien trinh cu dang chiem cong 8001 PID !PORT_8001_PID!
    echo [*] Dang giai phong cong 8001 de khoi dong lai dich vu...
    taskkill /F /PID !PORT_8001_PID! >nul 2>&1
    ping 127.0.0.1 -n 2 >nul
    echo [v] Da giai phong cong 8001 thanh cong.
    echo.
)

:: 2. Tu dong nap bien moi truong tu .env
if exist "%~dp0..\.env" (
    echo [i] Dang nap cau hinh bao mat tu: %~dp0..\.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0..\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

:: 3. Kiem tra Python
set "PY_CMD=python"
if exist "%~dp0.venv\Scripts\python.exe" (
    set "PY_CMD=%~dp0.venv\Scripts\python.exe"
) else if exist "%~dp0venv\Scripts\python.exe" (
    set "PY_CMD=%~dp0venv\Scripts\python.exe"
)

:: 4. Kiem tra mo hinh AI YuNet ONNX
if not exist "%~dp0data\models\face_detection_yunet.onnx" (
    echo [!] Chua tim thay mo hinh YuNet. Dang tu dong tai ve...
    %PY_CMD% download_models.py
)

echo ------------------------------------------------------------------------------
echo [THONG SO MO HINH THI GIAC AI]:
echo - Thuat toan phat hien : OpenCV YuNet ONNX [Face Detection]
echo - Vector dac trung     : ArcFace ResNet100 512 Chieu
echo - Kiem tra goc 3D      : 3D Head Pose [Pitch, Yaw, Roll]
echo - Chong gia mao        : Anti-Spoofing Liveness Detection
echo - Luu tru CDN anh      : Cloudinary [Thu muc: lms_datn/faces]
echo.
echo [DIA CHI TRUY CAP DICH VU AI]:
echo - Dich vu API goc      : http://localhost:8001
echo - Tai lieu Swagger UI  : http://localhost:8001/docs
echo - Endpoint Dang ky     : http://localhost:8001/api/v1/face/enroll
echo - Endpoint Doi soat    : http://localhost:8001/api/v1/face/verify
echo - Endpoint Head-Pose   : http://localhost:8001/api/v1/face/head-pose
echo ==============================================================================
echo [*] Dang khoi chay Uvicorn server tren cong 8001...
echo.

%PY_CMD% -m uvicorn main:app --host 0.0.0.0 --port 8001 --reload

pause
