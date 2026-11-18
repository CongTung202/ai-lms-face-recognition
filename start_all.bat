@echo off
title LMS ALL-IN-ONE LAUNCHER AND DASHBOARD
setlocal enabledelayedexpansion

:MAIN_MENU
cls
echo ==============================================================================
echo        HE THONG QUAN LY HOC TAP TRUC TUYEN [LMS] - ALL-IN-ONE CONTROLLER
echo ==============================================================================
echo.
echo [1/3] Dang kiem tra va giai phong cac cong mang [Clean Ports]...

:: 1. Giai phong cong 8001 (Face AI) neu bi chiem
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8001" ^| findstr "LISTENING"') do (
    echo     [*] Dang giai phong tien trinh cu chiem cong 8001 PID %%P
    taskkill /F /PID %%P >nul 2>&1
)

:: 2. Giai phong cong 8080 (Spring Boot Backend) neu bi chiem
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo     [*] Dang giai phong tien trinh cu chiem cong 8080 PID %%P
    taskkill /F /PID %%P >nul 2>&1
)
ping 127.0.0.1 -n 2 >nul
echo     [v] Trang thai cong 8001 va 8080 da san sang.
echo.

echo [2/3] Dang khoi dong Dich vu Python Face AI tren cong 8001...
start "LMS Face AI - Port 8001" cmd /k "cd /d "%~dp0" && call start_face_ai.bat"
echo     [v] Cua so Face AI da duoc kich hoat. Dang cho 3 giay...
ping 127.0.0.1 -n 4 >nul
echo.

echo [3/3] Dang khoi dong LMS Spring Boot 3 Backend tren cong 8080...
start "LMS Spring Boot Backend - Port 8080" cmd /k "cd /d "%~dp0" && call run_backend.bat"
echo     [v] Cua so Spring Boot da duoc kich hoat.
echo.
ping 127.0.0.1 -n 3 >nul

:DASHBOARD
cls
echo ==============================================================================
echo        BANG DIEU KHIEN TRUNG TAM HE THONG LMS [DASHBOARD AND SHORTCUTS]
echo ==============================================================================
echo [HA TANG DICH VU DANG CHAY]:
echo   [1] Python Face AI Microservice  : http://localhost:8001 [Port 8001]
echo   [2] Spring Boot 3 Backend Core   : http://localhost:8080/api [Port 8080]
echo   [3] MySQL 8.0 Database           : localhost:3306 [lms_online_db]
echo   [4] MongoDB NoSQL Database       : localhost:27017 [lms_nosql_db]
echo.
echo [DIA CHI TRUY CAP VA KIEM THU]:
echo   - Cong Trang chu va Landing Page : http://localhost:8080/
echo   - Giao dien Admin Portal Doc lap : file:///%~dp0admin_web/index.html
echo   - Tai lieu API Face AI [Swagger] : http://localhost:8001/docs
echo   - Kiem tra suc khoe Backend      : http://localhost:8080/api/health
echo   - Demo Video Player va Chong tua : file:///%~dp0test_video_player.html
echo   - Demo Tro ly AI RAG Giao trinh  : file:///%~dp0test_rag_chatbot.html
echo ------------------------------------------------------------------------------
echo [LENH THAO TAC NHANH]:
echo   [1] Mo Trang chu Landing Page tren trinh duyet
echo   [2] Mo Tai lieu Swagger UI cua Face AI [Port 8001]
echo   [3] Mo Trang Kiem thu Video Streaming va Chong tua
echo   [4] Mo Trang Kiem thu Tro ly AI RAG Giao trinh
echo   [R] KHOI DONG LAI TOAN BO HE THONG [Restart All Services]
echo   [S] TAT TOAN BO HE THONG [Stop All Services]
echo   [0] Thoat bang dieu khien [Dich vu van tiep tuc chay ngam]
echo ==============================================================================
set /p "CHOICE=Nhap lua chon cua ban [1/2/3/4/R/S/0] va nhan Enter: "

if /i "%CHOICE%"=="1" (
    start http://localhost:8080/
    goto DASHBOARD
)
if /i "%CHOICE%"=="2" (
    start http://localhost:8001/docs
    goto DASHBOARD
)
if /i "%CHOICE%"=="3" (
    start "" "%~dp0test_video_player.html"
    goto DASHBOARD
)
if /i "%CHOICE%"=="4" (
    start "" "%~dp0test_rag_chatbot.html"
    goto DASHBOARD
)
if /i "%CHOICE%"=="R" (
    echo.
    echo [*] Dang chuan bi khoi dong lai toan bo dich vu...
    goto MAIN_MENU
)
if /i "%CHOICE%"=="S" (
    call "%~dp0stop_all.bat"
    echo.
    echo [*] Da tat toan bo dich vu. Nhan phim bat ky de dong...
    pause >nul
    exit
)
if /i "%CHOICE%"=="0" (
    exit
)

echo [!] Lua chon khong hop le, vui long thu lai!
ping 127.0.0.1 -n 2 >nul
goto DASHBOARD
