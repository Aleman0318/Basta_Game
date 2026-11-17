# MISIÓN 15 — Informe de congelamiento y entorno

Fecha: 2026-11-16. Estado: conectividad del AVD recuperada y UI comprobada;
congelamiento total histórico NO reproducido durante esta investigación.

## 1. Causa principal y grado de certeza

**Demostrado:** el resolver DNS del AVD fallaba para www.google.com,
firebasestorage.googleapis.com, identitytoolkit.googleapis.com y dl.google.com.
La JVM Android devolvió UnknownHostException tanto en DNS como en HTTPS. También
fallaron ping/resolución de example.com. El indicador Android VALIDATED conservaba
una evaluación anterior; no demostraba que las peticiones actuales funcionaran.
En Windows, curl con validación normal y el JBR sí completaban HTTPS.

**Corregido en esta sesión:** arranque en frío del MISMO AVD con DNS 8.8.8.8,
que ya figuraba entre los DNS de Windows y se comprobó accesible. Después:
Google 200, Storage 404 de raíz, Identity Toolkit 404 de raíz y dl.google.com 200,
todos con TLS validado. Esos 404 de raíz prueban transporte, no denegación de cuenta.
No se puede separar con esta única intervención si lo determinante fue el DNS
explícito, descartar el estado Quick Boot anterior o su combinación.

**No demostrado:** que ese DNS fuera la causa de todos los congelamientos previos.
Antes de corregirlo ya funcionaban navegación y scroll locales; las acciones online
sí dependen de RTDB disponible. No se encontró un bloqueo del hilo principal.
La evidencia anterior M14 contiene ANR de inicio de Basta responsive, phone y GMS;
no es correcto atribuir todos esos fallos a código de Basta. La presión de memoria
del host es un factor probable, no una causa aislada experimentalmente.

## 2. Evidencia de ejecución

- Basta normal abrió desde SplashActivity por la ruta pública Android. Home no
  es exportada y ADB rechazó abrirla directamente; no se eludió esa restricción.
- Tienda cambió realmente de pantalla y mostró dos productos visibles. Perfil y
  su selector de avatar respondieron; se mostraron avatares reales tras recuperar
  DNS. Se cerró el selector con Cancelar, sin guardar cambios.
- Crear sala permite scroll hasta los controles y la pulsación + cambió 1 a 2.
  Unirse permite enfocar el campo, abrir teclado y escribir AB1234. No se envió
  ninguna solicitud de crear/unirse, compra ni partida a producción.
- El hilo principal de Basta estaba en do_epoll_wait antes y después: espera de
  eventos del Looper, no evidencia de un await Firebase bloqueante en esos momentos.
- dumpsys activity lastanr: sin ANR desde el arranque, tanto antes como después
  de la intervención. No existe un stack ANR actual de congelamiento reproducido.
- Muestra final: CPU app 0.0%; PSS 116019 KB (~113 MiB), heap Java 13136 KB.
  No hubo OOM de la aplicación observado en estas pruebas.

## 3. Las tres firmas/certificados rechazados

idea.log contiene tres diálogos próximos a las 08:51:32, 08:51:38 y 08:51:39:
ConfirmingTrustManager/CertificateWarningDialog, sujeto y emisor
OID.2.5.4.41=SSLA-Self-Signed-Certificate. Después aparecen mensajes de rechazo
recordado. A las 08:52:52 el IDE registra fallo PKIX en GitDownloadAndInstall al
consultar la versión más reciente de Git, con SSLHandshakeException.

Esto identifica avisos de confianza TLS del IDE, NO tres firmas APK de Google
Play Services ni tres licencias SDK. No demuestra que las tres ventanas recordadas
por el usuario fueran exactamente esas, ni identifica tres componentes distintos.
No se registran aquí URL, huella y cadena completas de cada ventana; se desconoce
el emisor real del certificado y por qué la UI solo ofrecía Reject.

Rechazar una conexión no demuestra una desinstalación o revocación de GMS. No se
halló evidencia de instalación SDK incompleta causada por esas ventanas. Hoy las
cuatro conexiones HTTPS públicas también funcionan desde JBR sin importar nuevos
certificados. No se demostró relación causal entre ese rechazo y el DNS del AVD.
McAfee y Defender figuran en el centro de seguridad; no se atribuye a ninguno la
emisión del certificado sin evidencia adicional.

Procedimiento seguro: conservar URL/huella/cadena/fechas si vuelve el aviso,
verificar origen con el proveedor de red o seguridad y solo entonces evaluar una
CA legítima. No se aceptó el certificado, no se cambió trust store y no se desactivó
TLS, App Check, antivirus ni validación de nombres.

## 4. Emulador, Android Studio y SDK

- Studio 2026.2.2, build AI-262.10968.63.2622.16840690.
- Android Emulator 37.2.13; Platform Tools 37.0.1; Build Tools 36.0.0.
- Plataformas instaladas 36.1 y 37.0. Un AVD disponible: Medium_Phone_API_37.0.
- Imagen Google Play x86_64, Android 17/API37, revisión 7, extensión 22;
  build CE2A.260420.051.A2 / 16418371. Requiere emulator >=37.2.11: instalado cumple.
- Configuración conservada: 2048MB RAM, CPU configurada 8, GPU auto, heap 336MB.
- Play Services 26.43.30 y Play Store 54.1.11 presentes y habilitados; enabled=0
  de GMS es estado predeterminado, no deshabilitado.
- /data: 10GB, 1.9GB usados, 7.6GB libres. Relojes Android/Windows difieren ~1s.
- Settings abrió en ~1.45s. Chrome estaba en primer inicio; no acepté sus términos
  ni lo usé como prueba de navegación ya configurada. La sonda JVM sí prueba HTTPS.
- Host al inicio: ~1.1GB de memoria virtual libre; procesos Studio ~2GB, AVD ~3.3GB,
  otro Java ~0.9GB. Al final: ~1.8GB virtual libre. No se cerraron procesos del usuario.
- No se reinstaló SDK ni se aceptaron licencias/certificados; no Wipe Data, borrado
  de AVD, snapshots ni datos. La incidencia oficial Play Protect API37 anterior a
  revisión 5 no coincide con la revisión 7 instalada; no justifica reinstalarla.

## 5. Firebase y caché de avatares

HomeActivity crea también PerfilFragment oculto. PerfilFragment.onCreateView llama
inicializarOpcionesAvatares: once getDownloadUrl asíncronos, incluso antes de abrir
el selector. Sus fallos generan avisos globales, actualmente incluso de tipo SUCCESS.
Eso explica ruido y avisos fuera de Perfil, pero no prueba un bloqueo del Looper.
No hay bucle de reintentos infinito implementado allí; los SDK realizan sus reintentos.

El código usa callbacks, Glide y caché de imágenes, no esperas de red en el hilo UI.
No se encontraron Tasks.await/Thread.sleep en las rutas de producción revisadas.
Las coincidencias await de pruebas no se confunden con código de la aplicación.

- Storage: el fallo DNS demostrado impide llegar al servidor y es compatible con
  los errores de red/reintentos reportados. -13030 indica límite de reintentos, no
  demuestra por sí solo una regla denegada. Tras la intervención se mostraron los
  avatares del selector, sin nuevos StorageException observados durante la muestra.
- Auth: no se resetearon cuentas ni se usaron contraseñas de producción. Se probó
  mediante el cliente de laboratorio ya instalado y Auth Emulator local (ver abajo).
- RTDB: el aviso Conectando desapareció en la aplicación real. No se probaron
  escrituras de salas ni se verificaron reglas reales con una cuenta administrativa.
- App Check: la advertencia No AppCheckProvider installed sigue apareciendo, pero
  no impidió la carga observada de avatares. No se comprobó el enforcement en consola;
  no se declara habilitado/deshabilitado ni se cambió seguridad.
- attributionTag: sin excepción fatal o ANR asociado demostrado. No se añadió un
  atributo al manifest para ocultar una advertencia sin causa confirmada.

No se identificó bucket incorrecto, denegación HTTP de reglas ni sesión inválida
como causa del bloqueo. No se asegura que esas configuraciones estén auditadas:
solo se informa lo observado y no se atribuye a App Check el problema por su aviso.

## 6. Código y regresiones

Se contrastaron git diff y PROJECT_STATE. El diff acumula varias misiones; no se
atribuyeron todos los cambios pendientes a M13/M14. M14 reorganiza layouts y ancho
de tarjetas; M13 distingue red/servicio y las operaciones online continúan teniendo
guardas. La reparación Auth retiró el requisito RTDB de Login/Registro.

No se observó overlay transparente bloqueando toda la pantalla ni bucle de UI. El
selector de avatar sí es modal, con Cancelar funcional. Las guardas de acciones
online pueden impedir operar cuando RTDB no está disponible: no equivale a un
hilo principal congelado. No se cambió código del juego por una hipótesis.
Graphify: consulta enfocada HomeActivity, sin reconstrucción ni actualización AST.

## 7. Corrección aplicada y reversibilidad

Solo se cambió la forma de arrancar el AVD en esta sesión:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_37.0 -dns-server 8.8.8.8 -no-snapshot-load -no-snapshot-save
```

Ejecutar únicamente cuando ese AVD esté detenido, sin abrir un segundo. La sesión
actual usó además las opciones de ventana/grpc del IDE. No se editó config.ini ni
DNS de Windows. Es un arreglo de arranque TEMPORAL: volver a Quick Boot sin flags
puede recuperar el estado anterior. No elimina datos de usuario ni snapshots.

Archivos nuevos: esta carpeta, DiagnosticoRed.java (sonda mínima, no incluida en el
APK), auth-instalado.cmd y resultados resumidos. PROJECT_STATE se amplía. No cambios
a app/src/main, Gradle del juego, reglas, dependencias ni APK; no se recompiló Basta.

## 8. Pruebas y límites

PASA: DNS/HTTPS tras corrección (cuatro dominios), Settings, arranque y navegación
real de Basta, Tienda, Perfil/avatares, Cancelar, scroll/contador Crear y escritura
Unirse. Sin nuevos ANR observados. No se enviaron compras, códigos ni salas reales.

PASA: AuthFlujoTest del APK de laboratorio previamente instalado. Auth/RTDB Emulator
oficiales locales, demo-basta-auth, sin compilación. OK (1 test), 8.863s: pulsaciones
Espresso, registro local/perfil, contraseña incorrecta/reintento, regreso de Home,
login con RTDB local offline y registro duplicado. Comprueba Intent a Home mediante
monitor; no es una prueba de alta real en producción. Servidores locales detenidos;
cliente de laboratorio detenido al terminar. Pruebas con heap servidor 256MB.

FALLÓ antes de la corrección: resolución DNS/HTTPS en Android. Primer intento de
script Auth no ejecutó pruebas por ruta local incorrecta; corregida y después pasó.

NO PROBADO: congelamiento total histórico (no reproducido), estrés de memoria,
fallo Storage aislado inyectado tras la corrección, nueva autenticación/registro
contra producción, reglas/enforcement de Firebase, multijugador, API25/tablet.
No se presenta la recuperación DNS como prueba de que se resolvió cualquier ANR.

## 9. Recomendación y evidencia

Mantener arranque limpio y DNS explícito para las próximas pruebas. Evitar tareas
pesadas simultáneas cuando la memoria virtual vuelva a estar cerca de agotarse.
Si se congela otra vez, recoger Logcat y dumpsys activity lastanr EN ESE MOMENTO,
antes de reiniciar, y capturar el hilo de la instancia afectada; no parchear el juego
ni aceptar certificados basándose solo en warnings.

Evidencia publicable: red-android.txt (fallo inicial), red-android-dns-explicito.txt,
red-jbr.txt, certificados-ide-resumen.txt, cpu-app.txt, anr-*.txt y auth-instalado.txt.
Logcat completo y UI con posible información privada: app/build/diagnostico-m15/
(ignorado por Git); no publicar esos archivos sin revisar. Incluye avatares.png.

Fuentes oficiales consultadas:
- [Android: problemas DNS y revisión de imagen API37](https://developer.android.com/studio/run/emulator-troubleshooting).
- [Android: opciones DNS y snapshots](https://developer.android.com/studio/run/emulator-commandline).
- [JetBrains: significado y diagnóstico de certificados no confiables](https://support.jetbrains.com/hc/en-us/articles/40249269141394-How-to-troubleshoot-Untrusted-Server-Certificate-Warning).
- [Firebase: errores de Storage y límite de reintentos](https://firebase.google.com/docs/storage/android/handle-errors).
