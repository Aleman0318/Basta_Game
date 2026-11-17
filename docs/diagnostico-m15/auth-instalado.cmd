@echo off
"C:\Users\cura\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell am instrument -w -e class com.example.basta_game.AuthFlujoTest com.example.basta_game.authtest.test/com.example.basta_game.AuthRunner > "C:\Users\cura\Desktop\02_ANDROIDSTUDIO\Basta_Game\app\build\diagnostico-m15\auth-instalado.txt"
findstr /c:"OK (1 test)" "C:\Users\cura\Desktop\02_ANDROIDSTUDIO\Basta_Game\app\build\diagnostico-m15\auth-instalado.txt"
exit /b %errorlevel%
