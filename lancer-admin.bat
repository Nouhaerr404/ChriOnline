@echo off
title ChriOnline - Administration
echo ========================================
echo    ChriOnline - Application Admin
echo ========================================
echo.
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar "%~dp0target\ChriOnline-1.0-admin.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Tentative sans module-path JavaFX...
    java -jar "%~dp0target\ChriOnline-1.0-admin.jar"
)
pause
