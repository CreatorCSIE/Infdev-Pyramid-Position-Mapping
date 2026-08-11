@echo off
setlocal
cd /d "%~dp0"

if not exist "BrickMapping.jar" (
    echo BrickMapping.jar not found. Run compile.bat first.
    pause
    exit /b 1
)

java -jar BrickMapping.jar
endlocal