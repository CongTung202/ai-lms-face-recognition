@echo off
title LMS - STOP ALL SERVICES
setlocal enabledelayedexpansion

echo ==============================================================================
echo        DUNG TOAN BO CAC DICH VU HE THONG LMS [STOP ALL SERVICES]
echo ==============================================================================
echo.

:: 1. Quet va dung tien trinh tren cong 8001 (Face AI)
echo [*] Dang kiem tra va giai phong cong 8001 [Face AI]...
set "COUNT_8001=0"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8001" ^| findstr "LISTENING"') do (
    set /a COUNT_8001+=1
    echo     [-] Dang tat tien trinh Face AI PID %%P
    taskkill /F /PID %%P >nul 2>&1
)
if "!COUNT_8001!"=="0" (
    echo     [i] Cong 8001 khong co tien trinh nao dang chay.
) else (
    echo     [v] Da dung dich vu Face AI tren cong 8001.
)
echo.

:: 2. Quet va dung tien trinh tren cong 8080 (Spring Boot Backend)
echo [*] Dang kiem tra va giai phong cong 8080 [Spring Boot Backend]...
set "COUNT_8080=0"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    set /a COUNT_8080+=1
    echo     [-] Dang tat tien trinh Backend Spring Boot PID %%P
    taskkill /F /PID %%P >nul 2>&1
)
if "!COUNT_8080!"=="0" (
    echo     [i] Cong 8080 khong co tien trinh nao dang chay.
) else (
    echo     [v] Da dung dich vu Backend tren cong 8080.
)
echo.

echo ==============================================================================
echo [v] TOAN BO CAC DICH VU DA DUOC DUNG AN TOAN VA GIAI PHONG TAI NGUYEN!
echo ==============================================================================
ping 127.0.0.1 -n 3 >nul
