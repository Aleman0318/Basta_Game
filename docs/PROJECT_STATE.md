# Basta! Online — estado del proyecto

Última actualización: 2026-11-12 (plantilla inicial; confirmar en el repositorio)

## Contexto conocido (confirmar técnicamente)
- Juego Basta! Online para la Semana de Sistemas.
- Hay una versión Android y un cliente web en desarrollo.
- Se ha usado Firebase para la sincronización; verificar producto concreto (Realtime Database, Firestore, Auth, etc.).
- Límite actual mencionado: 8 participantes por partida; verificar implementación.
- Existe un modo clásico que no debe dañarse.
- Se han mencionado mecánicas sociales, comodines, ruleta del caos y cambios de puntuación; comprobar cuáles están implementadas.

## Inventario técnico (rellenar tras inspeccionar)
- Carpeta/proyecto Android:
- Carpeta/proyecto web:
- Backend o funciones de nube:
- Firebase productos/configuración:
- Versiones JDK, Gradle, Android Gradle Plugin y SDK:
- Comandos de compilación y pruebas:
- Modelo de partida y fuente de verdad:
- Estado Android/web sincronizado:

## Funcionalidades estables
- [ ] Inicio y unión a salas
- [ ] Inicio de partida
- [ ] Rondas y temporizador
- [ ] Puntuación
- [ ] Reconexión
- [ ] Fin de partida

## Prioridades
1. Identificar y resolver sincronización Android/web.
2. Pantalla de espectador solo lectura con enlace por sala.
3. Modo Battle Royale separado del clásico.
4. Locuciones pregrabadas y animador visual liviano.
5. Ensayo integral en dispositivos reales y proyector.

## Decisiones registradas
- (Anotar: fecha, decisión, motivo, archivos implicados)

## Problemas conocidos y reproducción
- (Anotar: síntoma, pasos exactos, dispositivo, esperado, real)

## Último cambio comprobado
- (Anotar: commit, archivos, pruebas y resultado)

## Próximo paso concreto
- Auditar el proyecto en solo lectura y crear un mapa de componentes y flujo de una partida.


## Primera correcci?n de sincronizaci?n ? 2026-11-13

Alcance: exclusivamente el plazo del cron?metro Android. No se modifican esquema,
reglas ni datos de Firebase, dependencias, modo cl?sico, recursos visuales o sonidos.

### Evidencia y soluci?n

En PartidaFragment.iniciarCronometro el restante recibido se ampliaba 10000 ms y
se reconstru?a el vencimiento despu?s de otra consulta as?ncrona del offset.
recalcularCronometroConBasta calculaba BASTA + 10000 ms y volv?a a pasar por esa
ampliaci?n. Ejemplo sint?tico: inicio=1000, duraci?n=60 s, BASTA=21000;
fin esperado=31000, c?lculo anterior=41000 m?s demora de consulta.

Ahora PlazoRonda.calcularFin utiliza inicioRonda + tiempoRonda y, si existe,
el m?nimo con bastaTimeStamp + 10000. El bucle usa ese vencimiento absoluto y
el offset ya obtenido. La carga no agrega tiempo: el protocolo existente
confirma inicioRonda despu?s de la barrera de entrada. Una lectura atrasada
no puede ampliar un plazo ya recibido para la misma ronda. BASTA usa la misma
ruta que el inicio; al abrir la pantalla se consulta tambi?n su timestamp.
Se cancelan los callbacks del reloj al destruir la vista.

Compatibilidad: solo se leen campos existentes; no hay migraci?n. Clientes Android
anteriores todav?a a?aden tiempo, por lo que la correcci?n no garantiza plazos
iguales en una sala con versiones mezcladas. Web no est? en este repositorio.
El fallback sin offset conserva el vencimiento, pero depende del reloj local.
No se implementa todav?a rechazo autoritativo de respuestas tard?as.

Archivos: Fragments/PartidaFragment.java, utils/PlazoRonda.java,
app/src/test/java/com/example/basta_game/utils/PlazoRondaTest.java y este documento.
Graphify actualiza sus artefactos derivados en graphify-out/.

### Validaci?n local

- Regresi?n del c?lculo anterior: cinco pruebas JUnit contra una adaptaci?n temporal
  de su expresi?n original; cuatro fallan por los 10000 ms adicionales. Esta es
  una reproducci?n matem?tica, no una reproducci?n con clientes Firebase.
- PASA: gradlew.bat --offline testDebugUnitTest assembleDebug --console=plain,
  con GRADLE_USER_HOME=C:/Users/cura/.gradle. Cinco pruebas nuevas y la prueba de
  ejemplo existente, cero fallos; APK debug generado. Avisos de APIs/Gradle obsoletos.
- PASA: git diff --check del archivo Java modificado.
- PASA: Graphify update mediante C:/Users/cura/.local/bin/graphify.exe (fuera del PATH).
  El extractor no cubre XML; se verificaron los hallazgos directamente en Java.
- NO PROBADO: dispositivos, Firebase real/emulado, reconexi?n completa, web,
  permisos desplegados y presentaci?n audiovisual. No se alter? producci?n.

### Escenarios revisados y trabajo pendiente

| Escenario | Evidencia de c?digo y estado |
|---|---|
| Dos BASTA simult?neos | configurarBotones usa transacci?n sobre bastaPresionado: ya elige uno. Autor y timestamp se publican despu?s, en otra escritura; queda ventana incompleta si falla. Se corrige solo el plazo posterior. |
| Dos comodines simult?neos | BaseComodin.descontarInventario lee y escribe cantidad; consumo/efecto no son at?micos. Ruleta/Cambio de Flujo escriben puntos absolutos y Marca sobrescribe minijuegoActual. Pendiente. |
| P?rdida de conexi?n | Crear/Unirse programan onDisconnect().removeValue(); SalaActivity intenta gracia local de 10 s. Pendiente conciliar presencia y recuperaci?n. |
| Host cae al cerrar | cerrarRondaComoHost marca rondasProcesadas antes de completar puntuaci?n; no hay recuperaci?n completa de ese trabajo. Pendiente. |
| Jugador no vota | revisarSiTodosVotaronYResolver exige todos los votos; no tiene vencimiento. No existe un tiempo establecido efectivo en este flujo. Pendiente definir pol?tica sin cambiar reglas silenciosamente. |
| Respuestas tard?as | guardarRespuestasJugador escribe sin comprobar un vencimiento autoritativo. El reloj corregido no agrega tiempo, pero no sustituye validaci?n del servidor. Pendiente. |
| Cerrar y reabrir aplicaci?n | SalaActivity entra por Bundle y muestra lobby; no hay recuperaci?n completa persistida de partida. El c?lculo nuevo permite recuperar un plazo existente cuando se abre PartidaFragment; no resuelve navegaci?n/reincorporaci?n. |
| Eventos duplicados | Hay banderas/IDs locales, pero eventoCaosGlobal conserva solo un evento por ronda. Orden, replay y deduplicaci?n persistente pendientes. |
| Resultados tard?os | guardarMiHistorialUsuarioFinal usa bandera local y guardarMiHistorialEnUsuario crea push e incrementa estad?sticas. Puede repetirse tras recreaci?n; adem?s otro cliente puede resetear la sala. Pendiente. |

Los anteriores son hallazgos est?ticos; no se afirma reproducci?n multicliente.
Siguiente prioridad propuesta: cierre recuperable e idempotencia de puntuaci?n y
recompensas. Si exige cambiar el esquema, explicar y solicitar autorizaci?n antes.

### Guion manual pendiente (2 y 8 jugadores)

1. Usar el mismo APK nuevo en todos los equipos; configurar 60 s y dos rondas.
2. Registrar por cliente: sala, ronda, inicioRonda, bastaTimeStamp, offset,
   instante de recepci?n y env?o de respuestas. No registrar respuestas privadas.
3. Sin BASTA, comprobar fin=inicioRonda+60000 en todos; no reaparecen 10 s.
4. Dos jugadores pulsan BASTA a la vez: comprobar un ganador y
   fin=min(inicioRonda+60000, bastaTimeStamp+10000).
5. Repetir con BASTA en los ?ltimos cinco segundos: nunca prolonga la ronda.
6. Retrasar la recepci?n de un cliente y recrear la vista: conserva el vencimiento,
   no vuelve a diez segundos ni duplica el env?o dentro de la misma instancia.
7. Repetir BASTA/lecturas de estado: ninguna lectura anterior ampl?a el plazo.
8. Tras destruir la vista, comprobar que no contin?a el reloj anterior; pasar a la
   segunda ronda y verificar que utiliza el nuevo inicio y conserva el sonido final.
9. Con ocho jugadores repetir bajo latencia y una desconexi?n. Registrar bloqueos
   de presencia/votos/resultados por separado: no son funcionalidades corregidas.


## Mision 2 - diagnostico de duplicacion (2026-11-13)

Estado: analisis completado; implementacion de recompensas pendiente de autorizacion
explicita del cambio aditivo de esquema. Se conserva intacta la correccion del reloj.
No se ha cambiado Java, dependencias, reglas ni datos de Firebase en esta mision.

### Defecto comprobable en el flujo activo

PartidaFragment.mostrarModalPuntuaciones invoca guardarMiHistorialUsuarioFinal
al recibir puntuaciones finales. miHistorialFinalGuardado vive en la instancia del
Fragment. Una nueva instancia empieza en false. guardarMiHistorialEnUsuario genera
un push nuevo por llamada y ejecuta incrementos de partidasJugadas,
puntosTotalesHistoricos, partidasGanadas y monedas. No compara una identidad
persistida de la partida antes de incrementar. Por tanto dos ejecuciones aceptadas
para el mismo resultado producen dos historiales y dos incrementos. La repeticion
del listener dentro de una misma instancia SI tiene bandera local; no se afirma
que cada notificacion Firebase duplique por si sola las recompensas.

### Puntuaciones y concurrencia existentes

- Base: validaciones/ronda_N -> 0/5/10 por categoria, mas Doble o Nada.
- Resultado por ronda: puntajesRondas/ronda_N/uid se escribe como valor absoluto.
- Acumulado: actualizarPuntajesAcumulados usa una transaccion por escalar y suma
  puntosGanados. Es atomica, pero dos invocaciones distintas suman dos veces.
- Entrada al cierre: cerrarRondaComoHost usa transaccion sobre rondasProcesadas.
  Dos cierres normales compiten por ese candado y solo uno continua. No hay evidencia
  local de que el SDK repita onComplete exitosamente como dos operaciones distintas.
- El candado se confirma antes de terminar el trabajo: caida del host puede dejar
  cierre incompleto. Es un defecto de recuperacion, no prueba de duplicacion.
- Resultados/historial suman puntajesRondas; comodines modifican esa fuente despues
  de calcular el acumulado. No se cambia ese comportamiento en esta primera entrega.
- guardarHistorialYRecompensasSiHaceFalta tiene un candado global, pero solo se
  encuentra su declaracion: no protege el flujo personal activo de recompensas.

### Identificadores

idSala identifica una sala reutilizable, no una partida. ronda_N vuelve a existir
en cada partida. inicioRonda cambia por ronda y se limpia; fechaCreacion es de sala.
Los idPartida existentes son generados por push al guardar historiales (uno por
usuario/llamada en el flujo activo), no un ID compartido estable. No hay identidad
adecuada para deduplicar una recompensa a traves de recreaciones y clientes.

### Cambio concreto propuesto, AUN NO IMPLEMENTADO

1. Generar salas/idSala/idPartida una sola vez al iniciar una partida nueva.
2. Usar ese mismo ID como clave de historialPartidas del usuario.
3. Una transaccion en usuarios/uid comprueba recompensasAplicadas/idPartida y,
   solo si falta, guarda historial, marca e incrementos juntos. Ningun efecto
   externo dentro de doTransaction, pues Firebase puede reintentarlo.
4. Mantener recompensasAplicadas separado de historialPartidas: el historial
   visible se recorta a cinco entradas y no debe borrar la deduplicacion.
5. Sin ID valido, no generar un ID alternativo ni pagar silenciosamente partidas
   antiguas: mostrar/registrar el estado pendiente. No migrar ni reparar saldos reales.

Compatibilidad: campos aditivos; el formato visible del historial y las reglas de
recompensa se conservan. Clientes anteriores no comprueban la marca y pueden seguir
pagando por fuera de esta proteccion. Usar todos los clientes actualizados y salas
nuevas para validar; web no esta disponible en este repositorio. Las reglas
actualmente desplegadas podrian rechazar los nuevos campos o la transaccion en
usuarios/uid: revisar su contenido antes de desplegar, sin cambiarlas sin permiso.
Alcance previsto: LobbyFragment, flujo personal de historial en PartidaFragment,
pruebas de logica de transaccion y este documento; modelo Sala si requiere mapear ID.

### Validacion y siguiente paso

- Graphify consultado y hallazgos verificados directamente en Java.
- PASA verificacion offline: testDebugUnitTest assembleDebug, BUILD SUCCESSFUL;
  las 41 tareas quedaron UP-TO-DATE. Conserva las seis pruebas locales anteriores.
- NO PROBADO: carreras reales, Firebase emulado/produccion y reentrada en dispositivos.
- Pendiente al autorizar: pruebas repeticion, misma partida en dos solicitudes,
  nueva partida, marca conservada al recortar historial y retry de transaccion.
  Las simulaciones locales no se reportaran como prueba de concurrencia Firebase.
- Guion real pendiente (2/8 jugadores): final, reabrir resultados, dos sesiones del
  mismo UID, desconexion tras enviar recompensa y nueva partida en la misma sala.
  Verificar una marca/un incremento por uid/idPartida, sin tocar produccion.


## Mision 2 completada localmente - 2026-11-13

Esta entrada sustituye el estado pendiente de autorizacion y el diagnostico de
implementacion parcial anteriores, que se conservan como historial. El usuario
ha autorizado expresamente idPartida, recompensasAplicadas y las transacciones.
Estado final: implementacion local integrada, pruebas y APK debug verificados;
no desplegada ni verificada contra un servidor Firebase.

### Cambios implementados

- LobbyFragment genera un push ID local antes de iniciar, sin escribir un nodo
  auxiliar. La transaccion sobre salas/idSala confirma idPartida y el inicio juntos.
  InicioPartida comprueba estado de espera, host actual y jugadores listos dentro
  del snapshot transaccional. Una segunda solicitud no puede reemplazar el ID
  de una partida ya iniciada. Otra partida en esa sala recibe otro ID.
- El lobby transmite ID_PARTIDA en el Bundle que LoadingPartidaFragment conserva;
  PartidaFragment utiliza esa identidad en resultados. Sala mapea el campo opcional.
- guardarMiHistorialEnUsuario ya no genera push por entrada a resultados ni utiliza
  incrementos separados. Guarda historialPartidas/idPartida, monedas, estadisticas
  y recompensasAplicadas/idPartida en una transaccion sobre usuarios/uid.
- RecompensasPartida comprueba la marca ANTES de modificar el snapshot. Si ya existe,
  aborta sin volver a incrementar ni sustituir el historial. Los reintentos usan
  el snapshot recibido por Firebase, no un saldo capturado por fuera.
- El payload se construye antes del handler. doTransaction no ejecuta red adicional,
  callbacks de UI, sonidos, notificaciones ni escrituras fuera de su MutableData.
  Se desactivan eventos locales intermedios de estas transacciones.
- Los calculos se validan antes de escribir: identidad, UID, monedas no negativas,
  posicion y overflow. Los puntos negativos permitidos por las reglas se conservan.
- La marca se conserva separada del historial visible de cinco entradas. La poda
  del historial nunca borra recompensasAplicadas. Reentrada tras poda no vuelve a pagar.
- Ante error no se informa exito: se libera el estado en proceso y se muestra aviso.
  El boton existente de regreso ofrece reintentar o salir si el pago no se confirmo.
  No se deja al usuario encerrado indefinidamente si no hay conexion o permisos.
- Una partida sin ID valido o con resultados de otra partida NO genera un ID de
  respaldo ni entrega monedas. Las partidas antiguas pueden salir al lobby con aviso.
- El resultado debe coincidir con el ID capturado, ronda final y fase resultados_finales;
  una lectura posterior al reset no se interpreta como una recompensa nueva.

### Escrituras de usuario revisadas

- PerfilFragment: updateChildren solo de avatarID/fotoPerfilUrl; preserva marcas.
- TiendaFragment: transaccion sobre el usuario que modifica saldo e inventario
  mediante child(); conserva los demas hijos. La prueba de conflicto simulado
  con compra verifica el recalculo sin perder inventario ni marcas anteriores.
- BaseComodin: escritura puntual de cantidad; no sustituye el usuario completo.
- limitarHistorialUsuarioA5: elimina solo historialPartidas/clave, no las marcas.
- RegistrarActivity: setValue del perfil ocurre exclusivamente tras crear una
  cuenta Auth NUEVA con ese UID. No se usa para editar o regrabar usuarios existentes.
- Cadena global antigua guardarHistorialYRecompensasSiHaceFalta: se encontro solo
  su declaracion, sin llamada en el flujo actual. Su escritor antiguo no borra marcas,
  pero NO debe reactivarse sin adaptarlo al mismo contrato de recompensa. Se conserva
  para evitar una eliminacion/refactorizacion ajena a esta entrega.

### Compatibilidad y reglas

Los unicos campos nuevos de Firebase son salas/idSala/idPartida y
usuarios/uid/recompensasAplicadas/idPartida. El historial mantiene su forma y
utiliza ahora el ID compartido como clave; HistorialFragment sigue leyendolo.
No cambian formulas, puestos, limites de monedas, sonidos, animaciones ni layouts.
No se altero la correccion anterior del plazo.

Todos los clientes participantes deben actualizarse. Los clientes antiguos no
comprueban marcas y pueden pagar por fuera del protocolo; una sala mixta NO tiene
proteccion global demostrada. Web sigue ausente de este repositorio.

No existen reglas Realtime Database versionadas ni firebase.json en este checkout.
No se consultaron reglas desplegadas ni se leyeron/escribieron datos de produccion.
Pendiente revisar .read/.write y .validate efectivos en salas/idSala y usuarios/uid:
las transacciones requieren permisos en esos nodos, incluso si escrituras antiguas
por hijos estaban permitidas. Nuevos campos tambien pueden ser rechazados por
validaciones actuales. No se cambia seguridad ni se recomienda abrir permisos.

### Pruebas ejecutadas y resultados

1. Antes de corregir el helper parcial: testDebugUnitTest --tests '*RecompensasPartidaTest'
   ejecuto 9 pruebas; 4 fallaron por duplicados, reentrada, poda y concurrencia simulada.
2. Despues: gradlew.bat --offline testDebugUnitTest assembleDebug --console=plain
   termino BUILD SUCCESSFUL con 27 pruebas, cero fallos/errores/omitidas:
   - RecompensasPartidaTest: 16 (repeticion, reentrada, nueva partida, marca tras poda,
     fallos simulados de commit, reintentos, dos solicitudes con CAS local, cache vacia,
     perfil/inventario, timestamp, IDs/UID invalidos, overflow y saldo corrupto).
   - InicioPartidaTest: 5 (ID unico, conflicto/reintento simulado, siguiente partida,
     cambio de host/listos, cache vacia).
   - PlazoRondaTest: 5 anteriores, conservadas.
   - ExampleUnitTest: 1 anterior.
3. APK: app/build/outputs/apk/debug/app-debug.apk generado correctamente.
4. Android Lint: INTENTADO, NO COMPLETADO. lintAnalyzeDebug no pudo resolver offline
   androidx.graphics:graphics-shapes-desktop:1.0.1 y
   androidx.datastore:datastore-preferences-jvm:1.1.7. No se descargaron artefactos,
   no se actualizaron dependencias ni se desactivaron comprobaciones. No hay resultado
   de analisis Lint que permita declarar ausencia de errores.
5. git diff --check de los archivos Java modificados: sin errores de whitespace.
6. Graphify: grafo actualizado con update . y consulta final de los nuevos componentes;
   XML no esta cubierto por su extractor. No se instalaron extractores adicionales.
7. Avisos existentes de APIs obsoletas y compatibilidad futura con Gradle 10.

Las pruebas usan MutableData real del SDK en memoria y un auxiliar de test local.
CAS y descarte de snapshots SIMULAN conflictos y fallos; no prueban atomicidad del
servidor, reglas, desconexiones reales, callbacks de Firebase o concurrencia distribuida.
No se ejecutaron pruebas instrumentadas ni partidas en dispositivos.

### Limites y pendientes de validacion

- El codigo delega atomicidad/reintentos al SDK y al servidor Firebase; la evidencia
  actual es local, no una prueba de concurrencia Firebase real.
- Con la app viva, una transaccion pendiente puede continuar tras reconectar. Reentrar
  a resultados con el mismo ID vuelve a comprobar la marca. No hay cola duradera nueva
  para operaciones pendientes tras muerte de proceso; no se garantiza entrega si se
  abandona antes de confirmar y la sala se resetea. Se avisa antes de esa salida.
- Un participante puede resetear la sala antes de que otro haya leido su tabla final;
  recuperar resultados finales archivados para todos requiere trabajo posterior.
- Las marcas no caducan para no reabrir pagos antiguos: crece el nodo usuario y,
  con el tiempo, puede requerir archivado seguro sin borrar la deduplicacion.
- No se reparan historiales/saldos previos ni se migra una partida antigua en curso.
- Cierre recuperable de ronda, acumulados puntajePartida, votaciones y serializacion
  de comodines no fueron modificados. Son pendientes separados, no garantias de esta tarea.

Guion pendiente sin produccion: 2/4/8 participantes actualizados, misma cuenta en dos
sesiones, reabrir resultados repetidamente, desconectar durante commit, repetir tras
reconexion, denegar una escritura en un entorno de prueba, podar historial y empezar
otra partida en la misma sala. Verificar una marca y un incremento por UID/idPartida.
Tambien revisar la interfaz de reintento/salida y la proyeccion futura por separado.

Archivos de esta entrega: LobbyFragment.java, PartidaFragment.java, models/Sala.java,
utils/RecompensasPartida.java, utils/InicioPartida.java,
RecompensasPartidaTest.java, InicioPartidaTest.java, este documento y artefactos Graphify.
MutableDataLocal.java se reutiliza sin cambios. No hubo commits, despliegues,
operaciones de Firebase, instalaciones ni cambios en reglas. Mision 3/Battle Royale
no iniciados. Se detiene el trabajo al entregar el informe de esta mision.


## Mision 3 - primera etapa de transferencia automatica del host

Estado: implementada y compilada localmente. Las validaciones pendientes de
Firebase de las misiones anteriores siguen vigentes. No hubo cambios en reglas,
datos de produccion, dependencias, commits ni despliegues.

### Auditoria y defectos constatados en el codigo

SalaActivity ya intentaba reasignar host: no se encontro un if que eliminara siempre
la sala solo por salir su creador mientras hubiera otros jugadores. Sin embargo,
asignarNuevoHost, abandono y limpieza utilizaban lecturas previas y elecciones
aleatorias con updateChildren posteriores. Varios clientes podian aplicar resultados
calculados sobre listas diferentes. Esa eleccion no estaba protegida por transaccion.

PartidaFragment aceptaba esAnfitrion O esCreador como autoridad. Cambiar la corona
no revocaba el segundo permiso. TarjetasValidacionAdapter capturaba el permiso de
avance al construirse. SalaActivity cambiaba campos mediante reflexion y podia
recrear el lobby, sin retomar todas las responsabilidades de una revision existente.

Crear_salaFragment y Unirse_salaFragment programaban onDisconnect().removeValue(),
por lo que una desconexion borraba inmediatamente la membresia. onPause tambien
marcaba ausencia y expulsaba a los diez segundos, aunque la conexion siguiera viva.
Estos comportamientos se verificaron en fuente; no se afirma reproduccion real
multicliente del sintoma comunicado ni comprobacion contra Firebase de produccion.

### Flujo y autoridad actuales

salas/idSala/hostUID es la referencia administrativa; hostUid, hostNombre y
jugadores/uid/esHost se actualizan juntos en una transaccion sobre la sala.
TransferenciaHost utiliza exclusivamente campos ya existentes. No hay esquema nuevo.

Abandono voluntario -> cancelar onDisconnect propios -> transaccion sala -> retirar
membresia -> conservar host valido o escoger el UID conectado lexicograficamente menor
-> actualizar todas las coronas -> confirmar -> volver a Home solo para quien sale.
La seleccion se calcula de nuevo sobre MutableData en cada reintento. No usa azar,
UI, sonidos ni escrituras externas dentro de doTransaction. Se suprimen sus eventos
locales intermedios. Sin conexion no se anuncia una salida como confirmada.

Con dos o mas miembros restantes se mantienen idPartida, ronda, letra, reloj y puntos.
Sin candidatos conectados se conserva la sala y se deja la autoridad vacante hasta
que haya un candidato, en vez de coronar arbitrariamente a un desconectado.
Sin miembros se elimina la sala transaccionalmente; el indice de codigo se retira
por separado solo si todavia apunta a la misma sala. El ultimo paso puede fallar
y dejar un indice huerfano: requiere validacion/limpieza del lado servidor.

Las salidas se registran en Logcat y por la ausencia del nodo de membresia; no se
agrego una bitacora persistente de eventos. Los receptores comparan listas para
notificar una vez por cambio observado: '[Nombre] ha abandonado la partida'. Una
expiracion observada desde estado desconectado utiliza un mensaje de desconexion.
Una pantalla nueva no reproduce un historial de salidas ya ocurridas.

El ciclo de vida no depende de que el host original exista: CREADOR queda como
metadato historico sin privilegios. La lista real de participantes y la fase deciden
si continuar o finalizar. La corona de item_jugador_partida.xml se sigue pintando
con el adaptador existente; ningun XML, icono, sonido ni animacion fue eliminado.
El lobby actualiza su boton en sitio; la partida actualiza permisos y el adaptador
sin reconstruir la ronda. Al tomar autoridad se revisan votos, categoria finalizada,
confirmaciones y un posible inicio de ronda todavia no confirmado.

El avance a siguiente ronda tambien valida host, fase, ronda y participantes listos
en transaccion. Un callback del host anterior o una solicitud repetida no puede
reiniciar una ronda ya avanzada. Los votos/listos/entradas de quien sale se retiran
de las barreras existentes. Su duelo usa el resultado por desconexion ya previsto.
El acumulador ya no crea un nodo jugador parcial si su titular salio durante el calculo.

### Un participante restante y recompensa futura

La misma transaccion de salida cambia a la fase existente resultados_finales,
marca resumenRondaListo y rondasProcesadas, y limita cantidadRondas a la ronda actual.
No reinicia el reloj, no suma respuestas incompletas ni modifica puntos confirmados.
La vista cancela su reloj/revision y abre el resultado final una sola vez. La misma
identidad idPartida y la marca idempotente de Mision 2 protegen su historial normal.
Con un solo jugador no se cumplen los requisitos existentes para dar monedas;
se mantienen las estadisticas/historial normales, sin bono especial.

NO se implementaron los 5 puntos de supervivencia ni el limite diario de 30.
Futura integracion: determinar destino de esos puntos y definicion del dia; calcular
el bono y el limite dentro de la misma transaccion de usuario que comprueba la marca
de partida. No pagar como incremento independiente desde una notificacion de salida.
Un contador diario persistente y/o recibos diferenciados requeriran propuesta de
esquema, compatibilidad y autorizacion antes de implementarlos. No agregados aqui.

### Presencia basica con campos existentes

La gracia es TIEMPO_GRACIA_DESCONEXION_MS=30000 en SalaActivity, configurable en
codigo, sin campo remoto ni selector nuevos. Se escucha .info/connected y
.info/serverTimeOffset. Al conectar se verifica membresia, se registra
onDisconnect con estaConectado=false y desconectadoDesde=ServerValue.TIMESTAMP,
y solo despues de confirmar el registro se anuncia presencia con una transaccion
que no crea un jugador inexistente. Reconectar nunca escribe esHost=true.

onPause ya no equivale a abandonar. Los nodos permanecen durante la gracia. Los
clientes conectados programan un timer para el siguiente vencimiento observado,
sin polling por frame. La transaccion revalida conectado y timestamp antes de
retirar jugadores, por lo que un snapshot de desconexion anterior no expulsa a
quien ya esta conectado. No se expira sin haber obtenido offset del servidor.
Al expirar una membresia eliminada no se restaura automaticamente su corona.

### Limites de esta etapa y diseno pendiente

- Es una base de presencia para una sesion por UID, no un protocolo completo de
  sesiones. Dos dispositivos con el mismo UID o conexiones viejas pueden dejar
  tareas onDisconnect que afecten al nodo compartido. Se cancelan tareas propias
  al salir, pero no se puede cancelar la sesion remota desde otro telefono.
- Hace falta disenar identificadores de sesion y una generacion/lease de autoridad
  para rechazar operaciones antiguas. Esos campos NO se implementaron ni se asumen
  aprobados. Una futura propuesta debe explicar persistencia, limpieza y reglas.
- El plazo empieza cuando Firebase registra la desconexion; no garantiza treinta
  segundos exactos desde que se apaga el WiFi. Un host en segundo plano puede
  conservar conexion aunque Android suspenda el trabajo de UI.
- Si ningun cliente conectado esta ejecutandose, los timers locales no confirman
  expiraciones ni garantizan avance. La continuidad garantizada exige un servicio
  autoritativo con leases/temporizadores del lado servidor y sus reglas validadas.
- Si un host cae despues de adquirir rondasProcesadas pero antes de completar el
  calculo de puntos, la recuperacion completa del cierre sigue pendiente. No se
  libera ese candado a ciegas, pues el acumulador previo puede haber sumado puntos.
  Tampoco se cancelan escrituras ya enviadas antes de revocar autoridad local.
- Clientes antiguos conservan eleccion aleatoria, privilegios CREADOR y eliminacion
  inmediata onDisconnect. Deben actualizarse todos los participantes; no se promete
  seguridad en salas mixtas. Web no se encuentra en este repositorio.
- Las reglas desplegadas no estan disponibles localmente ni fueron consultadas.
  Deben permitir/validar las transacciones necesarias y restringir acciones de otros
  usuarios; validacion del cliente no equivale a seguridad de servidor.
- No se ha probado recepcion de notificaciones, corona, reanudacion de dialogs,
  presencia Android ni reconexion contra servidor/emulador o dispositivos reales.
- La Mision 2 no garantiza recuperacion del pago si se muere el proceso o se resetea
  la sala antes de leer resultados. Sus limites anteriores siguen documentados.

### Pruebas y compilacion

Comando final: gradlew.bat --offline testDebugUnitTest assembleDebug --console=plain
Resultado: BUILD SUCCESSFUL. 44 pruebas, cero fallos/errores:
- TransferenciaHostTest: 14 (host/no-host, salida repetida, retorno del anterior,
  candidatos desconectados/ausentes, solo/vacio, gracia exacta, reconexion, timestamp
  nuevo, barreras, duelo y dos salidas con CAS local y reintento).
- InicioPartidaTest: 8, incluyendo 3 nuevas de avance repetido, host anterior y listos.
- RecompensasPartidaTest: 16 anteriores conservadas.
- PlazoRondaTest: 5 anteriores conservadas.
- ExampleUnitTest: 1 anterior.

Se prueban transformaciones de MutableData real del SDK en memoria. El CAS simula
conflictos y NO prueba concurrencia ni permisos reales de Firebase. La corona y los
callbacks Android requieren validacion instrumentada/manual. No se ejecutaron
pruebas conectadas a produccion. APK generado en app/build/outputs/apk/debug/app-debug.apk.
Graphify se actualizo tras modificar codigo; no cubre XML. Se corrigio un espacio
sobrante en la linea del antiguo onDisconnect y se verifica git diff --check.
Permanecen avisos de APIs obsoletas/Gradle 10. Lint no se repitio: sigue pendiente
por las dos dependencias de analisis ausentes en cache ya documentadas en Mision 2.

### Guion manual pendiente (2/4/8 jugadores actualizados)

1. Tres o mas jugadores, host sale durante escritura: solo el saliente vuelve a Home;
   queda una corona, mensaje de salida, mismo idPartida/ronda/inicioRonda/letra/puntos.
2. Host sale durante revision o puntuaciones: verificar avance del nuevo, sin perder
   controles ni repetir categorias. Distinguir el caso de cierre ya bloqueado pendiente.
3. Dos salidas simultaneas y cambios de conexion: un host canonico, sin filas fantasma.
4. Cortar red menos de 30 s: conserva membresia; reconectar no reinicia ronda.
5. Cortar mas de 30 s con otro cliente activo: validar expiracion y transferencia;
   el anterior no recupera automaticamente la corona al volver.
6. Repetir con relojes locales distintos, app al fondo, suspension y muerte de proceso.
7. Dos jugadores y uno sale: resultado anticipado una vez; sin +5 ni monedas nuevas;
   repetir resultados y confirmar que la marca de Mision 2 no permite duplicaciones.
8. Sin candidatos activos: no borrar sala con membresias; reconectar uno y verificar
   eleccion. Ultima salida: sala vacia e indice limpiado si el servidor lo permite.
9. Probar dos sesiones por UID en entorno aislado como limite conocido, no como caso
   ya resuelto. No cambiar reglas ni reparar produccion para ejecutar este guion.

Archivos: SalaActivity.java; TransferenciaHost.java; Crear_salaFragment.java;
Unirse_salaFragment.java; LobbyFragment.java; PartidaFragment.java;
TarjetasValidacionAdapter.java; InicioPartida.java; TransferenciaHostTest.java;
InicioPartidaTest.java; este documento y artefactos Graphify.
Battle Royale, eleccion inicial aleatoria y pantalla de espectadores no iniciados.

## Mision 4 - Concurrencia con Firebase RTDB Emulator (2026-11-13)

Validacion local finalizada: ocho casos principales superados y un defecto de
presencia reproducido mediante una prueba diagnostica fallida independiente.
Se conserva toda la informacion y los cambios de las misiones anteriores.
No se modificaron clases de la app, datos reales, reglas desplegadas ni interfaces;
no hubo commits ni despliegues.

### Entorno y archivos nuevos

`emulator-tests/` es un proyecto Gradle Java independiente, no agregado al build
Android. Compila directamente los helpers reales RecompensasPartida,
TransferenciaHost, InicioPartida y PlazoRonda con Admin Java SDK 9.12.0 y JUnit.
Firebase CLI oficial 15.36.0 se instalo localmente en su node_modules sin scripts
de instalacion; Database Emulator oficial 4.11.2 se descargo al cache de Firebase.
No se actualizaron dependencias Android. Java 21, Node 26.13.1, npm 11.20.0.

Archivos: README.md, .gitignore, package.json/package-lock.json, firebase.json,
database.test.rules.json, settings.gradle, build.gradle y
src/test/java/com/example/basta_game/emulator/ConcurrenciaEmulatorTest.java.
Este documento es el unico archivo previo modificado en esta mision.

Proyecto fijo demo-basta-concurrency, emulador solo 127.0.0.1:9000. Los tests
rechazan otra variable FIREBASE_DATABASE_EMULATOR_HOST y otro GCLOUD_PROJECT;
no usan credenciales reales ni ADC. Cada caso tiene rutas runs/{UUID}; la conexion
fixture administrativa prepara/limpia exclusivamente el emulador. Los dos clientes
que compiten son conexiones independientes sin privilegios administrativos.
Reglas locales creadas expresamente para los fixtures, con un camino de escritura
denegada. NO representan las reglas desplegadas: compatibilidad pendiente.

### Pruebas ejecutadas y evidencia

Preparacion: prepareEmulatorTests compileTestJava, exitosa. El primer intento de
ejecucion offline no llego a ejecutar tests por dependencias runtime no cacheadas;
se descargaron en la preparacion y se excluyeron los modulos Admin ajenos a RTDB
(Firestore/Storage). No se desactivo ninguna proteccion.

Comando final desde emulator-tests: npm.cmd test (Gradle offline).
Informe XML final: 8 tests, 0 fallos, 0 errores, 0 omitidos.
Informe HTML: emulator-tests/build/reports/tests/test/index.html.

1. Dos transacciones reales concurrentes de recompensa: solo una confirma;
   evento repetido/reentrada no paga; historial, contadores y marca una sola vez;
   avatar, inventario y campos antiguos conservados.
2. Eleccion concurrente desde dos clientes: un host canonico y una sola corona.
3. Abandonos concurrentes del host y otro jugador: candidato consistente; salida
   repetida aborta; ronda, inicio, idPartida, puntos y otros datos conservados.
4. Escritura concurrente de avatar forzada entre lectura y envio: conflicto real
   del servidor, al menos dos ejecuciones no vacias del handler; avatar preservado.
5. Cliente offline encola una recompensa; otro paga; al reconectar, el primero
   aborta sin duplicacion. No equivale a matar un proceso o perder un ACK de commit.
6. Regla local deniega transaccion: PERMISSION_DENIED sin historial, marca ni pago.
7. onDisconnect real escribe ausencia y timestamp; el helper expira y transfiere
   con entrada temporal +30000 ms. No se esperaron 30 s de reloj real ni se probo
   el temporizador de SalaActivity; la corona no vuelve al anterior.
8. Dos clientes leen igual ServerValue.TIMESTAMP y calculan el mismo plazo.

Se validan transacciones RTDB reales del SDK Java y helpers compartidos, no solo
CAS simulado. No se ejecuta Android SDK, actividades ni callbacks de sus pantallas.
No se repitieron las 44 pruebas Android ni assembleDebug: no cambio codigo Android;
el APK exitoso de Mision 3 sigue siendo la ultima compilacion Android verificada.
Graphify consultado puntualmente, sin reconstruccion innecesaria del grafo.

### Defecto confirmado y pendientes

Comando npm.cmd run test:presence-limit: 1 test, 1 fallo, exit 1.
Informe: emulator-tests/build/reports/tests/presenceLimit/index.html.
Prueba conexionAnteriorNoDebeDesconectarOtraSesionActiva separada explicitamente
de los ocho casos principales; NO se presenta como requisito superado.

Dos conexiones sobre el mismo nodo de jugador: una antigua registra onDisconnect;
otra publica estaConectado=true; al cerrar la antigua, Firebase cambia el campo a
false mientras /.info/connected de la segunda sigue true. Confirma el riesgo de
sesiones advertido en Mision 3: podria expulsar a un jugador activo y transferir
su corona tras el periodo de gracia. No se autenticaron dos sesiones de Auth real;
se reproduce la colision del nodo de presencia con conexiones RTDB independientes.

La correccion requiere distinguir sesiones y evitar callbacks obsoletos con el
correspondiente esquema/reglas. No se implementa sin una decision de esquema;
queda como siguiente correccion propuesta, sin afectar las garantias atomicas de
eleccion y recompensa verificadas aqui. No se promete presencia segura multi-sesion.

Siguen pendientes reglas reales, pruebas Android instrumentadas, red inestable,
reconexion tras muerte del proceso, perdida de ACK, recuperacion de cierre tras
rondasProcesadas y continuidad sin clientes ejecutandose. Los clientes antiguos
pueden seguir escribiendo con el protocolo previo: salas mixtas no garantizadas.
No se implementaron recompensas de supervivencia, nuevas mecanicas ni espectador.

## Mision 5 - Correcciones tras pruebas reales (2026-11-14)

Estado: correcciones locales de registro, espera, identidad y cierre; presencia
multi-sesion pendiente de autorizacion de esquema; crash de tablet pendiente de
Logcat. No se afirma que se reprodujo en este entorno la sesion real de los dos
dispositivos. Los defectos siguientes se comprobaron en operaciones y listeners
del codigo; los helpers se verificaron localmente y contra RTDB Emulator.
Se conserva el trabajo previo. Sin datos/reglas de produccion, commits o despliegues.

### Registro institucional

RegistrarActivity aceptaba cualquier dominio antes de crear el usuario. Se agrega
CorreoInstitucional con dominio exactamente @ues.edu.sv, sin distinguir mayusculas,
rechazo de sufijos/subdominios, multiples arrobas, espacios y vacios. Se ejecuta
antes de createUserWithEmailAndPassword y muestra la notificacion personalizada
solicitada con su sonido existente. Login no cambia. Tres tests cubren aceptacion,
dominios enganosos y formato invalido, incluidos saltos de linea.
Es una restriccion provisional del cliente: no prueba que el correo exista, que
pertenezca a UES ni impide un registro desde otro cliente. No se modifica Auth.

### Espera de puntuaciones y siguiente partida

Defectos comprobados: escucharJugadoresListosRevision observaba solo revisionLista
y consultaba jugadores por separado, sin reaccionar necesariamente a una salida;
quitaba el listener ANTES de confirmar el avance. InicioPartida.avanzar no comparaba
idPartida, permitiendo una peticion tardia de otra partida con igual numero de ronda.
El boton deshabilitado tampoco ofrecia reintento si fallaba la escritura.

Ahora se observa salaRef con una sola instantanea de ronda, idPartida, miembros,
listos y host canonico; se conserva el listener hasta cambiar de ronda/destruir la
vista. El avance compara el id capturado y el resto de guardas en una transaccion.
La preparacion tambien valida pertenencia/fase/id/ronda en transaccion, sin crear
filas de participantes salientes. Los errores permiten reintentar y resuscribirse.
No se cuentan marcas de UID que ya no pertenece a la sala. Durante una desconexion
temporal, el miembro sigue contando hasta la expiracion definitiva.

LobbyFragment ademas reescribia un JugadorSala completo deserializado para cambiar
estaListo. Esa copia podia sobrescribir identidad, corona, presencia y campos
desconocidos actualizados entre lectura/escritura. Ahora la transaccion modifica
solo estaListo sobre los datos actuales y no recrea un jugador ausente.

No se atribuye todo el bloqueo observado a una causa unica sin trazas de esa partida.
Los tests de emulador ejecutan el algoritmo/transacciones, no los botones/listeners
Android; repetir segunda partida en dispositivos sigue siendo obligatorio.

### Identidad, recreacion y gracia

Unirse_salaFragment usaba user sin comprobar perfil/nombre y setValue(jugador)
reiniciaba el nodo incluso en reentrada. IdentidadJugador valida el nombre (rechaza
null, vacio y literal null), inicializa solo al nuevo y actualiza identidad sin
borrar puntos, corona, presencia, campos extra ni foto anterior cuando falta URL.
Usa el UID autenticado para la ruta, no un uid posiblemente ausente del perfil.
Una membresia existente puede reentrar aunque la sala ya cuente ese cupo ocupado.
Los perfiles incompletos no entran; aparece aviso de carga/reintento. Crear sala
tambien rechaza nombre incompleto; la notificacion de bienvenida tolera nombre null.

SalaActivity conserva el fragment restaurado por Android en lugar de sustituirlo
siempre por lobby; recupera identidad incompleta desde usuarios sin recrear una
membresia que haya expirado mientras llega el perfil. HomeActivity reutiliza sus
fragments restaurados para evitar navegar hacia instancias nuevas no agregadas.
Adapters muestran Cargando perfil en vez de null y el lobby indica Reconectando,
conservando la posicion. Hay aviso al cambiar a desconectado.

No hay abandono de SalaActivity en onPause/onStop; Home no se convierte en abandono.
La gracia cambia de 30000 a 90000 ms: desde ServerValue.TIMESTAMP al detectar RTDB
la desconexion, comparado con hora local + /.info/serverTimeOffset. La limpieza
exige conexion y offset disponible. No son 90 s desde presionar Home y no garantiza
exactitud durante suspension de Android ni frente a todo cambio de reloj local.
Test adicional comprueba 89999/90000 ms. No se presenta como solucion multi-sesion.

El defecto onDisconnect antiguo sobre UID compartido sigue confirmado y pendiente.
Se solicito autorizacion para sesiones/{idSesion} con conectado/desconectadoDesde;
no se agregan campos sin respuesta. Diseno, compatibilidad y efectos en
docs/MISION_5_VALIDACION.md. No garantizar salas con clientes antiguos mezclados.

### Ultimo jugador y resumen tardio

El cierre SI estaba implementado en TransferenciaHost.reconciliar desde Mision 3
(no era solo documentacion). Se descubrio que marcarResumenRondaListo publicaba
fase con updateChildren sin validar membresia/id/ronda en servidor: un callback
tardio podia reabrir revision despues del cierre por abandono. Ahora usa
InicioPartida.publicarResumen en transaccion y rechaza finales, un solo miembro,
otro id, otra ronda y host revocado. SalaActivity tambien reconcilia una lista
unitaria aunque la corona ya sea consistente, cubriendo remociones externas al
helper. Mantiene el cierre anticipado existente, puntos confirmados y sin bonos.

No se agrega premio de supervivencia. Faltan decision de contador de puntos, prueba
autoritativa de ronda valida, recibo independiente y acumulado diario. La propuesta
de campos y seguridad que requiere autorizacion queda en MISION_5_VALIDACION.md;
no reutilizar a ciegas la marca normal, ni confiar en el dia del telefono.

### Tablet API 25 y validacion pendiente

minSdk 24 permite instalacion en API 25. Se revisaron inicio, Home, perfil y sus
cargas; no se obtuvo un stack trace que demuestre la causa del crash en SM-T350.
La defensa de nombre null corrige un posible NPE comprobable en la bienvenida,
pero NO se identifica como causa del cierre de esa tablet. No se cambiaron
dependencias Android ni se deshabilitaron funciones/seguridad para adivinarla.
Se solicitaron FATAL EXCEPTION, Process y todos los Caused by; comandos y guion
manual de 2/3/8 jugadores en MISION_5_VALIDACION.md. Reglas reales siguen ausentes.

### Archivos de la mision

RegistrarActivity.java; CorreoInstitucional.java y Test; InicioPartida.java y Test;
PartidaFragment.java; LobbyFragment.java; IdentidadJugador.java y Test;
Unirse_salaFragment.java; Crear_salaFragment.java; SalaActivity.java;
HomeActivity.java; VerificacionesAuth.java; JugadorSalaAdapter.java;
JugadorPartidaAdapter.java; TransferenciaHostTest.java; emulator-tests/build.gradle,
ConcurrenciaEmulatorTest.java y README; este documento y MISION_5_VALIDACION.md.
Grafo AST actualizado tras cambios (sin LLM); excluye XML y no analiza completamente
Gradle. No se instalaron extractores opcionales sugeridos por Graphify.

### Resultado final de las verificaciones

Comando final: gradlew.bat --offline testDebugUnitTest assembleDebug lintDebug
--console=plain. BUILD SUCCESSFUL (11 s en la ultima ejecucion).
55 tests unitarios: cero fallos/errores. Desglose: CorreoInstitucional 3,
IdentidadJugador 3, InicioPartida 12, TransferenciaHost 15, PlazoRonda 5,
RecompensasPartida 16, ExampleUnitTest 1. Las pruebas anteriores se conservan.
Regresion adicional: se detecto que equalsIgnoreCase admitia el dominio visualmente
parecido ue\u017f.edu.sv; el caso fallo antes de corregirlo y paso con normalizacion
Locale.ROOT que conserva la exigencia del dominio ASCII exacto.

APK: app/build/outputs/apk/debug/app-debug.apk (120378364 bytes).
Lint: 0 errores, 692 advertencias, 0 NewApi. No se arreglan avisos generales ajenos
a la mision ni se actualizan librerias por sugerencias de version. El primer lint
offline no pudo resolver datastore-preferences-jvm:1.1.7; la ejecucion con red
descargo dependencias de analisis ya declaradas, sin cambiar versiones. Despues,
la verificacion final completa funciono offline.

Emulador: npm.cmd test desde emulator-tests, demo-basta-concurrency, loopback.
11 tests reales, cero fallos/errores; incluye los ocho anteriores y tres nuevos:
segunda partida con dos/tres miembros y avance concurrente/repetido, abandono en
espera seguido de cierre sin reapertura tardia, identidad con escritura concurrente.
La prueba diagnostica presenceLimit sigue excluida explicitamente de esa suite,
con su fallo confirmado en Mision 4. No se repitio como si se hubiera corregido.

Informes: app/build/reports/tests/testDebugUnitTest/index.html,
app/build/reports/lint-results-debug.html y
emulator-tests/build/reports/tests/test/index.html. Son pruebas de helpers y SDK
Java, no instrumentacion de pantallas ni seguridad de reglas desplegadas.
La regresion manual sobre dos partidas consecutivas, Home y retorno, tablet y
2/3/8 jugadores queda pendiente segun MISION_5_VALIDACION.md.

## Mision 6 - UX, revision, teclado y comodines (2026-11-14)

El usuario informa pruebas reales satisfactorias con Marlon, Diego y Tote para
salas, transferencia de host, regreso del fondo, partidas consecutivas, BASTA
simultaneo, recompensas y cierre del ultimo jugador. Desconexion temporal pendiente.
Se conserva esa base. Estado de esta mision: primera etapa local implementada;
temporizador compartido y cinco oportunidades por categoria pendientes de
autorizacion de esquema/decision de consumo, solicitada sin respuesta recibida.
Detalle verificable y guion manual en docs/MISION_6_VALIDACION.md.

### Cambios y causas comprobadas

1. item_tarjeta_revision contenia HorizontalScrollView y RecyclerView wrap_content
   con minWidth=330dp, ademas de los paddings del carrusel. Se quita la necesidad de
   ancho lateral: tarjeta de respuesta con avatar/nombre a izquierda y palabra con
   votar/puntos debajo a derecha. Lista de ancho disponible y scroll vertical;
   indicador discontinuo verde/amarillo/rojo segun +10/+5/+0 y estilo existente.
   EstadoRevision distingue vacias, repetidas validadas y resultado definitivo.
   Una vacia recibe +0, una correcta repetida +5 sin impugnacion; los resultados
   finales sustituyen inmediatamente el boton. No se convierte empate de votos en +5.
2. CategoriaPartidaAdapter y PartidaFragment competian con scrolls retardados y
   margenes fijos 120/135dp, mientras SalaActivity no aplicaba IME a edge-to-edge.
   Se usa requestRectangleOnScreen con margen 12dp y padding IME/systemBars,
   conservando adjustResize y el ScrollView. Se evita setText redundante y trim
   durante escritura; se guarda borrador por partida/ronda para recreacion.
3. Menu filtraba y ocultaba el otro grupo, y su lista wrap_content sin scroll podia
   crecer fuera del dialogo. Ahora muestra ambos grupos propios y disponibilidad,
   lista vertical acotada y botones adaptables. FaseComodin centraliza fases;
   BaseComodin tambien comprueba sala/membresia/ronda/fase antes del descuento.
   Resultados no habilitados tras ultima ronda. Rarezas, costos/efectos no cambian.
4. Congelador SI escribia comodinesUsados, pero el resumen leia solo comodinesResultados
   y manejaba solo Flujo/Doble. ResumenComodines combina ambos, incluye rondas previas
   en final, muestra datos registrados y distingue efectos temporales neutrales.
   No inventa puntos, duraciones de otros usos ni eventos historicos. Marca ahora
   registra sus nuevas activaciones usando el formato ya existente de comodinesUsados,
   sin campos de esquema nuevos. Su resultado historico no se supone disponible.
5. setearCronometroGlobal forzaba fase local jugando aun si Firebase decia resultados.
   FaseJuego lo impide y recupera ranking/resumen; se conserva el cierre especial
   de un participante y la proteccion idempotente de recompensas. Se protege revision
   frente a descarte con Atras y se libera el resumen al destruir su vista. No se
   agrega premio de supervivencia ni pila de navegacion hacia partida cerrada.
6. registrarVoto hacia setValue sin identificar el dialogo, y ambos cierres de
   votacion hacian updateChildren desde snapshots anteriores. VotacionRevision
   ahora valida partida/ronda/idVotacion/membresia, acepta un voto confirmado por UID,
   no reabre palabras resueltas/vacias/+5 y confirma resultado/historial/cierre
   atomicamente por el host vigente, solo si los votos no cambiaron. Se usan campos
   existentes; no se impone aun un deadline de turno que no existe en el esquema.
7. Corona y notificaciones existentes conservadas, sin animacion ni mensaje de
   host nuevo. No se modifica presencia, reglas desplegadas o datos de produccion.

### Pendientes explicitos

El contador actual de cinco segundos sigue siendo local por respuesta. El dialogo
si/no/abstenerse aun carece de timeout compartido. NO se presenta como sincronizado.
Faltan confirmaciones de preparacion, espera maxima, plazo unico, cifras grandes y
transicion entre turnos con hasta cinco oportunidades por categoria. No se crean
esos campos sin autorizacion. Propuesta concreta en MISION_6_VALIDACION.md.

La primera lectura comunicada del empate era incompleta: el cierre SI guarda
resuelta=true, conserva puntos previos y no consume el cupo antiguo. No permite
volver a votar la palabra; se corrigio expresamente esa explicacion. El limite
antiguo de dos decisiones por ronda esta inactivo con USAR_API_VALIDACION=false.
La decision pendiente es como cuentan empates/abstenciones para el nuevo cupo de
cinco por categoria; nunca se interpreto como cinco votos por usuario.

Las guardas de fase son cliente/lectura previa, no atomicidad inventario+efecto ni
seguridad de servidor. Subsisten carreras si cambia fase tras la lectura; el
descuento heredado tampoco garantiza exclusion entre sesiones del mismo UID.
Reglas reales ausentes: comprobar permisos de transaccion en sala y actualizar
todos los clientes antes de considerar compatibles las nuevas operaciones.

Teclado, medidas renderizadas, gestos, recreacion y UX multijugador requieren
validacion real. Los tres casos InterfazPartidaTest estan compilados, NO ejecutados.
No se instalaron APK en dispositivos del usuario ni se dirigio Android a produccion
para probarlos. Persisten los pendientes de desconexion/sesiones y tablet previos.

### Archivos de esta etapa

PartidaFragment.java; SalaActivity.java; CategoriaPartidaAdapter.java;
ValidacionAdapter.java; ComodinPartidaAdapter.java; ResumenComodinesAdapter.java;
BaseComodin.java; ComodinMarcaDeLaMuerte.java; EventoComodin.java;
EstadoRevision.java; FaseComodin.java; FaseJuego.java; ResumenComodines.java;
VotacionRevision.java y sus cinco clases Test; LayoutsPartidaTest.java;
InterfazPartidaTest.java; item_respuestas.xml; item_tarjeta_revision.xml;
dialogo_respuestas.xml; item_comodin_partida.xml; dialogo_comodines_partida.xml;
dialogo_resumen_comodines.xml; emulator-tests/build.gradle,
ConcurrenciaEmulatorTest.java y README; este documento y MISION_6_VALIDACION.md.

### Verificacion final de la etapa

gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug --console=plain: BUILD SUCCESSFUL (13 s en la ultima verificacion).
74 tests unitarios, cero fallos/errores: los 55 de Mision 5 y 19 nuevos:
EstadoRevision 3, FaseComodin 3, FaseJuego 2, ResumenComodines 3,
LayoutsPartida 3 y VotacionRevision 5. Son regresiones de logica/contratos XML,
no demuestran comportamiento fisico del teclado, gestos ni medidas renderizadas.

npm.cmd test en emulator-tests: 12 tests, cero fallos/errores, RTDB Emulator
demo-basta-concurrency / loopback. Incluye nueva prueba real de aperturas
concurrentes, voto duplicado y dos cierres concurrentes: exactamente uno confirma
en cada etapa; historial unico, palabra no reabierta y puntuacion previa conservada.
No prueba los cinco segundos compartidos, cupo por categoria o UI Android.
El caso conocido de presencia multi-sesion sigue fallido en su tarea diagnostica
separada de Mision 4; no se presenta como corregido ni se repite sin cambiarlo.

APK debug generado en app/build/outputs/apk/debug/app-debug.apk. APK instrumentado
generado en app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk;
sus tres pruebas nuevas de vistas no se ejecutaron. No se instalaron herramientas
ni dependencias nuevas. Lint: 0 errores, 692 avisos; no se actualizan librerias por
sugerencias de version. Graphify actualizado por AST tras modificar codigo, sin
LLM, sin instalar extractores opcionales; XML no esta cubierto por el grafo.
git diff --check revisado y sin errores en los archivos modificados de esta etapa.
Sin commits, despliegues, cambios de reglas ni operaciones sobre Firebase real.

## Preparacion de rama para tablet Diana (2026-11-14)

Publicacion autorizada en pruebas-tablet-diana-2026-11-14. Se elimina unicamente la
prueba antigua de FirebaseAuth en MainActivity (campos literales y creacion de
cuenta), junto a sus imports/campo innecesarios. Se conserva la actividad y su
interfaz; no se modifica RegistrarActivity, LoginActivity ni el manifiesto.
Launcher comprobado: SplashActivity -> LoginActivity, no MainActivity.

Se comprueba que la antigua prueba existe en la ascendencia local. Para no enviarla
en la rama de pruebas, se prepara un commit raiz independiente con un indice
temporal: los commits/ramas existentes, el indice original y los archivos de
trabajo previos se conservan. No se reescribe historial ni se hace force push.
Esta estrategia no elimina las copias que ya existan en otras ramas del remoto.

Se excluyen IDE, caches/Graphify, compilaciones, herramientas instaladas y
app/google-services.json. Este ultimo permanece local para compilar; Diana necesita
una copia autorizada por canal privado y su propia ruta SDK. Instrucciones de
compilacion y Logcat en docs/PRUEBAS_TABLET_DIANA.md. No hay despliegues ni cambios
en Firebase. Los pendientes funcionales de las misiones anteriores se mantienen.

Verificacion de preparacion: gradlew.bat --offline assembleDebug --console=plain,
BUILD SUCCESSFUL (4 s). Auditoria del contenido candidato sin nuevas credenciales
detectadas; los valores de la antigua prueba no aparecen en ningun archivo candidato.
Se revisaron 181 commits locales y el bloque antiguo aparece en 172, incluido el
commit base: un commit normal con ese padre no bastaria para publicar sin esa
ascendencia. Se conserva el indice original y se usa un indice temporal para la
instantanea limpia; no se descartan los archivos preparados o sin seguimiento.


## Mision 7 ? UX/UI e interaccion (2026-11-14)

Implementacion local terminada; no commits, pushes, instalaciones, cambios en
Firebase real ni reglas desplegadas. Se conservan los pendientes anteriores,
especialmente aislamiento de sesiones/presencia y recuperacion del candado de ronda.

### Comodines, compras y avisos

- Menu de comodines adaptado a la referencia: banners azules y dorados, tarjetas
  pastel con contorno azul, rareza, cantidad, disponibilidad y boton diferenciado.
  Conserva iconos, costos, reglas y dos grupos. No se cambian los efectos.
  FaseComodin sigue siendo la comprobacion compartida de fase; resultados solo
  si queda otra ronda. El resumen existente conserva Congelador y su duracion
  registrada, sin inventar efectos historicos. Su RecyclerView vertical se conserva.
- ComprasViewModel retiene una sola operacion pendiente por Activity, incluso al
  recrearse o cambiar de fragment; botones bloqueados inmediatamente antes de la
  animacion. Tras confirmar, publica el snapshot de inventario y saldo. No limita
  compras sucesivas. CompraComodin aplica moneda/inventario en una transaccion
  de usuario, preserva campos desconocidos y recibos de recompensas; admite monedas
  y gemas, valida saldos/cantidades y evita overflow. No agrega esquema de compras.
  Una compra offline queda pendiente hasta recibir resultado de Firebase; no se
  reenvia automaticamente como otra compra ante un resultado incierto.
- NotificationHelper aloja un unico aviso en la Activity visible y mantiene su
  cola/expiracion en la aplicacion. Cambiar de fragment o Activity no reinicia el
  plazo ni genera otro aviso. Compras cercanas (4 s entre ellas) actualizan un
  resumen por UID; sonido solo al iniciar grupo. Otros eventos conservan estilo
  y cola. No persiste tras muerte del proceso; una ventana Dialog independiente
  puede cubrir el overlay de la Activity. Falta comprobar estas transiciones reales.

### Votaciones: regla y reloj compartido

Empate real con votos decisivos iguales y mayores que cero => +5. Abstenciones
no superan mayorias. Con 0 votos decisivos se conserva la validacion previa: no
se inventa un empate +5. Vacias +0 y repetidas +5 siguen automaticas. Una respuesta
resuelta no vuelve a ser votable. Las validaciones iniciales ahora solo crean
categorias ausentes mediante transaccion: una carga tardia no pisa un resultado.

Esquema aditivo autorizado, solo implementado localmente:
- salas/{idSala}/revisionCategorias/ronda_N/indiceActual (campo ya existente).
- .../reloj/{idPartida,indice,categoria,turno,modo,idVotacion,preparandoDesde,inicio}.
  modo = propuestas o votos; idVotacion solo existe durante votos. Preparacion e
  inicio usan ServerValue.TIMESTAMP; plazo logico persistido = inicio + 5000 ms.
- .../reloj/listos/{uid}=true; .../cerradas/{indice}=true; .../completa=true.
- salas/{idSala}/votacionActiva/ronda_N/turnoRevision enlaza solicitud y turno.

Se muestran todas las palabras de la categoria sin temporizadores por fila.
El turno de propuestas espera validaciones de clientes activos; el de votos
espera su dialogo visible. Espera maxima 8 segundos, sin expulsar jugadores.
Cada ventana de propuestas y cada votacion decisiva tiene un plazo compartido
independiente de 5 segundos; tras resolver una palabra se abre otra ventana de
propuestas para palabras pendientes de esa categoria. No se altera la cuota
existente por modo: cinco segundos NO significa cinco votos por usuario ni se
introduce silenciosamente una cuota nueva de cinco impugnaciones.

Transacciones en la sala comprueban partida, ronda, indice/turno, pertenencia,
id de votacion, plazo y votos observados; votos por UID solo una vez. Cierres y
transiciones repetidos abortan. Cualquier miembro activo actualizado puede
completar la transicion determinista: no depende de que el host siga en primer
plano y no cambia la autoridad administrativa/corona. No hay escrituras desde
el tick de dibujo salvo al cruzar preparacion/inicio/cierre. El reloj Android
estima tiempo servidor con /.info/serverTimeOffset y anclaje monotono; al perder
conexion bloquea votos. La latencia puede retrasar el dibujo entre dispositivos.

Al cerrar una categoria, validaciones que nunca llegaron se completan desde las
respuestas canonicas usando la MISMA validacion local actual (minimo 3 letras,
inicial normalizada, repetidas por trim/ignoreCase). No pisa validaciones ya
existentes, incluidas las decididas por votacion. La API esta desactivada en el
modo actual; si se reactiva hay que revisar este fallback antes de desplegarlo.

Compatibilidad y seguridad: se mantienen nodos antiguos y overloads para salas
sin reloj, pero clientes antiguos pueden ignorar los plazos y borrar campos con
sus escrituras previas. Actualizar todos los participantes antes de probar este
protocolo; no se garantiza una sala mixta. No disponemos de reglas desplegadas:
no se verifican sus permisos de lectura/transaccion en sala/usuario, ni proteccion
servidor frente a un cliente adulterado o reloj manipulado. Los checks Java son
coordinacion de clientes, no reglas de seguridad. No hay progreso garantizado si
todos suspenden la app, ni recuperacion nueva del candado rondaProcesada. Web no
incluida/verificada. Estas limitaciones requieren validacion posterior, sin tocar
produccion en esta mision.

### Scroll y teclado

El ScrollView exterior conserva el desplazamiento vertical de toda la partida;
RecyclerView de categorias deja de consumir gestos verticales y mantiene foco.
Se conservan los borradores/cursor existentes y el ajuste IME/adjustResize. El
reposicionamiento por insets se realiza solo cuando cambia la altura del teclado,
para evitar que cada dispatch impida desplazarse manualmente mientras se escribe.
No se cambian temporizadores ni cierre de ronda desde ese ajuste.

### Archivos de esta etapa

- PartidaFragment, TiendaFragment, SalaActivity y BastaAplication.
- Adapters: CategoriaPartidaAdapter, CategoriaTiendaAdapter, ComodinTiendaAdapter,
  ComodinPartidaAdapter, TarjetasValidacionAdapter y ValidacionAdapter.
- Utils: NotificationHelper, VotacionRevision; nuevos ColaAvisos, ResumenCompras,
  CompraComodin, ComprasViewModel, ResultadoVotos, RelojRevision,
  CoordinadorRevision y ValidacionBasica.
- Layouts dialogo_comodines_partida, item_comodin_partida, dialogo_respuestas,
  dialogo_votacion_palabra; drawables bg_comodin_{banner,accion,cerrar,badge_oro}.
- Tests CompraComodinTest, ColaAvisosTest, ResultadoVotosTest, RelojRevisionTest,
  InterfazPartidaTest; emulator-tests/build.gradle y ConcurrenciaEmulatorTest.
  Documentacion y actualizacion AST de Graphify.

### Verificacion y pendientes de dispositivo

Gradle Wrapper offline: testDebugUnitTest (89 pruebas, 0 fallos/errores/omitidas),
assembleDebug, assembleDebugAndroidTest y lintDebug: BUILD SUCCESSFUL.
Lint: 0 errores y 684 avisos; no se actualizan librerias por estas sugerencias.
Cinco pruebas de vistas compiladas, NO ejecutadas: dimensiones de tarjeta de voto,
puntos definitivos, texto/cursor, bloqueo de compra/rebind e interaccion de scroll.
Estas pruebas NO demuestran teclado fisico, gestos ni navegacion real.

Firebase RTDB Emulator: 14 pruebas PASSED en demo-basta-concurrency/127.0.0.1:9000,
incluyendo dos clientes comprando con ambas monedas, inventario/saldo preservados,
un mismo inicio numerico para ambos, espera real de 5 segundos, rechazo de votos
tardios/previos y cierre concurrente unico con empate. Se compilan los helpers
reales, no traducciones; SDK Admin con clientes anonimos y reglas fixture locales.
La limitacion conocida de presencia multisession sigue excluida del conjunto de
exito y documentada, sin presentarla como corregida. No prueba Android/produccion.

Pendientes manuales: tres jugadores, entrada tardia, host en segundo plano,
reconexion durante voto/compra, menu en telefono pequeno/tablet, scroll de ultimas
categorias con IME abierto, avisos al navegar/recrear y resumen con muchos usos.
El cronometro esta implementado en Android y verificado en helpers/emulador; queda
pendiente la validacion integral de dispositivos y permisos reales antes de usarlo
en una demo. No se analiza el crash de Diana sin Logcat ni se inicia otra mision.

Graphify actualizado por AST al finalizar: 1810 nodos/5040 relaciones. Omite XML
y advierte extraccion parcial del DSL Gradle del emulador; ese Gradle SI compilo
y ejecuto su suite. No se instalaron extractores opcionales ni se uso LLM.

## Mision 8 — Recuperacion y verificacion local (2026-11-14)

Se conservan las modificaciones anteriores y se recupera el trabajo interrumpido
por falta de memoria virtual. Esta etapa actualiza pruebas y documentacion; no
hace commits/pushes, despliegues, cambios en Firebase real ni reglas desplegadas.
Por instruccion del usuario NO se ejecutan Python, Graphify, procesamiento de
imagenes, emuladores ni pruebas instrumentadas. El grafo queda pendiente de
actualizacion. Las pruebas y compilacion se ejecutaran en secuencia, sin paralelo,
con un trabajador y memoria acotada; cualquier fallo de memoria detiene la etapa.

### Cambios guardados y revisados

- Scroll: fragment_partida ya no tiene ScrollView exterior. La hoja tiene altura
  delimitada por peso y RecyclerView interno desplazable, sin scrollbar visible.
  BASTA/Salir quedan fuera de la lista. CategoriaPartidaAdapter usa
  configurarScrollInterno, conserva foco y borradores; SalaActivity oculta el
  encabezado secundario al aparecer IME y pide visibilidad del campo activo.
  No se demuestra comportamiento tactil/teclado en dispositivo mediante esta revision.
- Lobby: InicioPartida exige al menos dos jugadores, todos conectados y listos;
  verifica tambien las sesiones cuando existen. Expulsion transaccional solo por
  host, sin autoexpulsion y solo en espera, con confirmacion; no elimina cuentas.
  La accion se ofrece manteniendo pulsada la tarjeta. Limpiar preparacion por
  desconexion y volver a confirmar al reconectar esta implementado localmente.
- Reloj: InicioPartida fija inicioRonda con ServerValue.TIMESTAMP dentro de la
  misma transaccion de inicio/avance. Confirmar una entrada tardia conserva el
  inicio ya publicado. Se elimina la barrera que esperaba animaciones locales.
  LoadingPartida conserva animacion/sonido, reducida a un segundo con ID_PARTIDA.
- Compras: ControlCompra conserva una operacion pendiente y exige dos segundos
  tras confirmacion; ComprasViewModel conserva las transacciones de saldo e
  inventario. ResumenCompras agrupa cantidades reales por nombre en lineas.
  NotificationHelper conserva una cola y permite alojar el aviso en el dialogo
  visible registrado (PartidaFragment, CustomDialogHelper y detalle de tienda).
- Votaciones: contador con numero grande y etiqueta para jugadores. Una categoria
  con respuestas explicitamente vacias de todos se muestra un segundo y avanza
  mediante la transaccion existente, con +0 y sin votacion. Respuestas pendientes
  o no vacias conservan el plazo normal; no se inventa una cuota de cinco votos.
- Comodines: titulo, introduccion, estado vacio y badge compacto compartiendo
  Widget.Basta.Inventario con tienda. Ver estadisticas tiene minimo 48dp y texto
  de 14sp. Resumen usa variaciones deterministas de frases con resultados/deltas
  reales; no inventa duraciones ni resultados ausentes. Se mantienen fases y rarezas.
- Abandono: callbacks de recompensa comprueban salida/navegacion antes de procesar;
  un cierre con un participante explica que conserva sus puntos confirmados.
  Falta reproduccion fisica del aviso original. No se ocultan otros errores reales.

### Esquema de sesiones autorizado durante M8

Nuevo nodo aditivo: salas/{idSala}/sesiones/{uid}/{idSesion}/
{conectada,desconectadoDesde}. Cada conexion registra onDisconnect solo en su
propia sesion, con marca de tiempo servidor; no escribe ausencia global desde
un callback de sesion antigua. PresenciaSala deriva estaConectado y
 desconectadoDesde existentes del conjunto y limpia estaListo al quedar sin
conexiones. Expiracion vuelve a comprobar sesiones antes de retirar al jugador.
La presencia conserva la corona actual; no restaura una corona antigua.

Permisos reales NO comprobados: validar lectura de sesiones y transacciones de
sala; exigir pertenencia/propiedad de UID y sesion en reglas antes de desplegar.
No se ha actualizado ningun servidor. Clientes antiguos mantienen hooks sobre el
nodo global y no garantizan aislamiento al mezclarse con este protocolo. Hace
falta probar el ciclo Android de reconexion/recreacion y callbacks con dos
conexiones reales; pruebas puras no demuestran concurrencia de Firebase.

### Recursos conservados, sin reprocesamiento durante recuperacion

25 PNG optimizados en drawable-nodpi: iconos de 256x256 (aprox. 0.25 MiB RGBA
frente a 6 MiB por original de 1254x1254, antes de escalado); fondo de tienda
541x960 (aprox. 1.98 MiB), banners de 768px. nodpi evita multiplicacion por
densidad. Originales preservados fuera del APK en
 docs/referencias-ui/originales-recursos/. No se usa largeHeap.

Launcher oficial: legacy 48/72/96/144/192px y foreground adaptativo
108/162/216/324/432px, con margen de seguridad. Manifiesto mantiene icon y
roundIcon hacia mipmap; XML adaptativos y fondos/foreground por defecto se
sustituyeron, sin cambiar package ni launcher Activity. No usa la fuente de
1254px como icono runtime. Falta comprobar mascaras circular/cuadrada en launcher.

La optimizacion reduce dimensiones decodificadas de los recursos identificados;
NO demuestra resolver el OOM de la SM-T350. Medir heap y abrir repetidamente tienda
en API25 fisico. No se repite el script de imagenes. Otros recursos no optimizados
(como ic_comodines si sigue en drawable) requieren evaluacion posterior, sin
procesarlos en esta recuperacion.

### Pruebas actualizadas y limites

InterfazPartidaTest sustituye la expectativa de scroll exterior por la jerarquia
real de fragment_partida, lista interna y BASTA fijo en anchos de 320/800dp. Solo
se actualiza el fuente: NO se compila ni ejecuta APK de instrumentacion aqui.
CompraComodinTest espera el resumen multilinea agrupado. InicioPartidaTest usa
fixtures con dos conectados/preparados para iniciar; las revisiones mantienen
sus fixtures originales para conservar pruebas de abandono. Se agregan pruebas
de minimo de jugadores, expulsion y conservacion del plazo; ControlCompraTest,
PresenciaSalaTest y casos de categorias vacias en RelojRevisionTest.

Pendientes fisicos: scroll/foco/IME en telefono y tablet; memoria de Diana API25;
launcher; legibilidad; avisos sobre dialogos; reconexion con mismo UID y cambio de
host. No se ejecuta integracion Firebase Emulator en esta etapa por limite de
memoria. Los resultados de misiones anteriores no certifican estos cambios nuevos.

### Resultado final de la recuperacion M8

Emulador residual emulator/qemu cerrado con autorizacion explicita del usuario;
se comprobo posteriormente que esos procesos ya no estaban activos. Se realizaron
DOS tareas Gradle sucesivas (nunca en paralelo): testDebugUnitTest y assembleDebug.
Parametros comunes: --offline --no-daemon --no-parallel --max-workers=1,
-Dorg.gradle.jvmargs="-Xmx1280m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"
y -I docs/tools/verificacion-memoria.gradle (JVM de tests maximo 256 MiB,
maxParallelForks=1). El daemon de cada invocacion es de un solo uso y termina.
No se produjo un nuevo error de memoria en estas ejecuciones.

Primer intento de tests detecto un defecto localizado de recursos:
Widget.Basta.Inventario heredaba implicitamente de Widget.Basta, inexistente.
Se agrego parent="" a inventario_styles.xml: mismos atributos visuales, sin
cambiar mecanicas. Despues de esa correccion:
- testDebugUnitTest: BUILD SUCCESSFUL; 97 pruebas, 0 fallos, 0 errores, 0 omitidas.
- assembleDebug: BUILD SUCCESSFUL, APK debug generado con los cambios actuales.
No se repitieron Lint, instrumentacion ni Firebase Emulator en esta recuperacion.
Las 97 pruebas puras NO certifican concurrencia del protocolo nuevo en servidor,
ni el ciclo de vida Android, gestos/teclado, mascaras de launcher o heap de API25.

Pruebas/archivos actualizados en recuperacion: InterfazPartidaTest (fuente, no
compilado/ejecutado aqui), CompraComodinTest, InicioPartidaTest, RelojRevisionTest;
nuevos ControlCompraTest y PresenciaSalaTest; docs/tools/verificacion-memoria.gradle,
inventario_styles.xml y este documento. No se modifico otra logica funcional.

Revision de integridad de recursos: hay 25 PNG optimizados en drawable-nodpi;
ic_comodines.png aun permanece en drawable sin optimizacion adicional. No se
reproceso ninguna imagen. El OOM de Diana requiere nueva prueba en la tablet:
esta compilacion y las dimensiones reducidas no bastan para declararlo resuelto.

APK actualizado: app/build/outputs/apk/debug/app-debug.apk.
Sin commits/pushes, cambios de reglas, produccion, nuevas dependencias ni inicio
de otra mision. Se mantienen los pendientes fisicos y de seguridad descritos arriba.

## Mision 9 — Recuperacion visual y plazos (2026-11-14)

Cambios locales; se conserva el scroll interno de M8, las transacciones de
recompensas, transferencia de host, inventario y cierre con un participante.
Sin commits/pushes, reglas desplegadas ni lecturas/escrituras de produccion.
Se consultaron las capturas disponibles (nombres Captura de pantalla/WhatsApp)
y el XML anterior mediante git show, sin restaurar codigo. Los nombres literales
tienda_recortada.png/crear_sala_desalineado.png/partida_diseno_actual.png/
resumen_comodines_actual.png no estaban en esta copia; se usaron las referencias
existentes y defectos verificables de restricciones en XML.

### Temporizadores y letras

Regresiones comprobadas: LoadingPartida elegia 1 segundo cuando existia ID_PARTIDA;
PlazoRonda usaba min(finOriginal,basta+10s); el cronometro ademas hacia min con el
plazo recibido previamente. BASTA publicaba bandera y luego timestamp en dos
operaciones. Todos estos caminos se ajustaron:
- Preparacion inicial comun de 10 segundos, sin barrera de animaciones por cliente.
  inicioRonda es timestamp servidor de la preparacion; inicio efectivo de
  respuestas = inicioRonda + preparacionInicialMs SOLO en ronda 1. Rondas posteriores
  conservan su inicio inmediato al avanzar. LoadingPartida observa partida/reloj,
  muestra el restante y sale al plazo compartido. Si se reconecta despues, omite
  bienvenida/audio. Sounds detiene la pista al salir de preparacion.
  Una entrega de red tardia muestra el restante; nunca concede diez segundos
  nuevos ni retrasa a otros. Audio/animacion fisicos requieren comprobacion.
- BASTA aceptado publica bandera, UID y ServerValue.TIMESTAMP en UNA transaccion
  de sala, con idPartida/ronda/fase/miembro/plazo y rechazo durante preparacion.
  Fin posterior = bastaTimeStamp + 10000 incluso cerca del antiguo fin de ronda,
  segun la regla solicitada. Lecturas previas a BASTA no revierten el plazo.
  La latencia puede hacer que un dispositivo vea menos de 10 al recibir el evento;
  no se reinicia localmente para compensarla. Las guardas Java no sustituyen reglas.
- Reloj de propuestas/votos: duracion persistida 5000 ms hasta 5 elegibles y
  10000 ms con mas de 5; se fija en la preparacion del turno. No se recalcula por
  telefono ni por abandono en el mismo turno. Se conservan votos por UID,
  rechazo de tardios, id de turno y cierre unico. Contador: Tiempo para votar /
  Revisa las respuestas y decide. Categoria explicitamente vacia de todos muestra
  Respuestas vacias un segundo, puntos internos 0, sin +0 en encabezado ni voto.
- Alfabeto de 27 letras con Ñ. Selector maximo 27. Se baraja una lista nueva por
  partida y se guarda en sala; InicioPartida toma la letra de su indice de ronda,
  independientemente de la memoria del host o de candidatos locales anteriores.
  Un nuevo inicio reemplaza la lista; una transferencia no la cambia. Ñ tiene
  fallback tipografico si no existe drawable. ValidacionBasica conserva Ñ distinta
  de N al comparar iniciales; se mantienen minimo de letras, vacias y repetidas.
  API de diccionario sigue desactivada; si se reactiva revisar su cache que aun
  normaliza diacriticos. No se modificaron respuestas historicas.

### Esquema local autorizado por el usuario en esta mision

Tres campos aditivos:
- salas/{id}/preparacionInicialMs = 10000.
- salas/{id}/letrasPartida = lista barajada de 27 letras.
- salas/{id}/revisionCategorias/ronda_N/reloj/duracionMs = 5000 o 10000.
Usuario confirma que los dispositivos se reinstalan y todos usan la version nueva;
no se desarrollo un protocolo de mezcla con clientes anteriores. No se desplego
ningun cambio. Permisos/transacciones y validacion frente a clientes manipulados
quedan pendientes contra reglas reales; no se inventan reglas de produccion.

### Visual e interaccion

- Partida recupera presencia del logo y reloj/letra: dimensiones adaptables
  (logo 96/120 dp y reloj 120/140 dp segun altura) y letra 104 dp. Se conservan
  contenedores, fuentes, colores, jugadores, BASTA/Salir fijos y hoja con RecyclerView
  interno. No se recupera ScrollView exterior ni se altera manejo IME de M8.
- Tienda: el carrusel tenia altura fija 270 dp; ahora wrap_content y LM sin
  hasFixedSize, cache de vistas reducida y anchura de tarjeta acotada al espacio.
  Precios/inventario/compra conservan estilo y categorias. HomeActivity respeta
  inset inferior del sistema para no ocultar controles bajo navegacion.
- Crear sala: tarjetas de configuracion se apilan cuando no caben dos columnas
  (<440dp o fuente grande); categorias mantienen dos columnas salvo ancho muy
  estrecho/fuente aumentada. Controles de conteo y slider minimo 48dp. Maximo 27.
- Menu: encabezado centrado independientemente del icono, introduccion amigable,
  secciones Durante la partida/En resultados, estado vacio ilustrado con recurso
  existente de 256px. Se comprobo que el XML actual aun tenia la pill 0dp/weight:
  se sustituye por badge wrap_content compartiendo Widget.Basta.Inventario, ID
  txtCantidadComodinPartidaItem conservado y USAR separado. Encabezados se muestran
  solo en primer item/cambio de grupo. Fases/rareza/iconos/efectos se mantienen.
- Resumen: icono 56dp, titulo Lilita 19sp, descripcion Manrope 14sp, padding 16dp,
  scroll/colores de resultado existentes. Narraciones conservadas; perdida/ganancia
  se calcula SOLO de puntosBase/puntosFinales registrados. Sin importe se indica
  que no esta disponible; Congelador conserva duracion registrada sin inventar puntos.
- Compra: CustomDialogHelper existente devuelve Dialog para controlar su cierre.
  Confirmacion de nombre/precio/moneda, Cancelar/Comprar; una confirmacion por proceso
  y una transaccion pendiente. Desaparece espera fija de dos segundos. Inventario
  muestra siempre cantidad real, botones bloqueados solo durante transaccion;
  saldo/inventario y avisos agrupados no cambian. Al destruir vista se descarta
  confirmacion, mientras el ViewModel conserva operacion ya enviada.
  Dos dispositivos sobre el mismo usuario siguen protegidos por transaccion de
  saldo/inventario; dos compras confirmadas distintas son dos compras legitimas.
- Expulsar: mismo dialogo estilizado que abandono, con nombre y Cancelar/Expulsar.
  UI excluye UID propio (no solo bandera esHost), incluida indicacion de accion.
  Transaccion revalida host y sala en espera; durante ronda se rechaza.
- Lobby: copiar invitacion mediante icono vectorial de 48dp y aviso personalizado.
  O mayuscula en rojo, 0 normal, explicacion y descripcion accesible diferenciada;
  valor/codigo buscado y texto del portapapeles no se alteran.

### Icono y arranque

Se uso el PNG NUEVO de docs/referencias-ui/basta_online_icon.png (esquina con
alpha 0), no la copia anterior. Solo ese recurso se redimensiono: 5 densidades
legacy/round/adaptativas, fondo azul y margen seguro de mascara. No se ejecuto
el script de optimizacion masiva de M8 ni Python. Splash usa version nodpi 440px
(<1MiB RGBA), conservando su Activity, animacion, duracion y flujo de inicio.
Android 12+ usa tema de arranque crema y la API nativa SplashScreen, con icono
transparente en la pantalla obligatoria para evitar duplicar el logo antes de
SplashActivity. Sin nueva dependencia; API25 usa tema base/legacy. Validar
mascaras, nitidez y continuidad real en API25/31+, sin afirmar validacion fisica.

### Codigos y limpieza

Defecto comprobado: Crear_salaFragment escribia codigosSalas/{codigo} con setValue
sin reserva. Ahora ReservaCodigo/CreacionSala reservan atomicamente el nodo ya
existente usando idSala como propietario y estaActiva=false; publican sala completa
con host/jugador/configuracion y luego activan solo su reserva. Colision no crea
otra sala. Fallo libera solamente reserva del mismo ID aun inactiva. Tras liberar,
se intenta retirar la sala parcial solo si conserva ID/host, sigue en espera y
no tiene otros jugadores; nunca borra una nueva sala ni una partida activa.
No se agregaron campos a codigosSalas ni se cambiaron las reglas desplegadas.

Salida existente: TransferenciaHost elimina sala cuando no quedan jugadores;
SalaActivity libera codigo en transaccion comprobando idSala. La gracia de 90s
permanece: una ausencia temporal no elimina sala/codigo. La limpieza depende de
que quede un cliente ejecutando el coordinador: si todos desaparecen no existe
un barrido servidor. Callbacks onDisconnect pueden dejar nodos de sesiones sin
jugadores despues del cierre; un fallo/permisos durante creacion puede dejar una
reserva pendiente. NO se hizo limpieza masiva ni se declara resuelto ese caso.

Propuesta pendiente (NO implementada): tarea programada del servidor que revalide
sala realmente vacia, sesiones sin conexion y gracia vencida, y libere codigo
solo si idSala aun coincide. Para reservas sin sala tras un crash hace falta
reservadaEn con timestamp servidor y plazo de expiracion, mas permisos/server
coordinador aprobados. No borrar registros antiguos sin edad/presencia demostrable.
No se despliega tarea ni se crea ese campo sin nueva autorizacion.

### Archivos principales

Utils: PlazoRonda, InicioPartida, LetraSala, ValidacionBasica, RelojRevision,
CoordinadorRevision, ControlCompra, ComprasViewModel, CustomDialogHelper,
CodigoSala, ResumenComodines, Sounds; nuevos ReservaCodigo, CreacionSala e
InvitacionSala. Fragments: Crear_sala, Lobby, LoadingPartida, Partida y Tienda;
HomeActivity/SplashActivity y Manifest. Adapters: CategoriaTienda, ComodinTienda,
ComodinPartida y JugadorSala. Layouts correspondientes, item_carril_tienda,
item_resumen_comodin, dimensiones/tema v31, mipmaps y vectores ligeros.
Tests: PlazoRondaTest, InicioPartidaTest, RelojRevisionTest, ControlCompraTest,
ResumenComodinesTest; nuevos LetraSalaTest y ReservaCodigoTest. Emulator: dos
casos nuevos, sourceSet de ReservaCodigo y script verificar-m9.cmd. Script
launcher-m9.ps1 genera solo iconos desde la fuente nueva.

### Verificacion final M9

- testDebugUnitTest: 108 pruebas, 0 fallos, 0 errores y 0 omitidas. Incluye
  bienvenida/BASTA, lecturas anteriores a BASTA, letras de 1/10/27 rondas y Ñ
  distinta de N, cambio de host/nueva partida, votacion para 5/6 elegibles,
  categoria vacia, reservas/limpieza protegida, compra inmediata tras confirmar
  y narracion de perdida registrada. Permanecen pruebas de misiones anteriores.
- RTDB Emulator autorizado expresamente: 16 pruebas PASSED, demo-basta-concurrency,
  127.0.0.1:9000; colision de codigos y BASTA concurrente con timestamp real,
  ademas de recompensas, compras, host y reentrada previos. Reglas fixture locales,
  no Auth real. El caso legacy de presencia sigue separado de la suite de exito.
  La prueba de compra usa dos conexiones sobre un mismo nodo de usuario; no son
  dos interfaces Android autenticadas. El caso de reloj espera 5 segundos reales;
  5/6 elegibles y 10 segundos se verifican en helpers, no con seis dispositivos.
- assembleDebug: BUILD SUCCESSFUL. Se verifico APK actualizado despues de los
  ultimos ajustes de indicacion propia del host y O/0 tambien en partida.
- Ejecuciones secuenciales, --offline --no-daemon --no-parallel --max-workers=1,
  Gradle Xmx1280m / Metaspace256m y tests 256m con el init de memoria existente.
  JVM RTDB limitada a 512m. No se iniciaron emuladores Android ni hubo nuevos
  errores de memoria. No se ejecuto Python, Graphify, Lint ni instrumentacion.
  No se agregaron dependencias. git diff --check de archivos modificados sin
  errores de whitespace; advertencias de futura conversion LF/CRLF normales.

APK final: app/build/outputs/apk/debug/app-debug.apk (104727903 bytes).
Prueba fisica pendiente en telefono pequeno y tablet API25: scroll interno/foco
con IME, fuentes aumentadas, carrusel completo y precios, nueva composicion,
mascaras de launcher/arranque Android 12+, audio/10 segundos y reconexion.
El OOM previo de Diana NO queda certificado como resuelto por compilar.
Probar 2/4/8 jugadores, host en segundo plano y misma cuenta en dos telefonos.
La limpieza automatica cuando no queda ningun cliente conectado requiere el
servicio/esquema propuesto arriba y nueva autorizacion; no esta implementada.
No se modifico produccion ni se inicio Battle Royale/espectadores.

## Mision 10 — Esquema y alcance antes de implementar (2026-11-15)

Campos aditivos locales previstos: salas/{id}/expulsados/{uid}=true, tombstone de
pertenencia que se conserva mientras exista esa sala; no elimina cuentas. Entrada,
expulsion e inicio consultan la misma sala transaccionalmente. Una incorporacion
nueva comienza estaConectado=false/desconectadoDesde=timestamp servidor y solo
presencia confirmada la activa. No se agrega un estado de usuario global.

Revision comparte modos revision (3500ms), propuestas (seleccion de respuesta sin
caducidad local), votos (5000/10000ms sin segunda barrera) y resultado (3000ms).
Categoria completamente vacia usa 2000ms. reloj/elegibles/{uid}=true congela los
votantes iniciales; salientes dejan de ser requeridos. Se reutilizan inicio,
duracionMs, turno, categoria, idVotacion. No se cambia reloj de respuestas/BASTA.
Para duelos se preve ultimoMinijuego en sala y anuncioDesde en minijuegoActual,
con timestamp servidor; no se reinicia anuncio al recrear pantalla.
Implicaciones: todos los clientes de pruebas deben actualizarse. Permisos de estos
campos y transacciones dependen de reglas reales NO verificadas ni desplegadas.
Memoria Explosiva queda como propuesta si requiere un protocolo nuevo de
secuencia, intentos y desempate autoritativo; se preservan los tres juegos.

M10 ampliacion de registro local: Puntos Saltarines reutiliza comodinesResultados
con efecto=puntos_saltarines y uidOrigen/uidDestino/cantidad/mensaje del efecto
real. Registro y ambos saldos de ronda se escriben en una transaccion, una vez
por idUso. Ninguna escritura de puntos historicos. El host nuevo tambien se
publica desconectado con timestamp de incorporacion pendiente hasta SalaActivity.
La referencia resumen_comodines_aprobado.png no esta en esta copia local: se
conservan los componentes actuales y se aplican los tamanos/estilos descritos,
sin afirmar que se comparo una imagen ausente.

## Mision 10 - Implementacion y verificacion local (2026-11-15)

Decision confirmada por el usuario: las votaciones se solicitan manualmente con
Votar. No hay recorrido automatico obligatorio de todas las respuestas. Tras
3.5 segundos de revision se habilitan solicitudes sin un plazo de seleccion que
las cierre inesperadamente. Solo el host actual puede Finalizar categoria; la
transaccion comprueba partida/ronda/indice/turno, categoria no cerrada y ausencia
de votacion activa. Finalizar y solicitar simultaneamente son excluyentes.
Respuestas sin solicitud conservan su clasificacion inicial. Si ya no queda
ninguna candidata, se avanza automaticamente tras el resultado. Cada respuesta
resuelta queda bloqueada por su clave canonica; no hay limite fijo de cinco
respuestas por categoria. La opcion API de validacion continua desactivada.

Cambios comprobados en codigo:
- IngresoSala/Unirse_salaFragment: admision transaccional en la raiz de sala,
  valida cupo, lobby, identidad y expulsados. Una nueva incorporacion permanece
  desconectada hasta registrar presencia desde SalaActivity. Crear_salaFragment
  aplica tambien este criterio al anfitrion. InicioPartida registra la expulsion
  y retira pertenencia/preparacion en la misma transaccion. PresenciaSala no puede
  reconstruir un miembro expulsado desde sesiones antiguas. El bloqueo dura la
  vida de esa sala; no borra la cuenta ni impide entrar a otra sala.
- RelojRevision/VotacionRevision/CoordinadorRevision/ClaveRevision y PartidaFragment:
  revision 3500ms, vacias 2000ms sin votos; voto inmediato de 5000ms hasta cinco
  elegibles o 10000ms desde seis; resultado 3000ms. El inicio usa timestamp servidor
  y no espera otra preparacion dentro del dialogo. Cierre anticipado si todos los
  elegibles aun presentes votaron, incluyendo abstencion. Empate si/no o cero votos
  conserva la validacion inicial, no concede cinco puntos. Repetidas siguen con
  cinco y vacias con cero. Recepciones repetidas/cierres tardios se rechazan.
  La barrera inicial de carga conserva su maximo de espera de ocho segundos.
- dialogo_votacion_palabra/item_tarjeta_revision/dialogo_respuestas y adapter:
  panel inferior sin modal tactil, espacio desplazable bajo ultimas respuestas,
  confirmacion Has realizado tu voto y eleccion; contador/cantidad de votos visibles.
  Nuevo boton Finalizar categoria del host. No se modifico la hoja de respuestas.
- SeleccionMinijuego/ComodinMarcaDeLaMuerte/PartidaFragment y tres XML de duelo:
  seleccion equiprobable entre juegos distintos al ultimo, publicada una vez en
  transaccion de sala; anuncio persistido con reglas seis segundos y cuenta de
  combate tres segundos, nombres destacados y MODO ESPECTADOR. PPT valida UID
  participante dentro de transaccion y rechaza espectador o segunda eleccion.
- PuntosSaltarines/ComodinRuletaCaotica: reemplaza solo el evento de poner a cero.
  Dos participantes activos distintos transfieren min(5, saldo disponible), con
  recibo idUso y ambos saldos de ronda atomicos. Mensaje y resumen muestran valores
  reales; no modifica historial global. Otros eventos de ruleta se conservan.
- Crear_salaFragment/XML: Rondas/Jugadores en dos columnas, controles de 48dp;
  apilado solo en anchura excepcional (<300dp, o <360dp con fuente >1.8).
  Maximo 27 rondas preservado. codigo_aclaracion.xml explica O roja/0 normal con
  negrita; valor de codigo y copia intactos. Resumen conserva narraciones y sus
  tarjetas/IDs, iconos 56dp, descripcion 14sp, titulo mayor y efectos destacados.

Memoria API25 (sin largeHeap):
- NotificationHelper decodificaba los cuatro ic_notificacion_* de 1254x1254 en
  drawable: unos 6 MiB ARGB8888 cada uno antes de posible escalado de densidad.
  Ahora son 144x144 en drawable-nodpi, unos 81 KiB cada uno. Al terminar un aviso
  se retira el host en vez de retener una card oculta vinculada a Activity.
- Crear sala usa bg_fondocuaderno, logo_basta, nueve iconos de categoria y ademas
  btn_atras1, ic_crono e ic_siluetas. Estos tres ultimos tambien eran de 1254 o
  500 pixeles y se limitaron a 256. Fondo max1280, logo max960, categorias192,
  todos nodpi. Son 18 recursos puntuales, no procesamiento masivo; originales
  conservados en docs/referencias-ui/originales-m10, fuera de recursos del APK.
- RecursosMemoriaTest lee solo cabeceras PNG y comprueba dimensiones, ubicacion y
  presupuesto teorico combinado <9 MiB ARGB8888. NO mide heap real ni demuestra
  ausencia de OOM. Home conserva fragments para mantener estado; revisar retencion
  de bitmaps y memoria acumulada navegando inicio/tienda/crear/lobby en la tablet.
  Launcher, bienvenida, scroll interno, BASTA y plazo de ronda no se reconstruyeron.

Pruebas ejecutadas, secuencialmente, sin Python ni Android Emulator:
- testDebugUnitTest: 115 pruebas, cero fallos. Incluye Mision10Test (cinco/ocho
  solicitudes manuales, empate conserva original, cierre temprano, tiempos,
  finalizacion host/turno/repetida/tardia, ingreso/expulsion, minijuegos/PPT,
  saltarines) y RecursosMemoriaTest. Se actualizaron expectativas anteriores de
  RelojRevisionTest sin eliminar pruebas. Un intento intermedio fallo al insertar
  una guarda en metodos equivocados; se corrigio y la ejecucion final paso.
- RTDB Emulator demo-basta-concurrency: 20 pruebas, cero fallos. SDK/transacciones
  reales locales: entrada contra expulsion, duelo concurrente, saltarines unico,
  finalizar contra votar, reloj real/cierre concurrente/empate; se conservaron
  las pruebas de recompensas, host, codigos, compras y reconexion anteriores.
  Reglas de fixture aislado, NO reglas reales desplegadas ni garantias de clientes
  maliciosos. El diagnostico excluido de presencia antigua no forma parte de estas
  20 pruebas y no se declara corregido por este resultado.
- Gradle limitado a un trabajador, no paralelo, heap1280MiB, test JVM256MiB;
  RTDB Java512MiB. diff --check de archivos de codigo/layout tocados sin errores,
  solo advertencias LF/CRLF. Sin dependencias nuevas, commits, pushes ni produccion.

Pendientes y limites:
- Validacion fisica API25 de memoria acumulada y decodificacion al navegar; scroll
  con cinco/ocho respuestas y panel abierto, IME, tamanos pequenos/fuentes grandes,
  audio y recreacion del anuncio de duelo. No se ejecutaron instrumentadas ni Lint.
- Reglas reales y permisos para expulsados, elegibles, ultimoMinijuego/anuncioDesde
  y registros saltarines pendientes de validacion antes de desplegar.
- Debito de inventario del comodin y publicacion del duelo/ruleta siguen siendo
  operaciones en nodos distintos: un error entre ambas puede consumir inventario
  sin efecto. La atomicidad probada es la del efecto/recibo de sala, no el consumo
  completo del comodin. No se agregaron reembolsos inseguros.
- Memoria Explosiva no implementada: requiere secuencia/intentoss/tiempos y desempate
  compartidos nuevos; se conservan los tres minijuegos existentes.
- resumen_comodines_aprobado.png no disponible en esta copia: estilos ajustados a
  requisitos y componentes existentes, sin comparacion visual de archivo ausente.

Cierre M10: assembleDebug BUILD SUCCESSFUL (26s). APK actualizado: app/build/outputs/apk/debug/app-debug.apk (94482722 bytes). Detener aqui: no iniciar otra mision.


## Correccion definitiva de votaciones (2026-11-15): esquema previo
Se reutilizan modos/reloj existentes. Dos campos aditivos solo durante votos, reloj/seleccionInicio y reloj/seleccionDuracionMs, conservan el plazo previo si el intento termina sin votos. No cambian reglas desplegadas. Todos los clientes de pruebas deben actualizarse. resultado pasa a representar la presentacion final completa de 3000ms; propuestas tiene ahora vencimiento 5000/10000ms. Se elimina la finalizacion manual. Un cliente activo puede completar transiciones; sin ningun cliente conectado no hay ejecutor servidor y se recupera al reconectar.


## Correccion definitiva de votaciones - Resultado local (2026-11-15)

Esta seccion sustituye el flujo manual sin vencimiento y boton Finalizar categoria
registrado en M10; conserva el resto de esa mision.

Flujo implementado:
1. Carga/preparacion existente con espera maxima 8s; revision compartida de 3500ms,
   sin solicitudes. Si todas estan vacias: solo 2000ms y siguiente categoria.
2. Seleccion manual opcional con inicio servidor y duracion persistida 5000ms hasta
   cinco elegibles, 10000ms desde seis. No se votan respuestas automaticamente.
3. Una solicitud valida abre una sola votacion transaccional con su propio plazo
   de 5000/10000ms y listado de elegibles. Cierre al vencer o al votar todos los
   elegibles aun presentes. El resultado se registra una vez por clave de palabra.
4. Con al menos una entrada de voto registrada (incluye Paso explicito), el cierre
   concede un nuevo plazo completo de seleccion si quedan candidatas. No hay
   pausa adicional de tres segundos entre votaciones. Cada resultado ya resuelto
   queda visible y no puede solicitarse otra vez. Empates y abstencion mantienen
   clasificacion original; vacias y repetidas conservan sus reglas anteriores.
5. Sin votos registrados: la respuesta queda resuelta con su valoracion inicial,
   pero se restaura seleccionInicio/seleccionDuracionMs, nunca un plazo nuevo.
   El tiempo usado por ese dialogo no se regala: si el plazo anterior ya vencio,
   comienza la presentacion final al cerrar el intento. Si no quedan candidatas,
   comienza directamente esa presentacion, haya o no votos.
6. Al vencer seleccion sin voto activo: se completan clasificaciones faltantes,
   se deshabilitan solicitudes y se muestran simultaneamente todos los puntajes.
   modo resultado dura 3000ms compartidos y luego cierra/avanza una sola vez.
   No existe boton Finalizar categoria ni su operacion de cierre manual.

No se reinicia el reloj por cada voto, recepcion duplicada, solicitud repetida,
reconexion ni recreacion. El turno, partida, ronda, categoria y resultado resuelto
rechazan solicitudes viejas. Un participante activo cualquiera puede completar
cierre/transicion de revision, sin depender del host para avanzar categorias.
Sin ningun cliente conectado no hay ejecutor servidor: al regresar se lee el
plazo persistido y se completa lo vencido, sin ampliar tiempo. El cierre/resumen
posterior de ronda conserva su mecanismo anterior y transferencia de anfitrion.

Archivos de produccion tocados exclusivamente para este flujo:
- utils/RelojRevision.java: plazos de seleccion, restauracion sin votos, resultado
  final compartido y avance automatico.
- utils/VotacionRevision.java: captura de participacion antes de aplicar cambios,
  marca resuelta y transicion atomica con resultado. Se conservan votos unicos,
  cierre tardio rechazado y empate con valoracion inicial.
- utils/CoordinadorRevision.java: contador visible tambien durante seleccion,
  vencimiento automatico y revelado de todos los puntajes durante resultado.
- Fragments/PartidaFragment.java y layout/dialogo_respuestas.xml: retirada del
  boton Finalizar categoria y su listener; sin rediseno ni cambios en scroll.

Pruebas:
- testDebugUnitTest: 120 pruebas, cero fallos. Nueva SeleccionManualTest cubre tres
  votaciones/reinicios, eventos repetidos, votos tardios, intento sin votos, host
  desconectado, ocho elegibles, solicitudes antiguas y todas resueltas. Mision10Test
  cubre categoria sin solicitudes, presentacion final exacta, cinco/ocho solicitudes
  sucesivas. RelojRevisionTest actualizado para la nueva semantica conservando sus
  comprobaciones. LayoutsPartidaTest comprueba estructura desplazable y ausencia
  del boton; NO demuestra gestos/scroll reales en un dispositivo.
- RTDB Emulator demo-basta-concurrency: 21 pruebas, cero fallos, transacciones SDK
  reales locales. Dos solicitudes simultaneas distintas abren solo un voto; carrera
  entre vencimiento y solicitud es excluyente; se mantienen votos y cierres unicos,
  empate original y las pruebas anteriores de recompensas/host/entradas/compras.
  El reloj de cinco segundos del caso existente espera tiempo real. Los casos de
  carreras usan instantes de prueba controlados, no son una prueba de reloj seguro
  contra clientes maliciosos ni validan reglas desplegadas.
- Primer intento: dos expectativas antiguas de RelojRevisionTest fallaron por
  esperar avance inmediato y el modo anterior; se actualizaron sin borrar pruebas.
  La ejecucion final paso. No hubo error de memoria.
- Gradle un trabajador, no paralelo, heap1280MiB y JVM de tests256MiB; RTDB512MiB.
  Sin Android Emulator, Python, Graphify, dependencias nuevas, commits ni pushes.

Pendiente fisico: 2/5/8 jugadores; no cuestionar nada; cuestionar tres respuestas;
abrir sin votos; host sale durante voto; reconexion; scroll hasta ultima tarjeta
antes/durante/despues de dialogo, puntuaciones simultaneas y categorias vacias.
No se modifico el layout de scroll ni la hoja de respuestas. Las reglas reales
para nuevos campos y transacciones siguen NO verificadas; no se toco produccion.

Compilacion final: assembleDebug BUILD SUCCESSFUL (26s). APK: app/build/outputs/apk/debug/app-debug.apk, 94482574 bytes, generado 2026-11-15 03:31:04.


## MISION 11 - Pulido y continuidad local (2026-11-15)

Alcance: cambios localizados; sin produccion, reglas desplegadas, commits, pushes,
Python, Graphify, dependencias nuevas ni Android Emulator. Conservados el scroll
interno de escritura, icono/copia, diseno de votos, menu de poderes y plazos de ronda.

Sincronizacion y conexion:
- ConexionJuego (inicializada desde BastaAplication) mantiene una sola escucha de
  /.info/connected y offset por proceso, mas NetworkCallback para perdida real de
  interfaz. No toma Wi-Fi como prueba de acceso a Firebase. ACCESS_NETWORK_STATE
  es permiso normal local. No hay polling de red ni bucles de reinicio de Firebase.
  Dialogo ligero crema/azul, tipografias existentes y vector Wi-Fi tachado, sin
  bitmap grande. Bloquea interaccion en Home/login/registro/sala durante desconexion;
  Salir de la sala permanece disponible localmente. NotificationHelper.superposicion
  vuelve a poner el aviso sobre dialogos que lleguen asincronicamente.
- SalaActivity permite salir incluso offline: detiene sonidos, termina Activity,
  limpia listeners/navegacion y pasa a Home, que conserva el aviso global offline.
  No afirma haber eliminado el UID en el servidor. Solo cierra su token de sesion
  unico; si la escritura queda pendiente, no afecta sesiones nuevas ni otra sala.
  La pertenencia se reconcilia por onDisconnect y la gracia existente de 90000ms;
  no se agenda una eliminacion tardia global de cuenta/UID.
- ComprasViewModel, creacion, admision, listo/inicio de lobby y solicitudes/votos
  usan disponibilidad/epoca local de conexion donde corresponde. Cambiar la conexion
  invalida esas operaciones pendientes al reintentarse. Las transacciones de sala
  ademas comprueban identidad de partida, ronda, fase y/o turno. El bloqueo visual
  complementa esas comprobaciones y no sustituye seguridad de servidor.
- Recepcion confirmada de sala al reconectar con get(): verifica pertenencia,
  expulsion y reserva vigente, carga perfil y registra su sesion. Partida recupera
  cronometro desde el plazo compartido si quedo FINALIZANDO antes de enviar, sin
  escribir un nuevo inicio. LoadingPartida consulta fase/timestamp: no reproduce
  bienvenida fuera de jugando ni despues del plazo, y toma idPartida actual.
- InicioPartida.enviarRespuestas es transaccional, una vez por UID/ronda. Rechaza
  partida anterior, usuario sin presencia, otra fase y una revision ya iniciada.
  No convierte una respuesta ausente en vacia durante el tiempo de responder:
  conserva la espera maxima de cinco segundos de la revision anterior antes de
  cargar ausencias. No se agrego una ventana nueva de respuestas ni un diccionario.

Host y cierre:
- Defecto comprobado: reconciliar mantenia la corona de un host desconectado si
  conservaba pertenencia. Ahora elige lexicograficamente un conectado, dentro de
  transaccion de sala, conserva la reserva del anterior y no le devuelve corona
  al reconectar. Agregacion de sesiones y reconciliacion se confirman juntas.
- Defecto comprobado: se marcaba rondasProcesadas antes de terminar puntos, seguido
  de updateChildren y sumas por usuario separadas. CierreRevision publica puntos
  de ronda, acumulados, resultado Doble o Nada (misma formula), fase y recibos
  resumenRondaListo/rondasProcesadas en una transaccion. Puede completarla cualquier
  participante activo con revision completa; repeticion no incrementa acumulados.
  No hay bloqueo previo que dependa del telefono que lo adquirio.
- La espera de resultados y avanzar ignoran a los desconectados confirmados y
  reservados, conservando sus pertenencias. El host actual confirma la siguiente
  ronda con id/fase/ronda en transaccion; el anterior no puede retrocederla.
  Los votos ya emitidos no se borran cuando su autor sale: dejan de requerirse
  nuevos votos de alguien sin pertenencia, pero se conserva la decision registrada.
- Los metodos antiguos de calculo siguen en el archivo por no hacer una limpieza
  ajena; cerrarRondaComoHost ya utiliza exclusivamente CierreRevision. No mezclar
  APK antiguos con esta version en salas en curso. No se migro una ronda parcialmente
  pagada por un APK anterior; iniciar sala nueva para las pruebas de esta version.

Expulsion y cupo (confirmacion):
- expulsados/{uid}=true es un veto intencional durante toda la vida de ESA sala.
  Persiste al reiniciar porque esta en RTDB, no es una sesion/reserva mal limpiada.
  Otro idSala no comparte veto. IngresoSala rechaza el UID vetado y las sesiones
  viejas solo escriben su token, no reconstruyen jugadores. Reglas contra un cliente
  malicioso siguen sin verificar. No se cambio ni se agrego un sistema de vetos.
- Cupo: admision en raiz de sala valida maxJugadores y crea pertenencia en la misma
  transaccion. Dos aspirantes a la ultima plaza no pueden confirmar ambos. Reentrada
  valida actualiza identidad sin duplicar UID. Inicio de sala no admite miembros
  nuevos; fases lobby/espera se validan, no solo etiqueta En juego.
- IngresoSala.reservaVigente comparte criterio entre admision por codigo y recuperacion
  de Activity: una sesion activa preserva la reserva, callbacks antiguos no invalidan
  otra activa, y ausencia confirmada de 90s rechaza reentrada. Se usan timestamps y
  offset Firebase, no se expulsa por presionar Home ni por reloj local sin offset.
- Home distingue Abandonaste la sala / Has sido expulsado de la sala; reserva
  vencida o sala inexistente muestra lugar no disponible. No anuncia abandono
  voluntario cuando el motivo fue un veto comprobado.

Validacion, votaciones y renderizado:
- ValidacionBasica conserva filtro local de >=3 letras y prefijo de ronda, normaliza
  NFC/mayusculas/acentos vocalicos manteniendo N y Ñ distintas. No intenta existencia.
  VotacionRevision rechaza estructuralmente invalidas y fuerza +0 incluso si llega
  un cierre viejo con mayoria valida. EstadoRevision hace definitivos los ceros.
  No se cambio deteccion de repetidas ni el flujo 3.5s/seleccion/voto/reinicio/3s.
- ConteoVotos cuenta solo elegibles iniciales: sin voto o Paso => abstencion, nunca
  mayoria si/no. Se guardan estadisticas aditivas locales validos/invalidos/abstenciones
  en resultadosVotacionPalabras/ronda_N/clave; no requieren sobrescribir usuarios.
  Contadores actuales: 16sp, negrita, rojo/verde/ocre. Reglas reales de estos campos
  pendientes de validacion; no se desplego nada.
- Hallazgo de layout: ScrollView exterior y RecyclerView interno wrap_content con
  gestor vertical competian en desplazamiento/medicion. El gestor interior ahora
  no desplaza verticalmente, el exterior conserva scroll y se solicita layout/dibujo
  al enlazar una pagina. Sin rediseno ni cambiar la hoja de escritura. El problema
  blanco intermitente NO se ha reproducido ni certificado fisicamente aqui.
- Crear sala usa el mismo criterio para el GridLayoutManager interno, sin altura
  fija, sin animador de items y con re-medicion al enlazar. Controles +/- mantienen
  48dp tactiles con fondo inset de 5dp; dos columnas y limites 27/8 conservados.
- FINALIZANDO se dibuja a 17sp y BASTA restaura 26sp. Dimensiones compactas de logo/
  reloj 84/108dp en configuracion base (se mantienen variantes amplias), sin tocar
  temporizador. Boton existente incorpora Usar comodines. Carrito vacio tint oscuro.
- Lobby: una instruccion junto a lista, visible solo al host. Subtitulo queda debajo
  del contenido de tarjeta, Reconectando mantiene reserva y explica el motivo.

Ruleta:
- Ya no existia el evento de poner todo a cero: se conserva Puntos Saltarines.
  EfectoRuleta ahora aplica los cinco efectos con datos actuales y recibo idUso en
  transaccion, no updateChildren calculado sobre un ranking viejo. Semillas/efecto
  se capturan una vez fuera de reintentos, recibo evita reaplicar.
  La inversion mantiene saldos negativos anteriores sin intentar invertir el signo
  (el codigo viejo podia lanzar NumberFormatException); no crea una penalizacion nueva.
- Cuenta 5..1 con timestamp existente eventoCaosGlobal, icono real rotando y texto
  Eligiendo un efecto al azar. Revela titulo/mensaje real despues del plazo. Una
  reconexion usa tiempo restante; SharedPreferences por idSala/idUso evita repetir
  una revelacion ya vista en ese dispositivo. Cambiar de dispositivo puede mostrar
  el recibo, pero nunca vuelve a ejecutar el efecto. Handler se cancela al cerrar.
  El efecto se confirma atomico antes de la animacion; la animacion no frena el juego.
- Limite preexistente: descuento de inventario (BaseComodin) y efecto de sala son
  nodos/operaciones separados. Se agregaron guardas de conexion, pero una caida entre
  ambos puede consumir inventario sin efecto. No se promete atomicidad integral ni
  se hicieron reembolsos inseguros. Solucion fuerte requiere coordinacion/recibo
  transaccional servidor entre usuario y sala y validar reglas/infraestructura.

Limites de continuidad / servidor pendientes:
- Lo verificado depende de clientes cooperativos conectados y de la presencia que
  RTDB publique. Si un proceso se congela pero su TCP/sesion sigue activa, no hay
  prueba autoritativa de inactividad. No se inventa una expulsion por un reloj local.
  Este caso requiere lease/heartbeat y coordinacion servidor (y reglas que cercen
  el coordinador anterior), o un protocolo adicional autorizado y probado. No se
  implemento un lease nuevo ni infraestructura de produccion.
- Si todos desaparecen no hay ejecutor cliente: reconciliacion/cierre ocurren al
  volver alguno. Limpieza autonoma, vencimientos autoritativos y recuperacion de
  un coordinador congelado sin perder TCP requieren servicio servidor. No declarar
  continuidad completamente garantizada para esos casos.
- La epoca de conexion es una guarda cliente: no cancela retroactivamente una
  escritura que el servidor ya confirmo, ni protege contra clientes manipulados.
  Reglas reales de membresia, transacciones de raiz y estadisticas NO verificadas.
  El diagnostico presenceLimit antiguo sigue excluido de la suite normal.
- NO PROBADO fisicamente: API25/heap, perdida y retorno real de red, dialogo sobre
  otros modales, salir offline y recuperar Home, renders de Pelicula/Cancion, scroll
  con 5/8, creacion en pantalla pequena/fuentes grandes y cuenta animada compartida.
  Pruebas unitarias/RTDB no demuestran layout, IME ni lifecycle real de Android.

Guion fisico: dos y ocho jugadores; host pierde internet en bienvenida/respuestas/
  voto/resultados; conectados terminan y confirman siguiente ronda; host vuelve
  sin corona ni reloj reiniciado. Abrir app offline, probar crear/entrar/comprar/
  votar/usar poder bloqueados y Salir de sala sin forzar cierre. Probar host congelado
  con TCP vivo por separado y registrar el limite servidor. Revisar todas categorias
  sin tocarlas, ultima tarjeta accesible y expulsado que reinicia app.

M11 - Validaciones finales:
- PASA testDebugUnitTest: 134 pruebas, cero fallos/errores. Mision11Test cubre
  acentos/Ñ, invalidas no cuestionables ni convertibles por mayoria, abstenciones
  elegibles, reserva/sesiones/expiracion, no cupo doble, relevo sin devolver corona,
  cierre/recibo/acumulado una vez, Doble o Nada sin cambio de formula, respuestas
  antiguas descartadas, listo viejo bloqueado y cinco efectos de ruleta idempotentes.
- PASA RTDB Emulator demo-basta-concurrency: 24 pruebas, cero fallos/errores. Nuevas:
  ultima plaza concurrente + reentrada, host ausente + dos cierres + siguiente ronda
  + regreso sin corona, ruleta concurrente con un recibo. Se mantienen las pruebas
  anteriores de votos/cierres/host/expulsion/presencia/reconexion/compras/recompensas.
- Intentos intermedios fallaron por expectativas antiguas: dos fixtures de jugadores
  sin estado de conexion, la eliminacion de un voto al abandonar, y Manzana como
  respuesta a letra A. Se preservaron las pruebas y se actualizaron sus fixtures/
  expectativas para el requisito definitivo, sin omitir casos para conseguir PASA.
- diff --check de los Java/XML tocados sin errores, solo advertencias LF/CRLF.
  No se ejecuto Lint ni instrumentacion Android. No hubo error de memoria. Gradle
  offline/no-daemon/no-parallel/max-workers=1, heap1280MiB; tests256MiB; RTDB512MiB.
- El aviso de conexion es desplazable si crece la fuente y oculta IME al abrir,
  para conservar acceso al boton local Salir. Cambio visual compilado, NO prueba
  automatizada de su posicion tactil real.

Archivos principales M11: BastaAplication, HomeActivity, SalaActivity; utils/
ConexionJuego, CierreRevision, ConteoVotos, EfectoRuleta, InicioPartida, IngresoSala,
TransferenciaHost, ValidacionBasica, EstadoRevision, VotacionRevision,
CoordinadorRevision, ComprasViewModel, CreacionSala, Sounds y NotificationHelper;
Fragments/PartidaFragment, LoadingPartidaFragment, LobbyFragment, Crear_salaFragment,
Unirse_salaFragment; Adapters/TarjetasValidacionAdapter y JugadorSalaAdapter;
comodines/BaseComodin (solo guardas de red) y ComodinRuletaCaotica; Manifest,
layouts de partida/lobby/jugador/crear/voto/menu, partida_dimens y tres vectores/
insets pequenos; Mision11Test, TransferenciaHostTest y pruebas/config RTDB.
No se alteraron las mecanicas de otros poderes, launcher, copia ni hoja interna.

M11 APK: assembleDebug BUILD SUCCESSFUL (39s). app/build/outputs/apk/debug/app-debug.apk (94484665 bytes), 2026-11-15 11:30:38. Mision detenida aqui, sin commits/pushes.


## Responsive y memoria despues de M11 (2026-11-15)

Alcance: exclusivamente recursos, medicion/renderizado y distribucion visual.
No se modificaron transacciones, plazos, puntuaciones, admision, expulsiones,
recompensas ni flujo de votacion. Icono mipmap oficial, bienvenida/audio y botones
BASTA/Salir conservados. Sin commits, pushes, despliegues ni Firebase de produccion.

Imagenes y memoria:
- Inventario de cabeceras PNG de TODAS las carpetas drawable: 104 imagenes. Quedaban
  60 PNG sin calificador de densidad; siete de 2048x2048 (16 MiB cada una antes de
  posible escalado), letras e iconos 1254x1254 (unos 6 MiB). Partida carga letra,
  marco, corona y comodines mediante setImageResource/XML. Esto es un coste real
  de recursos, NO un diagnostico confirmado del ultimo cierre de Diana sin Logcat.
- docs/tools/optimizar-recursos-responsive.ps1 procesa secuencialmente SOLO PNG de
  drawable, limita lados segun uso (letras/marco/duelos384, iconos256, fondos/cuaderno
  768) y conserva transparencia/aspecto. Los nombres R.drawable no cambian.
  Originales preservados en docs/referencias-ui/originales-responsive; optimizados
  en drawable-nodpi para evitar el multiplicador de densidad. No toca mipmaps ni
  vuelve a procesar las imagenes optimizadas de M8/M10. Sin Python ni herramientas
  instaladas. CSV con CADA archivo/dimension/peso: docs/RECURSOS_OPTIMIZADOS.csv.
- Las 60 imagenes pasan de 383682672 a 36055552 bytes ARGB teoricos (365.91 a
  34.39 MiB) y de 76578797 a 9877309 bytes de archivos (73.03 a 9.42 MiB).
  El conjunto de 104 PNG drawable queda en 52.54 MiB de pixeles teoricos: no es
  memoria simultanea ni una medida de heap de la app.
- Se conserva PNG con alfa: no hacia falta instalar un conversor WebP. La reduccion
  de resolucion y nodpi corrigen memoria decodificada, no solo compresion del APK.
- BastaAplication reduce cache/pool de Glide con MemoryCategory.LOW solo cuando
  isLowRamDevice o memoryClass<=128. Sin largeHeap ni System.gc en la app.
  JugadorPartida/JugadorSala/Validacion limitan avatares192px y AvatarAdapter320px.
  ValidacionAdapter libera su solicitud de avatar al reciclar las filas.
- Revision focalizada de cargas: las demas fotos de perfil/historial/duelos usan
  Glide hacia ImageView, sin decodeFile/MediaStore.getBitmap/submit original ni
  decodificacion manual de fotos en Java. Los tres Lottie JSON no contienen imagenes
  base64 incrustadas. No se cambiaron ni se recortaron animaciones.
- Prueba real BitmapFactory en emulador API37, manteniendo los siete bitmaps de la
  escena a la vez: fondo, logo, letra M, marco, corona, comodines y cronometro suman
  7924224 bytes (7.56 MiB). Ver memoria-escena.txt. NO equivale al heap completo ni
  demuestra que la tablet API25 no pueda agotar memoria por otra causa.

Responsive y renderizado:
- ListaContenido.java reemplaza SOLO los dos RecyclerView interiores de revision
  y crear sala, dentro de sus ScrollView. Listas acotadas (8 respuestas/9 categorias)
  se miden completas con LinearLayout, reutilizando exactamente sus adapters/filas
  y callbacks. Unico propietario del scroll vertical; sin reciclaje/medicion diferida
  interior. Observa cambios de datos/rangos y vuelve a enlazar en attach, sin requerir
  gesto para dibujar ni alterar estados de Firebase. IDs rvRespuestasTarjeta y
  rvCategorias se conservan. No se cambio rvCategoriasJuego ni su logica IME/foco.
- TarjetasValidacionAdapter usa el contenedor nuevo; Crear_salaFragment elige una
  columna debajo de 360dp o fuente>1.25, dos donde caben. Se elimino ancho interno
  rigido150dp en item_categoria; iconos32dp y nombres15sp caben sin elipsis en las
  mediciones de 360dp con fuente1.2. Todas las nueve categorias existen y son
  pulsables desde el primer layout. Boton Crear accesible al bajar hasta el final.
- Rondas/Jugadores mantienen dos tarjetas horizontales y botones tactiles48dp.
  Centro adaptable con peso, minimo24dp y margen4dp; padding interno4dp. Comprobado
  contador27 y limite8 sin salto de linea. Se conserva la excepcion extremadamente
  estrecha/fuente grande que ya existia; no se modificaron limites/selector/letras.
- item_jugador_partida ahora horizontal/compacto (avatar40dp, corona26dp, nombre14sp,
  estado11sp), mantiene todos los IDs y la lista horizontal. Menos alto superior,
  sin eliminar identidad/estado. Avatares se cargan a tamaño limitado.
- Partida: dimensiones base logo48/reloj88dp, h700 logo72/reloj108, sw600 logo88/
  reloj120. Encabezado de hoja compacto, instruccion corta Empieza con [letra],
  cronometro decorativo36dp y titulo Tiempo para evitar superposicion/corte. Texto
  y control del reloj no cambian su plazo. Boton existente Comodines integrado con
  borde azul/fondo crema, tactil50dp. Se compacta espaciado de item_categoria_partida
  (icono36, padding8, titulo18) y se oculta el redundante Tu respuesta; EditText
  sigue de50dp y conserva IDs, textos, foco y scroll interno. BASTA y Salir intactos.
- Una prueba detecto que la primera iteracion dejaba la hoja en solo91dp a 320x568;
  se corrigio el encabezado, no se rebajo la asercion. Medicion final: hoja134dp a
  320x568 y 360x568, 394dp a 600x900, densidad160/fuente1.0. Ya cabe un campo entero
  en la prueba pequena; no se obliga a mostrar tres categorias en todos los equipos.
- Ruleta: titulo sin singleLine/ellipsis, wrap_content, 20sp solo en su cuenta.
  Captura confirma titulo completo y 5 debajo. Se conservan cuenta/animacion/evento.
- Menu de comodines: UNICO cambio es icono vacio ic_carrito_oscuro (vector oscuro),
  sin tocar tarjetas, estilos, rarezas, botones ni disponibilidad.

Validacion ejecutada:
- PASA testDebugUnitTest configuracion normal: 137 pruebas, 0 fallos. Tres nuevas
  ResponsiveRecursosTest: dimensiones/coste teorico/densidad de todos los PNG,
  ausencia de Recycler anidado en esas listas y ausencia de ancho150dp rigido.
- PASAN cuatro ResponsiveLayoutsTest instrumentadas en el emulador YA abierto
  emulator-5554, API37. No se inicio otro emulador ni se modifico Basta normal alli.
  Contextos medidos/dibujados con widgets Android reales a 320/360x568 y 600x900,
  fuentes1.0/1.2 y titulo ruleta1.3. Se verifican las ocho respuestas de Pelicula y
  Cancion desde primer layout, desplazamiento a ultima fila, click Votar, cambios
  a puntos de las ocho, nueve categorias con seleccion y boton Crear alcanzable,
  nombres sin elipsis, 27/8, scroll de escritura hasta ultima categoria y titulo
  completo. La prueba inicial fallo por hoja91dp y se corrigio el layout.
- Capturas revisadas directamente: docs/referencias-ui/verificacion-responsive/
  (28 PNG con datos ficticios + cuatro mediciones TXT). Incluye inicio/final/puntos
  de revision, crear320/360/600, partida pequena/tablet y ruleta. Resultado textual
  docs/RESULTADOS_LAYOUTS_RESPONSIVE.txt. Son renders de vistas en emulador, no
  prueba fisica de gestos/teclado ni una sesion multijugador completa.
- Aislamiento: propiedad Gradle -PlayoutsAislados usa applicationId .responsive,
  sin Google Services aplicado, manifiesto debug que QUITA INTERNET y LayoutsRunner
  que inicia Application simple (no BastaAplication/listeners/auth/audio). Se comprobo
  package y ausencia de permiso INTERNET antes de instalar. Solo se instalaron esos
  dos APK de prueba; el paquete normal no fue sustituido. Prueba termina y se
  detuvo solo com.example.basta_game.responsive, sin borrar datos. No hubo Firebase.
  Sin esa propiedad el applicationId y runner de Basta siguen siendo los originales;
  ResponsiveLayoutsTest se omite expresamente si no es perfil aislado.
- No se repitio RTDB Emulator: ningun algoritmo/transaccion de sincronizacion fue
  modificado en esta tarea. Los pendientes de reglas/servidor de M11 se conservan.
- Gradle offline/no-daemon/no-parallel/max-workers=1, heap768MiB y metaspace256,
  JVM de tests256MiB, tareas en secuencia. Se detecto emulador residente y solo unos
  2.3GiB virtuales libres antes de compilar; no se uso Python, Graphify ni procesos
  de imagenes en paralelo. No hubo OOM/MemoryError en las verificaciones.
- Repetir el script sin PNG nuevos no cambia archivos ni el informe: comprobado
  hash del CSV. Compilacion normal final sin -PlayoutsAislados, package original.

Archivos exactos de produccion: BastaAplication.java; utils/ListaContenido.java;
Adapters/TarjetasValidacionAdapter, ValidacionAdapter, CategoriaAdapter (sin cambios
  de logica; se reutiliza), AvatarAdapter, JugadorPartidaAdapter, JugadorSalaAdapter;
Fragments/Crear_salaFragment y PartidaFragment (solo presentacion); layouts
item_tarjeta_revision, fragment_crear_sala, item_categoria, item_categoria_partida,
fragment_partida, item_jugador_partida, dialogo_resultado_flujo y
 dialogo_comodines_partida; partida_dimens base/h700/nuevo sw600;
 ic_carrito_oscuro.xml y los 60 PNG listados en el CSV.
Soporte de pruebas: app/build.gradle (perfil optativo), src/layoutsAislados/
AndroidManifest.xml, LayoutsRunner, ResponsiveLayoutsTest, ExampleInstrumentedTest
(acepta tambien paquete aislado), ResponsiveRecursosTest, script/CSV y capturas.

Pendientes: probar el APK NORMAL en la SM-T350 API25 durante lobby -> partida,
varias rondas, duelos y navegacion repetida; registrar heap/Logcat si vuelve a
cerrar. No se puede certificar ausencia de OOM en esa tablet a partir de estos
renders API37. Tambien pendiente teclado/foco/gestos en ventanas reales y fuentes
muy aumentadas en los dispositivos fisicos. Las garantias y limites de Firebase
permanecen exactamente como documentados en M11; esta mision no los cambia.

APK NORMAL final: assembleDebug BUILD SUCCESSFUL (25s), app/build/outputs/apk/debug/app-debug.apk, 27785461 bytes, generado 2026-11-15 21:44:50, applicationId=com.example.basta_game. Sin commits/pushes.


Actualizacion final API25 (misma tarea): se confirmo en el SDK local
platforms/android-36.1/data/api-versions.xml que paddingHorizontal/paddingVertical y
layout_marginHorizontal/layout_marginVertical comienzan en API26. Se agregaron
paddingStart/End/Top/Bottom y marginStart/End/Top/Bottom equivalentes donde faltaban
en ocho layouts afectados: item_jugador_partida, fragment_partida, item_categoria,
item_categoria_partida, item_tarjeta_revision, fragment_crear_sala, item_respuestas
y dialogo_resultado_flujo. Se conservan valores individuales que ya existian.
En Android reciente son los mismos valores; API25 deja de ignorar esos espaciados.
El menu de comodines no recibio estos cambios generales: solo su icono vacio.

Validacion NORMAL definitiva: testDebugUnitTest BUILD SUCCESSFUL (27s), 138 pruebas,
0 fallos. Se sumo una cuarta prueba de recursos que verifica los fallbacks API25
para esas ocho pantallas. Las cuatro instrumentadas y capturas anteriores se
realizaron en API37: no sustituyen prueba fisica del comportamiento API25.

Correccion de listado: CategoriaAdapter.java se REUTILIZA sin editar su logica ni
archivo; sus tarjetas item_categoria.xml y contenedor son los que cambiaron.
La nueva prueba ExampleInstrumentedTest acepta el paquete normal o el aislado,
y no se eliminaron pruebas para hacer pasar resultados. El perfil de pruebas
sigue opcional; no cambiar el package ni quitar Internet al generar APK para Diana.

CIERRE definitivo responsive/memoria: assembleDebug BUILD SUCCESSFUL (20s). APK NORMAL app/build/outputs/apk/debug/app-debug.apk, 27790163 bytes, 2026-11-15 21:53:52, package=com.example.basta_game. Pruebas unitarias finales 138, fallos 0. Detenido sin commits ni pushes.


## MISIÓN 12 — Correcciones quirúrgicas y anexo de conexión (2026-11-15)

Se conservan las misiones anteriores. Esta sección sustituye únicamente la decisión
anterior de hacer scroll sobre toda la tarjeta de revisión: ahora el diálogo es fijo
y SOLO su RecyclerView de respuestas tiene desplazamiento vertical.

Cambios locales:
- RelojRevision: cuando llegaron todas las respuestas y ninguna es cuestionable,
  publica directamente el estado `resultado` con 3000 ms. Incluye repetidas,
  estructuralmente inválidas y clasificaciones originales de cero. Las totalmente
  vacías conservan 2000 ms; si faltan datos o existe una candidata, conserva el flujo
  de revisión/selección manual. No se cambian las puntuaciones ni los empates.
- PlazoRonda y PartidaFragment: BASTA usa el inicio compartido de RESPUESTAS,
  separado de la preparación. Rechaza antes de 30 s y cuando restan <=10 s, tanto
  en interfaz como en transacción. Una solicitud pendiente bloquea dobles toques;
  al aceptarla mantiene el cierre compartido existente de diez segundos. Los
  estados BASTA/CONGELADO/FINALIZANDO ajustan texto sin ampliar los botones.
- item_tarjeta_revision y TarjetasValidacionAdapter: RecyclerView con altura 0dp,
  peso 1 y LinearLayoutManager, dentro de tarjeta acotada; sin ScrollView padre.
  Encabezado y pie fijos, sin animador de cambios que deje filas visualmente vacías.
  Se conserva ViewPager2 y la consulta horizontal posterior de categorías.
- item_jugador_partida: tarjetas verticales compactas con avatar, nombre, estado,
  corona y check; se conserva el carrusel de participantes. Menos márgenes en
  fragment_partida para priorizar la hoja. BASTA/Salir mantienen tamaño y posición;
  no se cambia CategoriaPartidaAdapter ni su lógica de scroll/IME. Las etiquetas
  superiores y el texto del reloj se adaptan a su espacio con AppCompat/API25.
- Crear sala: las pruebas visuales detectaron el número de rondas en dos líneas
  a 320dp. Menor padding lateral en ambos contadores y número de una sola línea,
  conservando botones, dos columnas, nueve categorías, 27 rondas y ocho jugadores.
- Tema intencionalmente claro y Force Dark desactivado desde API29, con texto
  secundario explícito en revisión: evita herencia DayNight diferente por dispositivo.
- Ruleta: ancho efectivo del diálogo con margen de 12dp por lado y título adaptable,
  sin modificar selección, cinco segundos ni efectos. EfectoRuleta registra mensajes
  con nombres y puntajes reales antes/después y variación; Puntos Saltarines ya
  registraba origen, destino y cantidad reales y conserva su comportamiento.
- CargaBoton: indicador nativo de 18dp en login, registro, crear/unirse, compras y
  Reintentar. Bloqueo de pulsaciones, plazo UI de 30s (8s para Reintentar), limpieza
  al desmontar la vista y token por operación para descartar callbacks anteriores.
  Las compras conservan su transacción y bloqueo financiero hasta confirmación;
  no se da por cancelada una operación monetaria por un timeout visual.

Anexo conexión:
- ConexionJuego conserva una sola fuente global de red y transporte RTDB. Android
  verifica red INTERNET+VALIDATED; `.info/connected` no se presenta como prueba de
  ausencia de internet. onPause no cambia la conectividad; al volver se descartan
  comprobaciones y ventanas antiguas, sin retener la Activity.
- Fuera de sala, con red pero RTDB recuperándose: aviso discreto `Conectando…`
  durante hasta 20s; no mensaje de recuperación de partida. Sin red o servicio
  persistentemente indisponible, diálogo con causa diferenciada.
- Reintentar muestra spinner y `Comprobando…`, usa goOnline, comprobación de
  transporte y renovación real del token Auth si existe usuario; máximo ocho
  segundos, sin sondeo repetitivo y descartando respuestas de intentos obsoletos.
- En sala, las acciones esperan además lectura actual de pertenencia/reserva/fase.
  SalaActivity confirma la sincronización tras recuperar el estado y rechaza
  callbacks de conexiones anteriores; Reintentar también repite esa recuperación.
  Los errores de acceso a la sala se muestran como tales. Se conserva la salida
  local sin internet, la presencia, el período de gracia y la protección de expulsión.

Archivos funcionales afectados:
`utils/{PlazoRonda,RelojRevision,ConexionJuego,EstadoConexion,CargaBoton,EfectoRuleta}.java`,
`Fragments/{PartidaFragment,Crear_salaFragment,Unirse_salaFragment}.java`,
`{LoginActivity,RegistrarActivity,SalaActivity}.java`,
`Adapters/{TarjetasValidacionAdapter,ComodinTiendaAdapter}.java`.
Recursos: `layout/{item_tarjeta_revision,item_jugador_partida,fragment_partida,
fragment_crear_sala,item_comodin_tienda}.xml`, temas values/values-night/values-v29,
`values/carga_boton.xml`. Launcher, menú aprobado y lógica de otros comodines intactos.

Verificación y límites:
- Graphify se consultó realmente: query con nombres de RelojRevision, PlazoRonda,
  PartidaFragment y adaptador; path PartidaFragment/PlazoRonda. El mapa inicial era
  del 14/11 y se actualizó con `graphify update . --no-cluster` (AST, sin API/LLM).
  Pasó de 1810/5033 a 2128/5983 nodos/aristas antes de estas correcciones.
  El extractor omite XML y avisa de sintaxis Gradle DSL; los layouts se verificaron
  directamente y con medición/renderizado Android, no suponiendo que el grafo los cubre.
- RTDB Emulator: 25 pruebas, cero fallos, `demo-basta-concurrency`, solo database,
  apagado al terminar. Nueva carrera de categoría automática: un solo inicio de
  presentación de 3s y un solo cierre. BASTA concurrente actualizado a instante
  elegible; siguen pasando votos/cierres, compras, recompensas, cupos y expulsión.
  JVM emulador 512MB, Gradle 1280MB, un trabajador; sin procesos pesados paralelos.
- Instrumentadas: perfil `layoutsAislados`, applicationId separado y SIN INTERNET,
  sobre el único emulador ya abierto (API37). Pruebas de revisión 2/5/8 respuestas,
  320/600dp y fuentes 1.0/1.3, fases revisión/voto/puntos, última tarjeta, encabezado
  fijo y dos tarjetas sin scroll sobrante cuando caben. También crear sala 320/360/600,
  partida estrecha con fuente 1.3, tres textos BASTA, ruleta y operaciones de carga.
  Las primeras pasadas detectaron fallos reales de espacio/contadores y se corrigieron,
  sin quitar las aserciones. Evidencia en `docs/referencias-ui/verificacion-m12/`.
- Las capturas usan datos sintéticos y Canvas de vistas infladas: no son una partida
  real, ni una prueba física de IME, gestos completos, red del ZTE o heap de API25.
  Pendientes: teclado abierto en teléfono, escalas extremas, OOM real de Diana,
  ida/vuelta a otras apps, Wi-Fi/datos apagados/restaurados, errores Auth/permisos
  y recreación de Activity con RTDB real o laboratorio de red aislado.
- EstadoConexionTest verifica la política de reconexión y el bloqueo hasta recuperar
  la sala; la instrumentada comprueba tokens de carga/dobles pulsaciones. No se
  presentan esas pruebas como simulación física completa de conectividad Android.
- Los SDK remotos no se cancelan mediante el spinner. Un timeout no certifica que
  una escritura no ocurrió; se descarta navegación tardía y se conservan las
  transacciones existentes. La aplicación/seguridad de plazos frente a clientes
  manipulados sigue dependiendo de reglas/servidor, no verificados en producción.
- No se añaden campos RTDB, no se tocan reglas/datos desplegados, no hay commits/pushes.

Los resultados definitivos de unitarias/compilación y APK se añaden a continuación.

CIERRE M12: testDebugUnitTest y assembleDebug NORMALES: BUILD SUCCESSFUL (45s).
Unitarias: 147, fallos 0, errores 0. RTDB Emulator: 25/25.
Instrumentadas finales: OK (5 tests), API37, perfil separado SIN INTERNET.
Se inspeccionaron capturas reales del renderizado sintético de 320dp/fuente 1.3:
BASTA/CONGELADO/FINALIZANDO completos, etiquetas de sala/ronda/estado sin cortes,
reloj y hoja legibles, números 27/8 visibles en Crear sala. Revisión 2/5/8 con
encabezado fijo y acceso a la última respuesta; ruleta completa a fuente 1.3.
AppCompatTextView explícito evita que el autoajuste dependa del inflater/de la API.
No se declara validación física del teclado, OOM API25 ni ciclos reales de red.
Pruebas tocadas: PlazoRondaTest, CategoriaAutomaticaTest, EstadoConexionTest,
LayoutsPartidaTest, ResponsiveRecursosTest, ResponsiveLayoutsTest y
ConcurrenciaEmulatorTest. Graphify final: 2188 nodos, 6082 aristas.
APK FINAL NORMAL: app/build/outputs/apk/debug/app-debug.apk
Package: com.example.basta_game; bytes: 27808389; fecha local: 2026-11-15 23:42:42.
SHA256: D252905A917BC53B80AA335CA19F036955029A3B2B789D1845AF0E5D832995BA
Sin commits, pushes, despliegues ni acceso a datos de producción. Se detiene la misión.

## MISIÓN 13 — Conexión, presencia y ajustes puntuales (2026-11-16)

Causas comprobadas: EstadoConexion.bloquear devolvía true por enSala/error/20s
incluso con red; ConexionJuego invalidaba salaLista en cada onResume. La salida
local solo ejecutaba irHome, cerraba la sesión, pero no retiraba la participación.
No se encontró goOffline en el código de producción; solo goOnline en Reintentar.
No se conoce la causa de la demora de transporte del ZTE sin trazas del dispositivo.

Correcciones:
- El diálogo global solo corresponde a ausencia de red Android con capacidad
  INTERNET. No se exige VALIDATED (no todos los teléfonos validan igual la red).
  Una red disponible NO prueba conexión funcional RTDB: las acciones siguen
  verificando transporte, reloj y recuperación de sala por separado.
- No se bloquea la navegación por consultas lentas, por estar en sala o esperar
  20 segundos. Con red y servicio pendiente, banner discreto pulsable para comprobar;
  spinner del botón original para operaciones. Sin red: «¡Sin conexión a internet!»
  y «Necesitas conectarte para continuar jugando». Sin temporizador de bloqueo.
- Reintentar conserva comprobación asíncrona con límite de ocho segundos, token
  de intento y recuperación de sala, sin ciclos de goOffline/goOnline. onResume
  ya no invalida una sala sincronizada; callbacks anteriores se descartan.
- Salir localmente con transporte disponible utiliza el abandono transaccional
  existente. Sin transporte guarda un ticket en SharedPreferences antes de salir
  inmediatamente; se reconcilia al recuperar RTDB o volver a una Activity con
  transporte disponible. Se comprueban UID autenticado, sala, idPartida y sesión.
  TransferenciaHost.abandonarSesion exige que exista la sesión antigua y que no
  haya otras sesiones; no retira una participación nueva ni otra partida. Tras
  respuesta sin error se descarta el ticket, incluso si quedó obsoleto. Un error
  de reglas conserva el ticket. No hay campos nuevos de Firebase.
- 90 segundos permanecen intactos. cerrarSesionActual/onDisconnect registran la
  ausencia y los clientes conectados ejecutan la expiración/reconciliación existente.
  Sin ningún cliente conectado no existe ejecución programada: garantizar limpieza
  con todas las apps cerradas necesita función/servicio de servidor con comprobación
  transaccional de reserva/sesiones/partida. No implementado ni desplegado.
- CargaBoton usa overlay foreground nativo de 18dp, centrado verticalmente y con
  margen interior de 12dp: no desplaza el texto ni cambia el ancho; restaura el
  foreground original y detiene animación. Grosor/contraste final en API25 pendiente.
- Tarjetas de jugadores 102dp (antes 90), avatar igual de compacto, corona 16x14dp
  arriba a la derecha. Cápsula azul Comodines de 112x48dp, sin cambiar su acción.
- Unirse: ScrollView con fillViewport, contenido wrap_content y decoración más
  pequeña; controles no dependen de que todo quepa en la altura inicial. Se conserva
  aplicación de insets de HomeActivity. Validación con IME real del ZTE pendiente.
- Crear sala: padding y separación algo menores, sin cambiar categorías, scroll ni
  distribución horizontal Rondas/Jugadores. Tienda: producto de 260dp por debajo
  de 360dp, 280dp desde 360; menor separación antes de precio/compra, sin omitir datos.
- CoordinadorRevision no relanza setCurrentItem mientras ViewPager2 está desplazándose;
  conserva smoothScroll=true, la entrada horizontal y los plazos compartidos.

Archivos: utils/ConexionJuego, EstadoConexion, CargaBoton, SalidasPendientes (nuevo),
TransferenciaHost, CoordinadorRevision; SalaActivity; layouts item_jugador_partida,
fragment_partida, fragment_unirse_sala, fragment_crear_sala, item_comodin_tienda;
values/carga_boton.xml y tienda_dimens.xml (base y w360dp). Pruebas EstadoConexionTest,
SalidaPendienteTest, ResponsiveLayoutsTest y ConcurrenciaEmulatorTest.

Admisión comprobada en operación transaccional real del emulador:
A) Lobby con 5/5: sexto rechazado.
B) Partida iniciada con 5: sexto rechazado.
C) Partida iniciada con 4/5: usuario NUEVO rechazado.
D) Miembro válido: reentrada aceptada sin aumentar participantes.
También pasa la carrera existente por la última plaza. IngresoSala valida todo dentro
 de la transacción, no solo en interfaz. Las reglas desplegadas NO están disponibles
ni verificadas: esto no demuestra protección contra un cliente malicioso que escriba
por otra ruta. Requiere revisar reglas reales antes de declarar esa garantía.

Verificación: 26 pruebas RTDB Emulator, cero fallos, demo-basta-concurrency, apagado
al terminar. Casos A-D agregados; el caso D usa miembro existente conectado, y las
pruebas previas de reserva cubren reconexión/gracia. No prueba física de conectividad.
Cinco instrumentadas pasan en perfil aislado SIN INTERNET, en el único emulador ya
abierto: revisión, Crear sala, espacio de partida a 320/360/600dp, cargas y textos.
Capturas copiadas a docs/referencias-ui/verificacion-m13; se inspeccionó partida 320dp
con fuente 1.3: corona pequeña, hoja y botones conservados. No prueba visual nueva
completa de Unirse con IME, compra estrecha, tienda ni transición animada real.
Graphify consultado y actualizado solo AST: 2208 nodos, 6124 aristas; XML revisado
 directamente. Sin Python, sin nuevos emuladores Android, sin cambios en producción,
sin commits/pushes. Gradle un trabajador, heap Android 768MB.

Pendiente físico: Wi-Fi/datos y regreso del segundo plano en ZTE, demora real SDK,
reentrada tras varias fases, salida offline y expiración observadas desde otro teléfono,
recreación de Activity, IME, contraste del spinner y descubrimiento de tienda. Los
unitarios del ticket no prueban su persistencia real tras matar el proceso. Seguridad
servidor y limpieza sin clientes siguen pendientes. No se promete garantía física
ni seguridad de producción por haber compilado.

CIERRE M13: testDebugUnitTest + assembleDebug normales BUILD SUCCESSFUL (49s). Unitarias 148, fallos/errores 0. APK app/build/outputs/apk/debug/app-debug.apk, 27813273 bytes, 11/16/2026 00:56:27, package com.example.basta_game. Sin commits ni pushes.

## EMERGENCIA — Regresión Login/Registro (2026-11-16)

Causa comprobada por git diff y ejecución: en LoginActivity.onCreate/onClick y
RegistrarActivity.onCreate/onClick se añadió `if(!ConexionJuego.disponible() ||
!CargaBoton.iniciar(...))return`. Esto impedía llamar a Firebase Authentication
cuando RTDB/reloj/sala no estaban listos, sin mensaje. Auth no requiere esos estados.
Los callbacks también descartaban éxito si el token del spinner había vencido.

Corrección exclusiva de autenticación: quitar condición ConexionJuego de ambos
botones; conservar bloqueo de pulsaciones y spinner existentes; terminar carga al
responder, sin ignorar resultado Auth/perfil solo porque expiró el indicador.
Se conserva protección de Activity destruida, filtro institucional y demás reglas.
Registro muestra el error real recibido del SDK y del guardado del perfil; Login
incluye fallback de error para excepciones antes ignoradas. AuthFlujo registra solo
inicio/final y booleano de éxito; no correo, contraseña, tokens ni credenciales.
No se modificaron ConexionJuego, EstadoConexion, CargaBoton ni pantallas de juego.

Verificación REAL previa al APK final: AuthFlujoTest instrumentado con Activities
reales y pulsaciones Espresso (no solo performClick). Proyecto demo-basta-auth,
Auth Emulator 9099 y RTDB Emulator 9000. Paquete separado com.example.basta_game.authtest;
Application de laboratorio sin los listeners globales. FirebaseOptions ficticias y
useEmulator se configuran únicamente en AuthRunner de androidTest, nunca en main.
Se comprobó ConexionJuego.disponible()==false desde el inicio, que habría bloqueado
ambos botones antes de la corrección.

Un recorrido instrumentado, OK (1 test), 11.562s, que comprueba:
- Registro institucional correcto y perfil persistido (lectura REST independiente
  del RTDB Emulator local con autorización de laboratorio).
- Solicitud de navegación a Home tras confirmar el perfil; botón habilitado.
- Contraseña incorrecta: sin usuario autenticado ni navegación, botón liberado.
- Regreso real a Home del sistema y retorno a Login mediante REORDER_TO_FRONT.
- Reintento con contraseña correcta: Auth devuelve usuario y se solicita Home.
- Durante esos logins el SDK RTDB local está explícitamente goOffline; Auth continúa.
- Registro duplicado: error Auth, sin navegación y botón nuevamente habilitado.
La navegación se comprueba interceptando el Intent a Home con ActivityMonitor;
no se ejecuta ni se modifica HomeActivity ni sus pantallas durante estas pruebas.
Evidencia: docs/verificacion-auth/{auth-resultados.txt,logcat-flujo.txt}.

Incidencia del harness documentada: al añadir una lectura adicional del perfil por
SDK get(), y después por listener, hubo timeout. La prueba mantuvo la comprobación
de datos usando REST independiente, que sí confirmó el perfil en el servidor local.
No se afirma haber explicado ese timeout de lectura SDK ni que fuera un fallo de
internet; queda pendiente si se investiga el harness RTDB. Las operaciones Auth y
las confirmaciones de escritura/navegación sí quedaron verificadas.

Archivos modificados en esta emergencia: LoginActivity.java, RegistrarActivity.java,
app/build.gradle (perfil opcional authAislado, sin plugin Google Services ni cambio
al paquete normal), androidTest/AuthRunner.java y AuthFlujoTest.java; configuración
local emulator-tests/firebase.auth.json, auth.test.rules.json, auth-android.cmd,
auth-resultados.txt y esta documentación. Las reglas son SOLO del emulador y no
representan las desplegadas. No se crean cuentas en producción ni se usan secretos.
No Graphify, auditoría general, commits, pushes ni cambios a las mecánicas del juego.

Límites: prueba API37 en el único emulador Android ya abierto; no validación física
ZTE/API25, ni login real de producción. Se prueba el Intent a Home, no el renderizado
posterior de Home. El SDK Auth puede seguir pendiente si el servicio no responde;
el spinner conserva su límite UI de 30s y ya no descarta silenciosamente un éxito
tardío. Las credenciales sintéticas del test existen únicamente en emuladores.

CIERRE EMERGENCIA: testDebugUnitTest --tests '*CorreoInstitucionalTest' + assembleDebug NORMAL: BUILD SUCCESSFUL (28s). Filtro institucional: 3 pruebas, 0 fallos. APK app/build/outputs/apk/debug/app-debug.apk, 27813273 bytes, 11/16/2026 01:28:09, package com.example.basta_game. Auth Emulator/RTDB detenidos tras prueba exitosa. No commits ni pushes.

## MISIÓN 14 — Tarjetas de tienda y responsive (2026-11-16)

ESTADO: implementación local realizada; validación visual final PARCIAL por
inestabilidad del AVD. No presentar esta misión como responsive completamente
validado ni afirmar que se repitió Auth Emulator en esta sesión.

Referencia examinada: docs/referencias-ui/Captura de pantalla 2026-11-16 012544.png.
Se siguió su distribución, conservando familias tipográficas, colores de rareza,
iconos, bordes y listeners existentes.

Cambios:
- item_comodin_tienda.xml: pill de tipo compacta; fila icono/nombre/estadísticas;
  descripción de ancho completo sin límite artificial de líneas; inventario y
  precio a la izquierda, COMPRAR a la derecha. Se conservaron IDs y tipos usados
  por el adaptador. El botón conserva el verde y carrito; ancho 116dp y texto
  AppCompat autoajustable 10–12sp para evitar recorte con fuente ampliada.
- ComodinTiendaAdapter.java: únicamente cálculo de ancho en onCreateViewHolder;
  respeta ancho_producto y reserva espacio de carrusel. Sin cambios en bindings
  de inventario, rarezas, precios, callbacks, confirmaciones o transacciones.
- item_carril_tienda.xml: menor separación entre secciones y sin minHeight de
  280dp que agregaba espacio vacío. Se conservan ambos RecyclerViews y scrolls.
- fragment_crear_sala.xml: controles visualmente menores y márgenes reales de
  8dp a ambos lados del número; ancho de botón 40dp en pantalla estrecha y 48dp
  desde 360dp, altura táctil 48dp. Decoración 42dp y padding menores; Crear 52dp.
  Dos tarjetas horizontales, categorías, scroll y límite de 27 rondas intactos.
- fragment_unirse_sala.xml: encabezado sin solapar Atrás, tamaños locales menores,
  campo 52dp/24sp y botón 52dp; ScrollView existente conservado con fillViewport,
  espaciados proporcionados y fallbacks de padding para API25.
- Se examinó btnComodines en fragment_partida.xml. Ya es cápsula azul de 112x48dp,
  icono original y texto Comodines. Se conservó finalmente su implementación
  anterior (incluidos color del icono, tamaño y espaciado), sin cambios Java.
- Nuevos recursos locales controles_sala_dimens.xml en values y values-w360dp.
  No se cambian estilos/colores globales, fuentes compartidas, launcher ni imágenes.

Validación realizada:
- Pruebas instrumentadas ampliadas en ResponsiveLayoutsTest: tienda con nombre
  largo, descripción completa, stock/precio 12345, fuentes 1.0/1.3 y 320/360/600dp;
  carrusel con tres productos; Crear con 1/8/27 y última categoría; Unirse en una
  ventana sintética de 300dp, scroll real y botón dentro de límites visibles.
- Una iteración pasó 7/7; la inspección de captura detectó COMPRAR recortado pese
  a ello. Se corrigió y se añadieron comprobaciones de texto. Otra iteración tuvo
  6/7: tienda pedía scroll con solo dos productos que cabían en 600dp; se corrigió
  el fixture a tres productos manteniendo la aserción, no quitando la prueba.
- Crear quedó comprobado a 320/360/600dp, fuentes normal y ampliada, valores
  1/8/27 y acceso a la última categoría. Unirse permitió desplazar 187px un
  contenido de 487px en ventana de 300px y mostrar el botón completo.
- PENDIENTE: repetir la suite FINAL tras los últimos ajustes de COMPRAR/carrusel
  y del texto del campo de código. No se obtuvo captura final de esa versión.
- El AVD API37 presentó consumo elevado y luego pérdida de servicios package/
  activity y ANR de arranque tanto de la aplicación de pruebas como de procesos
  del sistema (phone). Se cerró para proteger memoria. Se probaron configuraciones
  secuenciales, nunca varios AVD simultáneos; no más reintentos pesados al persistir
  la inestabilidad. No se atribuye ese ANR a un defecto probado del layout.
  Evidencia: docs/referencias-ui/verificacion-m14/limite-avd.txt.
- Las capturas son Canvas de layouts sintéticos. Las de tienda preceden al ajuste
  final y NO prueban que desapareció el recorte. Ver README de esa carpeta.
- Unitarias focalizadas: LayoutsPartidaTest, ResponsiveRecursosTest y
  CorreoInstitucionalTest: 11 pruebas, cero fallos/errores. No se eliminaron pruebas.
- APK normal: assembleDebug BUILD SUCCESSFUL (26s), tras la pasada de unitarias
  testDebugUnitTest/assembleDebug de 45s. Un trabajador, heap Gradle 768MB.

Regresión crítica de acceso:
LoginActivity y RegistrarActivity no se editaron. SHA256 antes/después idénticos:
LoginActivity E7A5087C857E17EC8B96EF68424ED5C8C2C915F07D4FE21B7DEDB16E1BFF0E7B
RegistrarActivity B1A6234483D28EEAD9E7A276881FB45AC70D002C03775B225BBC5ED8B867D3F0
La prueba completa Auth Emulator de la emergencia sigue documentada en la sección
anterior; NO se repitió en M14 porque el AVD no pudo iniciar estable el runner.
No afirmar validación dinámica nueva de login/registro a partir de esos hashes.

Pendientes físicos: ZTE/API25, teclado real e insets de navegación en Unirse,
compras con fuente ampliada y validación final de ambos scrolls de tienda. La
ventana de 300dp simula espacio reducido; no demuestra comportamiento IME real.
No hubo cambios de Firebase, producción, autenticación, juego, commits o pushes.
No se ejecutó Graphify ni procesamiento de imágenes. Se cerró el AVD propio.

Archivos de esta misión: Adapters/ComodinTiendaAdapter.java;
res/layout/{item_comodin_tienda,item_carril_tienda,fragment_crear_sala,
fragment_unirse_sala}.xml; res/{values,values-w360dp}/controles_sala_dimens.xml;
androidTest/ResponsiveLayoutsTest.java; docs/PROJECT_STATE.md y evidencia m14.
APK normal: app/build/outputs/apk/debug/app-debug.apk, 27816444 bytes,
applicationId=com.example.basta_game. Validación visual final pendiente.

## MISIÓN 15 — Investigación de congelamiento (2026-11-16)

Informe completo: docs/diagnostico-m15/INFORME.md.
CAUSA DEMOSTRADA de conectividad: DNS del AVD daba UnknownHostException para
Google, Storage, Auth y dl.google.com mientras Windows sí completaba HTTPS.
Arranque en frío del mismo AVD con -dns-server 8.8.8.8 y sin cargar/guardar snapshot
restauró DNS y HTTPS con TLS validado. Sin Wipe Data, reinstalación ni cambios SSL.
Es una corrección de ARRANQUE TEMPORAL; no se editó config.ini ni DNS de Windows.

CONGELAMIENTO TOTAL: no se reprodujo hoy, ni antes del arreglo. Main thread estaba
en do_epoll_wait y la navegación funcionaba. Se conservan antecedentes M14 de ANR
del sistema y presión de memoria; no se afirma causa definitiva de esos ANR ni que
hayan quedado resueltos. Final: CPU app 0%, PSS ~113MiB, Java heap ~13MiB, sin ANR.

Certificados: idea.log registra diálogos próximos 08:51:32/38/39 para el mismo nombre
SSLA-Self-Signed-Certificate, TrustManager del IDE. Un rechazo se correlaciona con
PKIX en la consulta de actualizaciones GitDownloadAndInstall. No son evidencia de
firmas APK GMS ni licencias SDK rechazadas. Se desconocen las URL/huellas completas
y el motivo de que solo apareciera Reject; no se aceptó nada ni se culpó al antivirus.
Android Studio AI-262.10968.63.2622.16840690; emulator 37.2.13, plataforma-tools 37.0.1;
imagen API37 Google Play x86_64 revisión 7, extensión 22; Play Services/Store instalados.
La incidencia oficial Play Protect anterior a revisión 5 no corresponde a revisión 7.

Validación REAL: Home/Tienda/Perfil/selector avatares/Cancelar; Crear scroll y +
cambia 1 a 2; Unirse enfoca y acepta texto sin enviar código. Tras DNS se mostraron
avatares aun con warning App Check: no era prueba de bloqueo por enforcement. No se
verificaron reglas ni enforcement en consola. No se crearon salas ni compras reales.
AuthFlujoTest del laboratorio YA INSTALADO: OK (1 test), 8.863s, con Auth/RTDB Emulator
demo locales: registro/perfil, contraseña incorrecta y reintento, regreso de Home,
login con RTDB offline y registro duplicado. No se crearon cuentas reales; no se
recompiló ni reinstaló un APK. Servidores locales y app de laboratorio detenidos.

No cambios productivos en código, Firebase, seguridad, dependencias o APK; sin
commits/pushes. Graphify solo consulta enfocada, sin reconstrucción. Se compiló una
sonda Java mínima de red, fuera del APK; no se ejecutaron Gradle ni Python.
SDK Storage getDownloadUrl se ejecuta para once avatares desde Perfil oculto al
crear Home: explica ruido de logs, no un bloqueo probado. No Tasks.await/sleep UI
ni overlay global bloqueante demostrados durante esta investigación.

Pendientes: capturar stack/ANR EN EL MOMENTO si vuelve el congelamiento, comprobar
fallo Storage aislado y pruebas bajo carga sin agotar memoria; API25 no probado.
No completar automáticamente la validación visual final M14 por esta investigación.
El AVD queda activo con DNS explícito y datos conservados para continuar probando.
Logcat completo y UI privados en app/build/diagnostico-m15/ (ignorado por Git).

## MISIÓN 16 — Cierre compartido, salida offline y ajustes puntuales (2026-11-17)

Informe, lista completa de archivos, escenarios y evidencias: docs/verificacion-m16/README.md.
Se conservaron las misiones anteriores y todos los cambios preparados/no preparados.

Causa comprobada capaz de dejar 00:00: iniciarCronometro detenía el runnable tras
llamar una vez a guardarRespuestasJugador; si ConexionJuego no estaba disponible
retornaba sin activar la revisión. Un error restablecía la bandera, pero no el
recorrido. La apertura dependía del callback/timeout local de cada envío. Sin una
traza de la partida original no se atribuye exclusivamente a la red de un equipo.
Ahora CierreRespuestas confirma en transacción el reloj de revisión YA EXISTENTE,
con partida/ronda, vencimiento compartido y la espera máxima previa de cinco segundos
para respuestas. Cualquier miembro conectado puede hacerlo. PartidaFragment observa
esa confirmación persistida y recupera revisión tras retrasos/recreación, sin forzar
fases por un reloj local. No crea respuestas faltantes ni cambia puntuaciones.

Abandono: ya no espera cancelaciones/operaciones remotas para salir. Se guarda un
ticket local antes de navegar; conserva idPartida y sesiones propias tras recreación.
La prueba instrumentada detectó y permitió corregir el caso de sesión aún en registro,
que antes dejaba sin ticket. La reconciliación reconoce sesiones históricas conocidas,
protege conexiones ajenas/desconocidas y partidas nuevas; callbacks antiguos no borran
tickets nuevos. Las escrituras de entrada/estaListo son transaccionales y no recrean
un UID retirado. Registrar presencia comprueba reserva/membresía y versión de conexión;
no publica un setValue tardío de sesión activa. No se encola un timestamp de cierre
offline que prorrogue la gracia al reconectar. Nuevas listas de sesiones del ticket
son SOLO SharedPreferences/estado local: no hay campos ni reglas RTDB nuevos.

90 segundos: se conserva 90000 ms desde la desconexión registrada por RTDB. El
comparador de expiración ya funcionaba con un timestamp confirmado; no se inventa
una causa aritmética del incidente de dos minutos. El servidor puede tardar en detectar
un corte silencioso. Clientes conectados derivan presencia, transfieren host y retiran
transaccionalmente membresías vencidas; el último participante conserva el cierre
existente sin bono de supervivencia. Sin cliente ejecutor o permisos efectivos no hay
limpieza garantizada. Propuesta de trigger RTDB + Cloud Tasks/Function, coste Blaze y
necesidad adicional de lease/heartbeat para TCP vivo, documentada pero NO implementada
ni desplegada. No se promete ejecución exacta a los 90 segundos de apagar Wi-Fi.

Tienda: minLines=3; altura uniforme medida con el contenido más alto del carrusel para
ancho/fuente reales; si supera tres líneas, todas crecen, sin truncar. Controles de
compra alineados abajo y medida invalidada al reciclar categorías. Se preserva diseño.
Los avisos negativos de tienda no llamaban al reproductor; avisoTienda reutiliza el
audio original, agrupado con un sonido por ráfaga. Las compras exitosas ya tenían sonido.
Icono de Comodines pasa de 18 a 20dp; cápsula, texto y altura de 48dp intactos.

Validación FINAL:
- 159 unitarias: 0 fallos/errores.
- 34 RTDB Emulator demo-basta-concurrency: 0 fallos/errores/ignoradas. Dos clientes
  independientes, cierre concurrente, receptor tardío/listener recreado, host ausente,
  onDisconnect real, tiempo controlado 89999/90000/90001 ms, reconexión frente a limpieza,
  sesión anterior, entrada concurrente con abandono y cierre único con un participante.
- AVD API37 + Auth/RTDB locales demo-basta-auth: Mision16SesionTest OK (1 test), 10.065s:
  revisión, Activity recreada, revisión recuperada, salida offline <1500ms, isFinishing,
  intención de Home, ticket y retirada del UID al recuperar conexión. Home se intercepta
  y una pantalla local auxiliar mantiene el foreground; no es un test de Home completo.
- AuthFlujoTest OK (1 test), 7.873s: registro local, contraseña incorrecta y reintento,
  segundo plano, login con RTDB offline, duplicado y botones liberados. Sin cuentas reales.
- Mision16InterfazTest OK (3 tests), 2.839s: 320/360/600dp, fuente 100/130%, alturas iguales,
  descripción completa, COMPRAR alineado/visible, reciclado, icono/cápsula; audio original
  cargado y AudioTrack activo tras aviso, sin repetición de ráfaga. 0 ignoradas en Logcat.
  Dos capturas sintéticas 320dp/fuente130% revisadas y conservadas en verificacion-m16.
- Compilación normal final: :app:testDebugUnitTest :app:assembleDebug, BUILD SUCCESSFUL
  (38s), un trabajador, Gradle Xmx1024m/Metaspace384m, sin ejecuciones pesadas paralelas.
- APK: app/build/outputs/apk/debug/app-debug.apk, 27813397 bytes,
  applicationId=com.example.basta_game, SHA256
  886D92835AFEB82CB015D53408052EF3B8F288D1338866D7D99ADFC6B5FCA6CB.

Laboratorio: el AVD retenía instalación/runner. Arranque en frío del MISMO AVD sin
borrar datos, sin snapshots y con DNS de M15 resolvió ese bloqueo. No se desactivaron
verificaciones ni se aceptaron certificados. El runner Auth necesitó URL local fija
para evitar otra instancia RTDB no redirigida (RepoInfo mutable del SDK 22.0.1).
Se midió cachedHost=127.0.0.1:9000 al reconectar: un puente adb reverse temporal hizo
accesible ese localhost de Windows desde Android. Puente retirado al finalizar;
servidores locales y app authtest detenidos. Reglas m16.test.rules.json son únicamente
fixtures, NO reglas de producción. Fallos intermedios del harness se conservan
explicados en el informe; no se omitieron tests para pasar.

LoginActivity y RegistrarActivity sin modificar; SHA256 coinciden con M15.
Graphify: consulta enfocada y actualización incremental AST (sin LLM); parser de DSL
Gradle emitió avisos, aunque Gradle compiló. No se ejecutó Python ni se procesaron imágenes.
No hubo Firebase real, cambios de seguridad, commits o pushes.

Pendientes: teléfono+AVD simultáneos, ZTE/API25, Wi-Fi real y expiración desde marca
servidor, audio audible/modo silencio. Dos clientes SDK y una UI instrumentada no
sustituyen dos dispositivos físicos. Reglas reales y backend autónomo siguen pendientes.
No se declara multijugador infalible ni garantía remota sin clientes conectados.
Nota de cobertura M16: el filtro preexistente del diagnóstico legado conexionAnteriorNoDebeDesconectarOtraSesionActiva se mantuvo intacto; no forma parte de las 34 pruebas RTDB ni se afirma que pasó. Evalúa hooks antiguos escritos directamente en jugadores; no sustituye las pruebas nuevas de sesiones. No se eliminaron pruebas ni se añadieron exclusiones en esta misión.
