@echo off
REM Double-click this file to run. (.ps1 files don't execute on double-click by
REM default on Windows, so this .bat wrapper is what makes "just double-click it" work.)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-server.ps1"
pause
