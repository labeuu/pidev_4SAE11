@echo off
setlocal EnableExtensions
title Smart Freelance - stop backend
cd /d "%~dp0"

if not exist "%~dp0stop-backend.ps1" (
    echo [ERROR] stop-backend.ps1 not found next to this file:
    echo         %~dp0
    goto :end
)

set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS%" (
    echo [ERROR] PowerShell not found at %PS%
    goto :end
)

echo Running: stop-backend.ps1
echo.

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-backend.ps1"

:end
echo.
if errorlevel 1 (
    echo Script exited with error code %ERRORLEVEL%.
) else (
    echo Script finished.
)
echo.
echo Press any key to close this window...
pause >nul
endlocal
