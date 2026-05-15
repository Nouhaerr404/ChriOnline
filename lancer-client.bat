@echo off
title ChriOnline - Client
echo ========================================
echo    ChriOnline - Application Client
echo ========================================
echo.
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar "%~dp0target\ChriOnline-1.0-client.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Tentative sans module-path JavaFX...
    java -jar "%~dp0target\ChriOnline-1.0-client.jar"
)
pause
