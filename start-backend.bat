@echo off
setlocal EnableExtensions
title Smart Freelance - start backend
cd /d "%~dp0"

if not exist "%~dp0start-backend.ps1" (
    echo [ERROR] start-backend.ps1 not found next to this file:
    echo         %~dp0
    goto :end
)

if not exist "%~dp0backEnd\" (
    echo [ERROR] backEnd folder not found. The .bat file must live in the repo root.
    echo         Current folder: %~dp0
    goto :end
)

set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS%" (
    echo [ERROR] PowerShell not found at %PS%
    goto :end
)

echo Running: start-backend.ps1
echo Folder: %~dp0
echo.

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"

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
