@echo off
echo Building NeoForge 1.20.1...
copy /Y build.gradle build.gradle.forge >nul
copy /Y settings.gradle settings.gradle.forge >nul
copy /Y build-neoforge.gradle build.gradle >nul
copy /Y settings.gradle settings.gradle.orig >nul
echo settings.gradle > settings.gradle

gradlew build --no-daemon %*

copy /Y build.gradle.forge build.gradle >nul
copy /Y settings.gradle.forge settings.gradle >nul
del build.gradle.forge
del settings.gradle.forge
del settings.gradle.orig
