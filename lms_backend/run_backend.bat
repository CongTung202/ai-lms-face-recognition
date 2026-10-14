@echo off
title LMS Spring Boot 3 Backend
cd /d "%~dp0"

echo ====================================================================
echo   KHOI CHAY LMS BACKEND (SPRING BOOT 3 + MYSQL + MONGODB)
echo ====================================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
set "MAVEN_CMD=C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"

echo [i] JAVA_HOME: %JAVA_HOME%
echo [i] Maven: %MAVEN_CMD%
echo [i] MySQL: localhost:3306/lms_online_db
echo [i] MongoDB: localhost:27017/lms_nosql_db
echo [i] Dang khoi chay Spring Boot tren cong 8080...
echo.

"%MAVEN_CMD%" spring-boot:run
pause
