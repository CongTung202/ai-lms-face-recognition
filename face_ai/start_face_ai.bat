@echo off
title Python Face AI Engine - LMS Online
cd /d "%~dp0"
echo ========================================================
echo   KHOI DONG PYTHON FACE AI SERVICE (PORT 8001)
echo   3 GOC MAT TRUC DIEN (STRAIGHT, LEFT, RIGHT)
echo   TICH HOP CLOUDINARY CDN: dhefmthim
echo ========================================================
echo.
python -m uvicorn main:app --host 0.0.0.0 --port 8001 --reload
pause
