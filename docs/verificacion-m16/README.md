# Misión 16 — Cierre de respuestas, presencia y ajustes puntuales

Fecha: 16–17 de noviembre de 2026. No hubo commits, pushes, despliegues, cambios de reglas reales ni escrituras de prueba en producción.

## Defectos comprobados y correcciones

### Pantalla detenida en 00:00

En `PartidaFragment.iniciarCronometro`, al vencer el plazo se llamaba una vez a `guardarRespuestasJugador` y se detenía el runnable. Ese método podía retornar por conexión no disponible. Un error de transacción restablecía `respuestasGuardadas`, pero no reiniciaba el recorrido. La apertura de revisión dependía de su callback local y de un timeout local iniciado desde ese mismo callback; no existía una observación persistente que recuperara esta transición. Es un defecto demostrado en el código capaz de producir el síntoma descrito. Sin la traza de aquella partida no se puede atribuir exclusivamente a una condición concreta de red del teléfono/emulador original.

Ahora `CierreRespuestas.aplicar` confirma transaccionalmente el cierre utilizando los campos existentes de partida/ronda, el plazo calculado por `PlazoRonda` y el reloj existente `revisionCategorias/ronda_N/reloj`. Cualquier miembro conectado puede confirmar el cierre, sin depender del host. Solo se publica una vez. Se conserva la espera máxima existente de cinco segundos para recibir respuestas, medida desde el vencimiento compartido; puede cerrarse antes cuando los conectados ya enviaron. No se crean respuestas ficticias para los ausentes.

`PartidaFragment` observa persistentemente la sala y abre la revisión al recibir ese comprobante, independientemente del callback de envío. Recupera también un comprobante ya existente tras recreación. Los reintentos consultan el estado cacheado y solicitan una transacción; nunca fuerzan una pantalla por un timeout local. Los errores de lectura de respuestas permiten reintentar la apertura. Las respuestas posteriores al comprobante siguen rechazadas por la protección existente. No se modificaron duraciones de votación, reglas de puntuación ni recompensas.

Las escrituras directas de `estaListo` y `entradaPartida` también podían reaparecer después de retirar un UID. `InicioPartida.registrarEntrada` las sustituye por una operación condicionada a membresía, partida y ronda vigentes; no reinicia timestamps ya confirmados.

### Salida offline, recreación y 90 segundos

El botón de abandono podía confiar en `conectadoServidor` obsoleto y esperar `onDisconnect().cancel()` antes de navegar. Además, `abandonarSesion` rechazaba cualquier otra sesión, incluso histórica y desconectada. El identificador de partida de presencia no se actualizaba al iniciar otra partida. Durante recreación podía faltar `sesionActual` y no guardarse ningún ticket; esto se reprodujo con la Activity real.

Ahora abandonar guarda primero un ticket local y navega sin esperar RTDB. Conserva las sesiones propias y el identificador de partida al recrearse. El ticket incluye únicamente datos locales adicionales (`sesionesConocidas`, `sesionesPropias`), no campos nuevos de RTDB. La reconciliación comprueba sala, partida, UID y sesiones: puede retirar las sesiones propias de la Activity anterior y actual; protege sesiones desconocidas o conexiones activas ajenas a ese conjunto. No elimina un ticket nuevo desde el callback de uno anterior.

El registro de sesión es transaccional y vuelve a comprobar membresía, expulsión, reserva y versión de conexión. Un callback antiguo no restablece una membresía expirada. Cerrar una sesión offline ya no encola un timestamp nuevo que al reconectar prolongaría la gracia. La reserva consulta los timestamps de las sesiones aunque el agregado del jugador todavía esté atrasado. Se conserva la expiración de 90000 ms y la reconciliación/transferencia de host existentes.

**Límite importante:** `TransferenciaHost.expirar` ya podía retirar correctamente una ausencia confirmada. No se demostró que su comparación aritmética fuese la causa de los dos minutos observados. El plazo cuenta desde el timestamp de desconexión registrado por RTDB, no necesariamente desde el instante de apagar Wi-Fi. Un corte silencioso puede tardar en ser detectado. La eliminación sigue necesitando un cliente conectado y permisos efectivos para realizar la transacción; no hay un ejecutor servidor implementado en este proyecto.

### Tienda e icono

- Descripción con `minLines=3`, sin insertar saltos en los datos ni limitar el máximo de líneas. El adaptador mide el contenido más alto del carrusel para el ancho y fuente actuales y aplica esa altura a todas sus tarjetas. La descripción absorbe el espacio restante y deja inventario, precio y COMPRAR alineados abajo. Si hacen falta más de tres líneas, crece todo el carrusel. Se invalida la medida al reciclar otra categoría.
- Las compras confirmadas ya tenían sonido agrupado. Los avisos de error/saldo/inventario de tienda usaban `showGlobal` sin reproducción. Ahora utilizan `avisoTienda`, con el mismo recurso y `Sounds.playNotificaciones`, y un límite de un sonido por ráfaga de cuatro segundos compartido con el grupo de compras. No se cambia volumen ni modo silencio y no se agregan recursos de audio.
- Icono de la cápsula Comodines: 18 → 20 dp. Texto, ancho, altura, colores, posición y comportamiento conservados.

## Evidencia ejecutada

- Unitarias: **159 pruebas, cero fallos y cero errores**. Incluyen cierre independiente del callback, rechazo de operaciones repetidas/viejas, plazo original, ausencia sin respuesta inventada, entradas tardías, sesiones propias y protección de sesiones nuevas.
- RTDB Emulator `demo-basta-concurrency`: **34 pruebas, cero fallos**. Se conserva, sin cambiarlo, el filtro anterior del diagnóstico `conexionAnteriorNoDebeDesconectarOtraSesionActiva`: utiliza el protocolo obsoleto de escritura directa sobre jugador y NO está incluido en estas 34 ni se presenta como superado. Clientes SDK independientes `a` y `b`, transacciones reales del emulador y fixtures locales. Cierre concurrente, recepción retrasada, listener retirado/recreado, host ausente, mismo índice/reloj recuperado y conservación de respuestas/puntos. `onDisconnect` real; límites 89999/90000/90001 ms con tiempo controlado, reconexión frente a expiración, sesión antigua, salida concurrente y cierre con un participante. No equivale a esperar físicamente 90 segundos en un teléfono sin Wi-Fi.
- AVD API 37, Auth/RTDB `demo-basta-auth`: `Mision16SesionTest`, **1 prueba**, abre revisión, recrea la Activity, recupera el diálogo persistido, abandona offline en menos de 1500 ms, verifica `isFinishing`, intención de navegación, ticket y retirada del UID al volver RTDB. Home se intercepta para evitar inicializar servicios ajenos; una Activity de registro vacía mantiene el foreground durante la reconciliación. No se pulsa registrar en esa pantalla auxiliar.
- `AuthFlujoTest`: **1 prueba**, registro institucional local, perfil, contraseña incorrecta y reintento, regreso desde Home, login con RTDB offline, registro duplicado y liberación de botones. La navegación exitosa se comprueba con un monitor de Home; no es autenticación contra producción.
- `Mision16InterfazTest`: **3 pruebas**, 320/360/600 dp y fuente 100/130 %, misma altura y posición de COMPRAR, descripción larga completa, reciclado de categoría, icono de 20 dp/cápsula de 48 dp, recurso de audio cargado y AudioTrack activo tras aviso de tienda, sin repetir la ráfaga. Confirmado por Logcat: cero pruebas ignoradas. Capturas sintéticas inspeccionadas: `tienda-320-fuente130-{corta,larga}.png`. No son capturas de un ZTE físico.
- Resultados instrumentados finales: `sesion-avd.txt`, `auth-avd.txt`, `interfaz-avd-final.txt`. `interfaz-avd.txt` conserva la pasada previa aislada sin Internet, también satisfactoria.

## Incidencias del laboratorio, resueltas sin alterar producción

El AVD retenía instalación e instrumentación; se reinició el mismo AVD en frío, sin borrar datos, sin guardar/restaurar snapshots y con el DNS explícito de M15. No se desactivaron verificadores ni se aceptaron certificados. Un intento de packaging falló porque el cliente ADB retenía el archivo del APK; se detuvo exclusivamente ese cliente propio y las compilaciones posteriores terminaron correctamente.

En el SDK Android RTDB 22.0.1, aplicar el emulador modifica `RepoInfo`, usado como clave del caché. El runner antiguo podía crear una segunda instancia no redirigida y quedar en lobby. Se verificó el bytecode local y se fijó la URL local en `AuthRunner` desde la inicialización, con una aserción de identidad de instancia. Esto afecta solamente al laboratorio.

Otra medición del transporte mostró `host=10.0.2.2:9000`, `cache=127.0.0.1:9000`: el servidor local anuncia localhost y la reconexión de Android intentaba su propio loopback. El script crea temporalmente `adb reverse tcp:9000 tcp:9000` y lo retira al terminar. Con ese puente pasaron la reconciliación y todas las pruebas instrumentadas. No se atribuye este detalle del emulador a la red Firebase real.

Las reglas `m16.test.rules.json` son fixtures permisivos de sala autenticada para probar el algoritmo, **no son las reglas desplegadas ni una propuesta de seguridad lista para producción**. Las garantías frente a clientes manipulados y los permisos de lectura/escritura de transacciones completas siguen sin verificarse contra las reglas reales.

## Qué requiere servidor y autorización posterior

No hay código de Cloud Functions/Tasks desplegable de limpieza implementado en este proyecto. Propuesta mínima, no implementada: trigger RTDB al quedar sin sesiones conectadas; encolar una tarea con identificadores de sala/partida/UID y timestamp observado para después del vencimiento; ejecutar una transacción que vuelva a comprobar el mismo intervalo y ausencia de sesiones vigentes, retire al participante y reconcilie host/cierre. Reintentos y duplicados deben ser idempotentes. La tarea debe ignorar reconexiones, partidas nuevas y timestamps sustituidos.

Esto permitiría ejecución sin teléfonos conectados; la entrega puede retrasarse y no ofrece tiempo real exacto a los 90 segundos. Para limitar también aplicaciones congeladas o conexiones TCP que aún parecen vivas se necesita un lease/heartbeat con campos y reglas nuevos, pendiente de diseño/autorización. Una tarea periódica por minuto es otra opción, con retraso adicional.

Cloud Functions requiere facturación Blaze. Cloud Tasks tiene cuotas gratuitas y cobro por operaciones posteriores; también cuentan ejecución de funciones, tráfico/RTDB, logs y reintentos. No se habilitó ningún servicio ni facturación. Referencias oficiales consultadas el 17/11/2026:

- https://firebase.google.com/docs/database/android/offline-capabilities
- https://firebase.google.com/docs/functions/task-functions
- https://cloud.google.com/tasks/pricing
- https://firebase.google.com/pricing

## Archivos de la misión

Producción local, bajo `app/src/main/`:

- `java/com/example/basta_game/Fragments/PartidaFragment.java`
- `java/com/example/basta_game/SalaActivity.java`
- `java/com/example/basta_game/utils/{CierreRespuestas,InicioPartida,IngresoSala,TransferenciaHost,SalidasPendientes,NotificationHelper,ComprasViewModel}.java`
- `java/com/example/basta_game/Fragments/TiendaFragment.java`
- `java/com/example/basta_game/Adapters/ComodinTiendaAdapter.java`
- `res/layout/{item_comodin_tienda,fragment_partida}.xml`

Pruebas/configuración local:

- `app/src/test/java/com/example/basta_game/utils/{CierreRespuestasTest,SalidaSesionTest}.java`
- `app/src/androidTest/java/com/example/basta_game/{Mision16SesionTest,Mision16InterfazTest,AuthRunner}.java`
- `emulator-tests/build.gradle`
- `emulator-tests/src/test/java/com/example/basta_game/emulator/ConcurrenciaEmulatorTest.java`
- `emulator-tests/{firebase.m16.json,m16.test.rules.json,m16-android.cmd}`
- `docs/PROJECT_STATE.md`, este informe, resultados y dos capturas en `docs/verificacion-m16/`.
- Metadatos generados de `graphify-out/`: actualización incremental AST, sin LLM. El parser no comprendió dos regiones del DSL Gradle; no representa un error de compilación.

No se modificaron `LoginActivity.java` ni `RegistrarActivity.java`. Sus hashes permanecen iguales a M15. Los cambios anteriores, incluidos los preparados y no seguidos, se conservaron.

## Pendientes físicos y seguridad

Repetir la partida con teléfono + AVD actualizado; apagar Wi-Fi realmente durante cierre y revisión; medir desde el timestamp RTDB de desconexión hasta retirada; comprobar regreso antes/después del límite y sonido audible/mode silencio en ZTE/API25. No se probó físicamente esa plataforma. Las pruebas con dos clientes SDK y una Activity instrumentada no equivalen a dos interfaces Android ejecutadas simultáneamente. No se declara garantía absoluta de continuidad ni limpieza sin clientes/backend o sin revisar reglas desplegadas.

APK normal final: `app/build/outputs/apk/debug/app-debug.apk` (applicationId esperado `com.example.basta_game`). Los perfiles `.authtest` y `.responsive` son únicamente laboratorio y no deben distribuirse como versión del juego.

Compilación normal final: BUILD SUCCESSFUL (38 s), 27813397 bytes, SHA256 `886D92835AFEB82CB015D53408052EF3B8F288D1338866D7D99ADFC6B5FCA6CB`. No hubo ANR desde el arranque en frío (`anr-final.txt`). El puente ADB quedó retirado. `git diff --check` señaló whitespace anterior en PartidaFragment (zona de congelador no editada en M16); no se reformateó código ajeno a esta misión.
