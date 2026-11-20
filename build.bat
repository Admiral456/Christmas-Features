@echo off
setlocal
cd /d "%~dp0"
mvn clean package
if errorlevel 1 (
  echo Build failed. Check the Maven output above.
  pause
  exit /b 1
)
echo.
echo Built: target\ChristmasFeatures-1.21.10.jar
pause
