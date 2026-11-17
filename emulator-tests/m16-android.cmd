@echo off
cd /d "%~dp0"
set ADB=C:\Users\cura\AppData\Local\Android\Sdk\platform-tools\adb.exe
"%ADB%" install --no-streaming -r -t ..\app\build\mision16-auth\app-debug.apk
if errorlevel 1 exit /b 1
"%ADB%" install --no-streaming -r -t ..\app\build\mision16-auth\app-debug-androidTest.apk
if errorlevel 1 exit /b 1
"%ADB%" reverse tcp:9000 tcp:9000
"%ADB%" shell am instrument -w -e class com.example.basta_game.Mision16SesionTest com.example.basta_game.authtest.test/com.example.basta_game.AuthRunner > ..\docs\verificacion-m16\sesion-avd.txt 2>&1
type ..\docs\verificacion-m16\sesion-avd.txt
findstr /L /C:"OK (1 test)" ..\docs\verificacion-m16\sesion-avd.txt >nul
if errorlevel 1 exit /b 1
"%ADB%" shell am instrument -w -e class com.example.basta_game.AuthFlujoTest com.example.basta_game.authtest.test/com.example.basta_game.AuthRunner > ..\docs\verificacion-m16\auth-avd.txt 2>&1
type ..\docs\verificacion-m16\auth-avd.txt
findstr /L /C:"OK (1 test)" ..\docs\verificacion-m16\auth-avd.txt >nul
if errorlevel 1 exit /b 1
"%ADB%" shell am instrument -w -e class com.example.basta_game.Mision16InterfazTest com.example.basta_game.authtest.test/com.example.basta_game.AuthRunner > ..\docs\verificacion-m16\interfaz-avd-final.txt 2>&1
type ..\docs\verificacion-m16\interfaz-avd-final.txt
findstr /L /C:"OK (3 tests)" ..\docs\verificacion-m16\interfaz-avd-final.txt >nul
if errorlevel 1 exit /b 1
"%ADB%" reverse --remove tcp:9000
"%ADB%" shell am force-stop com.example.basta_game.authtest
exit /b 0