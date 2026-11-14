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
