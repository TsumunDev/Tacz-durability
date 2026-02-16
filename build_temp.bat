@echo off
cd /d "%~dp0"
gradlew.bat build --no-daemon --console=plain
