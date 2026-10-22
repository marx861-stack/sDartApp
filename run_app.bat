@echo off
title s'Dart App Launcher
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
"%JAVA_HOME%\bin\java.exe" -jar "composeApp\build\compose\jars\sDartApp-windows-x64-1.0.0.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Die App wurde mit Fehler beendet (%ERRORLEVEL%).
    pause
)
