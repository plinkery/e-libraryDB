@echo off
title College E-Library System
echo ============================================================
echo   Compiling & Starting College E-Library Server...
echo ============================================================
echo.

:: Check if Java is installed
where javac >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Java JDK is not installed or not in your PATH!
    echo Please install Java JDK (version 11 or higher) to run the server.
    pause
    exit /b
)

:: Ensure bin directory exists
if not exist "bin" mkdir bin

echo Compiling Java Backend...
javac -d bin -sourcepath src src/ELibraryServer.java
if %errorLevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b
)

echo.
echo ============================================================
echo   Server started successfully!
echo   Open your browser and visit: http://localhost:8080
echo   Press Ctrl+C to stop the server.
echo ============================================================
echo.

java -Xms32m -Xmx256m -cp bin ELibraryServer
pause
