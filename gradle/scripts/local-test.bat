@echo off
setlocal EnableExtensions

REM Always run from repo root (script's parent)
cd /d "%~dp0..\.."

REM Optional: align CI-ish behavior
set CI=true

REM Use gradlew.bat
call gradlew.bat --no-daemon --stacktrace ^
  clean ^
  :app:assembleDebug ^
  :app:testDebugUnitTest ^
  :app:lint

if errorlevel 1 (
  echo.
  echo Local CI check FAILED.
  exit /b 1
)

echo.
echo Local CI check PASSED.
exit /b 0
