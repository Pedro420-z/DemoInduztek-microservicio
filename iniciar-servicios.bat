@echo off
setlocal
cd /d "%~dp0"
title Induztek - arranque de servicios

if not exist "correo.local.bat" goto sincorreo
call "correo.local.bat"
echo Correo configurado con la cuenta %MAIL_USUARIO%
goto servicios

:sincorreo
echo No se encontro correo.local.bat: los correos se escribiran en la consola de usuarios-service.

:servicios
call :iniciar eureka-server 8761
call :iniciar usuarios-service 8081
call :iniciar equipos-service 8082
call :iniciar protocolos-service 8083
call :iniciar reportes-service 8084
call :iniciar api-gateway 8080
echo.
echo Servicios iniciados. Eureka: http://localhost:8761 - Gateway: http://localhost:8080
pause
exit /b 0

:iniciar
set /a intentos=0
echo Iniciando %1 ...
start "%1" cmd /k "mvnw.cmd -pl %1 spring-boot:run"

:esperar
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName localhost -Port %2 -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 goto listo
set /a intentos+=1
if %intentos% GEQ 60 goto fallo
timeout /t 3 /nobreak >nul
goto esperar

:listo
echo %1 listo en el puerto %2
exit /b 0

:fallo
echo AVISO: %1 no respondio en el puerto %2 despues de 3 minutos. Revisa su ventana.
exit /b 0
