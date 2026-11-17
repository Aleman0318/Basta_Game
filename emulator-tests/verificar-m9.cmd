@echo off
call ..\gradlew.bat -p . --offline --no-daemon --no-parallel --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx1280m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8" -I ..\docs\tools\verificacion-memoria.gradle test --console=plain
exit /b %errorlevel%
