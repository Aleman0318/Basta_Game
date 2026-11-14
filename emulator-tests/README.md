# Concurrencia RTDB: entorno local de la Mision 4

Proyecto aislado `demo-basta-concurrency`, RTDB en `127.0.0.1:9000`, sin credenciales
reales ni Application Default Credentials. Las pruebas rechazan otro host y otro
proyecto. Firebase CLI inicia y detiene el emulador; no ejecuta despliegues.

## Preparacion y ejecucion (PowerShell, desde esta carpeta)

Requiere Node compatible con Firebase CLI y Java 21. Herramientas utilizadas:
Firebase CLI 15.36.0, Database Emulator 4.11.2 y Admin Java SDK 9.12.0.
El CLI se instala solo en esta carpeta y el SDK solo en este proyecto Gradle;
no se cambian dependencias de Android.

```powershell
npm.cmd ci --ignore-scripts --no-audit --no-fund
.\node_modules\.bin\firebase.cmd setup:emulators:database
..\gradlew.bat -p . prepareEmulatorTests compileTestJava --console=plain
$env:CI = 'true'
npm.cmd test
```

La preparacion necesita red para descargar herramientas oficiales y dependencias.
La ejecucion Gradle de las pruebas usa `--offline`. Si Gradle utiliza un cache
personalizado, conservar el mismo `GRADLE_USER_HOME` durante ambos pasos.

## Resultados comprobados

`npm.cmd test`: **12 pruebas, 0 fallos** en Mision 6 (11 en Mision 5, 8 en Mision 4), con dos conexiones RTDB independientes y
transacciones reales del servidor emulado. Se compilan directamente los archivos
Java de la app `RecompensasPartida`, `TransferenciaHost`, `InicioPartida` y
`PlazoRonda`; no se traduce ni simula su algoritmo.

- Recompensas concurrentes, evento repetido y reentrada: un solo pago, historial
  y marca; los datos anteriores del usuario permanecen.
- Elecciones concurrentes: un anfitrion canonico y una sola bandera de corona.
- Dos abandonos concurrentes y repeticion: conserva ronda, plazo, puntos y datos
  ajenos a la operacion.
- Conflicto provocado con otra escritura: el servidor exige un reintento real;
  se conserva el nuevo avatar y se paga una sola vez.
- Operacion encolada sin conexion: otro cliente paga; al reconectar, no duplica.
- Transaccion denegada por regla local: no publica recompensa, historial ni marca.
- `onDisconnect` real y timestamp servidor; expiracion con plazo avanzado como
  entrada, sin esperar 30 segundos reales; transferencia sin devolver corona.
- Ambos clientes leen el mismo inicio de ronda y calculan el mismo plazo.
- Mision 5: segunda partida con dos/tres participantes, avance concurrente y
  pulsaciones repetidas; rechazo de idPartida anterior y conservacion de puntos.
- Mision 5: abandono durante espera, avance sin el ausente y rechazo de un resumen
  tardio que intentaria reabrir el cierre del ultimo participante.
- Mision 5: reentrada de identidad concurrente con una escritura de puntuacion.

Tambien se compila directamente `IdentidadJugador`. Estos casos verifican helpers
y transacciones; no ejecutan los listeners ni botones Android.

Mision 6 agrega `VotacionRevision` y `EstadoRevision`: apertura concurrente de una
sola votacion, un voto por UID, cierre concurrente aplicado una sola vez, rechazo
de voto tras el cierre y de reapertura de palabra resuelta. Los votos del fixture
son todos "si"; se valida la coordinacion transaccional, no el dialogo ni un plazo
de votacion. El temporizador compartido y el maximo de cinco oportunidades por
categoria siguen pendientes de autorizacion de esquema/decision de reglas.

Informe HTML: `build/reports/tests/test/index.html`.

## Defecto reproducible de presencia (NO resuelto)

```powershell
npm.cmd run test:presence-limit
```

**1 prueba, 1 fallo; salida 1 intencionadamente visible.** No se incluye en la
suite principal porque verifica un requisito actualmente incumplido:
`conexionAnteriorNoDebeDesconectarOtraSesionActiva`.
Una conexion registra `onDisconnect` sobre `jugadores/h`; otra conexion escribe
presencia activa sobre ese mismo jugador. Al desconectar la primera, RTDB escribe
`estaConectado=false` aunque `/.info/connected` de la segunda sigue siendo `true`.
El fallo confirma el limite advertido en Mision 3; no es un error de eleccion
atomica. Las conexiones comparten un nodo de jugador, sin autenticar un UID real.

Resolverlo requiere distinguir sesiones y proteger la presencia frente a callbacks
antiguos, con diseno de esquema y reglas. No se agrega ese esquema en esta mision.
Informe: `build/reports/tests/presenceLimit/index.html`.

## Limites y seguridad

`database.test.rules.json` es exclusivamente un fixture local: permite operaciones
anonimas bajo `runs`, deniega escrituras bajo `denied` y rechaza otros caminos.
Solo la conexion de preparacion/limpieza usa privilegios administrativos; las dos
conexiones de prueba tienen override de autenticacion `null` y respetan esas reglas.
Cada caso limpia unicamente sus rutas con UUID dentro del emulador.
Estas reglas NO son las desplegadas. Su compatibilidad y seguridad no se verifican.

Se usa el SDK Admin Java para ejecutar los helpers, no el ciclo de vida Android:
no se prueban UI, Auth real, suspension/muerte del proceso, perdida del ACK tras un
commit, ni temporizadores de Android durante 30 segundos. El caso offline prueba
una operacion encolada, no todas las interrupciones posibles. No existe garantia
de avance cuando ningun cliente ejecuta el coordinador ni recuperacion del candado
de ronda adquirido antes de un cierre incompleto. No se valida un cliente web.

Referencias oficiales: [conexion a RTDB Emulator](https://firebase.google.com/docs/emulator-suite/connect_rtdb),
[Admin Java SDK](https://firebase.google.com/docs/admin/setup),
[override de autenticacion](https://firebase.google.com/docs/database/admin/start).
