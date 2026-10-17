@echo off
title LMS Backend Core - Spring Boot 3 [Port 8080]
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==============================================================================
echo        HE THONG QUAN LY HOC TAP TRUC TUYEN [LMS] - SPRING BOOT 3
echo ==============================================================================

:: 1. Tu dong kiem tra va giai phong cong 8080 neu dang bi chiem dung
set "PORT_8080_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    set "PORT_8080_PID=%%P"
)
if defined PORT_8080_PID (
    echo [!] Phat hien tien trinh cu dang chiem cong 8080 PID !PORT_8080_PID!
    echo [*] Dang giai phong cong 8080 de khoi dong lai dich vu...
    taskkill /F /PID !PORT_8080_PID! >nul 2>&1
    ping 127.0.0.1 -n 2 >nul
    echo [v] Da giai phong cong 8080 thanh cong.
    echo.
)

:: 2. Tu dong nap bien moi truong tu tep .env neu ton tai
if exist "%~dp0.env" (
    echo [i] Dang nap cau hinh bao mat tu: %~dp0.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
) else if exist "%~dp0..\.env" (
    echo [i] Dang nap cau hinh bao mat tu: %~dp0..\.env
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0..\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

:: 3. Thiet lap duong dan JDK 17
if exist "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
)

:: 4. Thiet lap duong dan Maven
set "MAVEN_CMD=mvn"
if exist "C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" (
    set "MAVEN_CMD=C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"
)

echo ------------------------------------------------------------------------------
echo [THONG SO MOI TRUONG]:
echo - Java runtime : %JAVA_HOME%
echo - Maven binary : %MAVEN_CMD%
echo - MySQL CSDL   : localhost:3306/lms_online_db
echo - MongoDB NoSQL: localhost:27017/lms_nosql_db
echo.
echo [DIA CHI TRUY CAP HE THONG]:
echo - RESTful API Context  : http://localhost:8080/api
echo - Kiem tra suc khoe    : http://localhost:8080/api/health
echo - Giao dien Quan tri   : http://localhost:8080/api/index.html
echo ==============================================================================
echo [*] Dang khoi dong Spring Boot 3 tren cong 8080...
echo.

"%MAVEN_CMD%" spring-boot:run

pause
