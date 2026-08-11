@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac not found. Please install JDK and add it to PATH.
    pause
    exit /b 1
)

if not exist build mkdir build

echo Compiling sources...
dir /s /b "src\*.java" > sources.txt
javac -encoding UTF-8 -d build @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

echo Packaging BrickMapping.jar...
jar cfe BrickMapping.jar brickmap.PyramidMapping -C build .
if errorlevel 1 (
    echo [ERROR] Packaging failed.
    pause
    exit /b 1
)

echo.
echo Build complete: BrickMapping.jar
echo Run with run.bat or: java -jar BrickMapping.jar
pause
endlocal