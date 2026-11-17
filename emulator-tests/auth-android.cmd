@echo off
"C:\Users\cura\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r -t ..\app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 exit /b 1
"C:\Users\cura\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r -t ..\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
if errorlevel 1 exit /b 1
"C:\Users\cura\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell am instrument -w -e class com.example.basta_game.AuthFlujoTest com.example.basta_game.authtest.test/com.example.basta_game.AuthRunner > auth-resultados.txt
findstr /c:"OK (1 test)" auth-resultados.txt
exit /b %errorlevel%
