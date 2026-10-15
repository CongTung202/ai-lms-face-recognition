@echo off
chcp 65001 >nul
title LMS Backend Core - Spring Boot 3 (Port 8080)
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==============================================================================
echo        HỆ THỐNG QUẢN LÝ HỌC TẬP TRỰC TUYẾN (LMS) - SPRING BOOT 3
echo ==============================================================================

:: 1. Tự động nạp biến môi trường từ tệp .env nếu tồn tại
if exist "%~dp0.env" (
    echo [i] Đang nạp cấu hình bảo mật từ: %~dp0.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
) else if exist "%~dp0..\.env" (
    echo [i] Đang nạp cấu hình bảo mật từ: %~dp0..\.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0..\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

:: 2. Thiết lập đường dẫn JDK 17
if exist "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
)

:: 3. Thiết lập đường dẫn Maven
set "MAVEN_CMD=mvn"
if exist "C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" (
    set "MAVEN_CMD=C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"
)

echo ------------------------------------------------------------------------------
echo [THÔNG SỐ MÔI TRƯỜNG]:
echo - Java runtime : %JAVA_HOME%
echo - Maven binary : %MAVEN_CMD%
echo - MySQL CSDL   : localhost:3306/lms_online_db
echo - MongoDB NoSQL: localhost:27017/lms_nosql_db
echo.
echo [ĐỊA CHỈ TRUY CẬP HỆ THỐNG]:
echo - RESTful API Context  : http://localhost:8080/api
echo - Kiểm tra sức khỏe    : http://localhost:8080/api/health
echo - Giao diện Quản trị   : http://localhost:8080/api/index.html
echo ==============================================================================
echo [*] Đang khởi động Spring Boot 3 trên cổng 8080...
echo.

"%MAVEN_CMD%" spring-boot:run

pause
