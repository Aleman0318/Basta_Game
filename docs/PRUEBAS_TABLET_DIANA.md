# Version de pruebas para Diana - 2026-11-14

Rama: `pruebas-tablet-diana-2026-11-14`.
Dispositivo objetivo: Samsung Galaxy Tab A SM-T350, Android 7.1.1, API 25.

Esta rama es una instantanea con un commit raiz independiente. No incorpora la
ascendencia que contenia una antigua prueba con credenciales; no se reescriben
las ramas ni el historial existentes. El historial de otras ramas del repositorio
no queda saneado por esta publicacion y requiere una decision separada si se desea
retirar tambien aquellas copias. Para obtener solo esta version, usar el ZIP de
esta rama o `git clone --single-branch --depth 1 --branch pruebas-tablet-diana-2026-11-14
https://github.com/Aleman0318/Basta_Game.git`.

## Configuracion necesaria para compilar

- JDK 21 para el daemon Gradle; Android SDK 36.1 para compilar. minSdk=24 permite
  instalar en API 25, pero no demuestra ausencia de crashes en ese dispositivo.
- El Gradle Wrapper y su JAR estan incluidos; no hace falta instalar Gradle aparte.
- `app/google-services.json` NO esta publicado. Diana debe obtener por un canal
  privado un archivo de configuracion autorizado para `com.example.basta_game`.
  La compilacion Google Services lo requiere. No se incluye una cuenta de servicio,
  una clave privada ni una configuracion inventada que aparente ser la real.
- Android Studio puede generar `local.properties` con la ruta local del SDK. No
  copiar la ruta de otra computadora ni publicar este archivo.
- Primera compilacion: red para las dependencias declaradas. No se cambian versiones.
  No hace falta keystore de release: assembleDebug usa la firma de depuracion local.

Desde la raiz, en PowerShell:

```powershell
.\gradlew.bat assembleDebug --console=plain
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (generado localmente, no versionado).
Los emuladores Firebase son opcionales para compilar Android; sus instrucciones
estan en `emulator-tests/README.md`. Ese conjunto usa exclusivamente un proyecto
demo y NO redirige automaticamente la app Android normal al emulador.

## Flujo y alcance de esta version

El launcher del manifiesto es SplashActivity, que abre LoginActivity. MainActivity
se conserva, sin la antigua prueba de crear una cuenta. El registro/login normal
no se modifica en esta preparacion de publicacion.

Incluye las correcciones acumuladas de sincronizacion, recompensas, host, registro
institucional, continuidad, UX y pruebas. Los limites pendientes siguen en
PROJECT_STATE.md y MISION_6_VALIDACION.md: el temporizador compartido de votacion y
el cupo de cinco por categoria no estan implementados ni autorizados aun. Tampoco
se implementa premio de supervivencia. No es una declaracion de compatibilidad ya
verificada con la tablet ni de solucion del cierre inesperado.

No se publican IDE local, builds, APK, caches, Graphify generado, herramientas
instaladas, claves de firma o configuracion Firebase local. Los recursos Java/XML,
graficos/sonidos, Gradle, tests y documentacion si forman parte de la instantanea.

## Capturar el cierre de la tablet

Con depuracion USB y dispositivo autorizado, seleccionar su serial:

```powershell
adb devices -l
adb -s <serial-tablet> logcat -v threadtime AndroidRuntime:E '*:S'
```

Abrir Basta y reproducir. Conservar FATAL EXCEPTION, Process y todos los Caused by.
Si no se captura en vivo:

```powershell
adb -s <serial-tablet> logcat -b crash -d -v threadtime
```

Adjuntar el punto exacto del cierre y la version compilada; ocultar datos personales
y tokens antes de compartir el registro. No cambiar reglas ni datos de Firebase
para diagnosticar; la automatizacion de esta rama no despliega nada.
