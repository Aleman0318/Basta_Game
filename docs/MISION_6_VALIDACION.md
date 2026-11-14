# Mision 6: primera etapa local y protocolo pendiente

## Implementado

- Revision: avatar a la izquierda, nombre, palabra adaptable y accion debajo,
  indicador +10/+5/+0 con borde discontinuo, colores suaves y texto contrastado.
  Se elimina HorizontalScrollView y el ancho minimo de 330dp. La categoria tiene
  scroll vertical; palabras largas no requieren desplazamiento lateral.
- Los resultados definitivos sustituyen el boton; vacias son +0 y las correctas
  repetidas ya validadas (+5) no permiten impugnacion. Una validacion inicial local
  de 10/0 sigue siendo impugnable mientras la ventana existente lo permite; no se
  confunde con el resultado definitivo de una votacion. Se conserva la regla de
  letra inicial/minimo de letras y la deteccion existente de repeticiones.
- Apertura, voto y cierre se protegen con transacciones de sala y los identificadores
  existentes. Una escritura de un dialogo anterior no vota sobre el siguiente.
  No se cambia un voto ya confirmado del mismo UID, no se reabre palabra resuelta,
  no votan miembros retirados y el cierre comprueba host y votos actuales. Si los
  votos cambian antes de confirmar, se vuelve a evaluar la votacion vigente.
- Teclado: IME/systemBars en el contenedor edge-to-edge, ScrollView existente,
  requestRectangleOnScreen con margen de 12dp al enfocar y al cambiar IME. Se
  eliminan los desplazamientos competidores de 260/280/650ms y los margenes fijos
  de 120/135dp. No se reconstruye el adapter al abrir el teclado.
- El rebind conserva texto exacto, espacios y cursor si no cambio la respuesta.
  Un borrador se guarda en savedInstanceState y se restaura solo para la misma
  partida/ronda. No se usa como si fuera una respuesta confirmada de Firebase.
- Menu: dos grupos para comodines propios, fases visibles, tarjetas verticales,
  boton de al menos 48dp y lista acotada con scroll. Se conservan inventario,
  rarezas, iconos, descripciones y efectos. Los de resultados no estan disponibles
  si no queda otra ronda. UI y BaseComodin consultan la fase antes de descontar;
  para juego tambien se comprueba si el jugador ya envio respuestas.
- Resumen: incluye las rondas anteriores en el final y combina usos/resultados sin
  duplicar Doble o Nada. Congelador usa los registros reales de comodinesUsados;
  solo atribuye duracion al mismo idUso que tenga duracion registrada. Doble o Nada
  muestra puntosBase -> puntosFinales cuando existen; no se inventan importes de
  castigos cuyo resultado solo describe la clase de efecto. Ruleta muestra su
  efecto registrado. Los eventos temporales/pendientes tienen estilo neutral.
- Marca de la Muerte antes no escribia comodinesUsados. Las nuevas activaciones
  registran tipo/uid/nombreJugador/timestamp en ese formato ya existente, junto al
  minijuego. No se reconstruyen usos anteriores ni se afirma un resultado de duelo
  que no este guardado en el historial. No se agregan campos de esquema.
- Recreacion: recuperar cronometro ya no convierte resultados en "jugando" solo
  porque estadoPartida conserve "En juego". Se recuperan puntuaciones/resumen y
  se mantiene el caso especial del ultimo jugador. La revision no se descarta con
  Atras dejando la partida a medias. Se libera el dialogo de resumen al destruir
  la vista. El lobby sigue usando reemplazo de fragments sin pila de partida.
- Corona y notificacion de abandono conservadas; no se agrega mensaje/animacion
  de cambio de host. No se implementa premio de supervivencia.

## Todavia NO implementado: temporizador compartido y cinco oportunidades

Se pidio autorizacion explicita mediante la pregunta de esta mision y no se recibio
respuesta. No se interpreta el silencio como autorizacion. Campos propuestos:

`salas/{idSala}/revisionCategorias/ronda_{n}/turnos/{indiceCategoria}`

| Campo | Proposito |
| --- | --- |
| idPartida | Evitar que callbacks de la partida anterior gobiernen la nueva |
| numeroTurno | Identificar la oportunidad global de la categoria, hasta cinco |
| estado | Preparando, activo o resuelto |
| preparandoDesde | Timestamp Firebase para acotar la preparacion a 8s propuestos |
| listos/{uid} | Confirmacion de carga/preparacion para ese turno |
| finTurno | Un unico plazo persistido de 5s para todos los clientes |

El host debe iniciar/avanzar mediante transaccion; los clientes calculan el tiempo
restante desde ese mismo plazo y referencia de Firebase. Un cliente tardio recupera
el estado vigente sin reiniciar el reloj. Inicio/cierre deben validar la tupla
partida-ronda-categoria-turno, impedir votos fuera de plazo y excluir miembros
retirados sin confundir ausencias temporales con abandono. Falta implementar y
probar ese protocolo con limite de preparacion; no existen esos campos en el codigo
de esta entrega, ni se escribieron en el emulador o en produccion.

La implementacion actual distingue ventana para impugnar (5s LOCALES por respuesta)
del dialogo si/no/abstenerse posterior. El segundo no tiene plazo compartido y
puede esperar indefinidamente si falta un voto. Se conserva ese protocolo mientras
no haya autorizacion; no se presenta como solucion completa de temporizacion.
Tampoco se implementan las cifras grandes de un supuesto reloj sincronizado.

En la pregunta inicial se afirmo incorrectamente que el empate dejaba repetir la
palabra. La lectura completa y la correccion comunicada muestran: abstencion/empate
no consume el cupo antiguo, PERO guarda resuelta=true y conserva la validacion
previa. No concede automaticamente +5. El limite antiguo es de dos decisiones por
ronda solo si USAR_API_VALIDACION=true; actualmente es false, por lo que no se
impone ese limite. No existe aun el cupo de cinco por categoria.

Decision pendiente: contar cada votacion abierta, incluyendo empate/abstencion,
como una de las cinco oportunidades globales, o conservar su no-consumo. No son
cinco votos por usuario. Tambien concretar la relacion de la ventana de impugnacion
con el plazo del dialogo decisorio antes de codificar el nuevo protocolo.

Compatibilidad: los clientes antiguos ignorarian los turnos nuevos y seguirian
escribiendo votos/indices mediante el protocolo anterior. No garantizar salas
mixtas: actualizar todos y validar reglas reales antes de desplegar. Las reglas
desplegadas no estan disponibles; las nuevas transacciones de sala requieren
permisos compatibles en ese nodo, no solo en un hijo de votos. No se cambian reglas.
Las reglas abiertas del fixture del emulador no equivalen a seguridad desplegada.

## Limites de los cambios locales

Las comprobaciones de fase de comodines son guardas de cliente y lectura previa,
no una transaccion que abarque inventario y efecto. Subsiste la carrera entre
validar fase, descontar y aplicar si la ronda cambia en ese intervalo; el descuento
heredado de BaseComodin tampoco es atomico ante dos sesiones del mismo usuario.
No se promete seguridad contra un cliente modificado sin reglas/servidor autoritativo.
No se alteran aqui los efectos para intentar ocultar esa limitacion.

La restauracion de estado de Android depende de que Android haya guardado la
instancia. No es persistencia de borradores ante cualquier muerte abrupta del
proceso. La presencia multi-sesion y la desconexion temporal siguen pendientes de
las misiones anteriores; no se corrigen aumentando otro contador.

## Verificacion y guion manual

Las pruebas unitarias cubren estados de respuesta, vacias, +5, +10/+0 definitivos,
fase de juego y comodines, resumen sin usos ficticios, estructura adaptable y
transacciones de voto/reapertura/cierre repetidos. Las pruebas existentes de
recompensas, anfitrion y plazos de ronda se conservan.

InterfazPartidaTest contiene tres pruebas instrumentadas: ancho de fila a
240/320/360/600/800dp, sustitucion del boton por +0/+5/+10 y rebind sin perder
respuesta/cursor. APK de instrumentacion compilado; NO se ejecutaron esos casos
en un emulador Android ni dispositivo. No se instalaron APK sobre dispositivos
del usuario ni se afirma haber verificado un teclado real.

Antes de aceptar visualmente, en telefonos pequenos y tablet:

1. 320x480 y 360x640dp, tablet 600/800dp, horizontal y fuente grande. Respuestas
   largas y ocho participantes: todos los botones accesibles por scroll vertical,
   sin desplazamiento lateral, y puntos legibles en sus tres colores.
2. IME de Samsung/Gboard: primer/ultimo campo, Siguiente, regreso al anterior,
   abrir/cerrar teclado repetidamente. Texto/cursor conservados y margen sobre IME.
   Pulsar BASTA/cerrar ronda con teclado abierto sin enviar otra ronda ni perder foco
   antes del cierre. Verificar tambien navegacion por gestos y API 25 cuando sea posible.
3. Dos/tres/ocho jugadores: impugnacion simultanea, voto doble rapido, transferencia
   de host durante votacion, reentrada y resultado definitivo consistente. La espera
   por quien no vota sigue pendiente del protocolo temporal autorizado.
4. Inventario con ambos grupos: comprobar botones deshabilitados en fase incorrecta
   y ultima ronda; dejar el menu abierto mientras cambia la fase. No gastar objetos
   fuera de la fase permitida mediante el flujo normal.
5. Congelar varias veces y Doble o Nada en rondas distintas: cada uso real debe
   aparecer; solo mostrar la duracion que corresponda al id registrado. No +pts
   ficticios para Congelar. Probar resultados de Ruleta/Flujo y duelo nuevo.
6. Atras/boton/gesto, recreacion en revision/ranking/lobby, ultimo jugador y regreso
   al lobby: ninguna pantalla debe reabrir la ronda ni pagar otra recompensa.

Las pruebas de Firebase automatizadas se ejecutan solo en demo-basta-concurrency
y loopback. Los guiones de partidas con UI necesitan un entorno aislado de pruebas;
la app Android normal no se ha redirigido al emulador en esta mision.
