@echo off
setlocal
set "DIR=%~dp0"
cd /d "%DIR%"
"%DIR%\gradlew.bat" build --no-daemon
