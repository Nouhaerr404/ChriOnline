@echo off
title ChriOnline - Serveur
echo ========================================
echo    ChriOnline - Serveur TCP
echo ========================================
echo.
java -jar "%~dp0target\ChriOnline-1.0-server.jar" %*
pause
