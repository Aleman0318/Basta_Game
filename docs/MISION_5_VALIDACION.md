# Mision 5: validacion con dispositivos y decisiones pendientes

## Repeticion manual (2, 3 y 8 jugadores actualizados)

1. Registro: probar alumno@UES.EDU.SV y rechazar Gmail, subdominios y sufijos como
   alumno@ues.edu.sv.ejemplo.com. Comprobar notificacion personalizada y que el login
   de una cuenta existente sigue funcionando. No se verifica propiedad del correo.
2. Completar una partida y comenzar otra en la misma sala. En puntuaciones pulsar
   continuar simultaneamente, repetir y variar quien pulsa primero. Verificar una
   transicion, misma ronda/letra/idPartida y puntos anteriores conservados.
3. Con tres/ocho jugadores: todos salvo uno listos; ese jugador abandona. El host
   avanza al quedar listos los miembros restantes. Repetir abandonando el host;
   el nuevo adquiere la responsabilidad. No eliminar una desconexion temporal.
4. Denegar/interrumpir una escritura solo en el entorno de pruebas. Verificar que
   puede reintentarse al recuperarse la conexion y no aparecen dos rondas nuevas.
5. Home, otra app y regreso, tanto en lobby como jugando/puntuaciones. Repetir con
   recreacion de actividad y muerte del proceso. Comparar nombre, avatar, corona,
   puntos y fase antes/despues. No confundir proceso muerto con una mera pausa.
6. Cortar red y verificar "Reconectando", posicion conservada y expiracion a partir
   de 90 s desde el timestamp que registra Firebase; no desde tocar Home. Repetir
   con reloj local diferente. El test unitario del umbral no sustituye esta prueba.
7. Dos participantes: uno sale definitivamente durante juego, revision y espera.
   Verificar resultados finales en el restante, sin vuelta a juego por callbacks
   tardios ni premio extra. Sala nunca iniciada: debe conservar lobby.

## Tablet SM-T350 API 25: evidencia necesaria

minSdk=24 admite la instalacion en API 25; no demuestra ausencia de fallos runtime.
No se obtuvo el stack trace ni se conecto la tablet en esta mision. Lint se ejecuto;
no encontro NewApi ni errores, pero esto no identifica la causa del crash real.
El Home carga varios fragments, incluso ocultos; sin Logcat no se atribuye el fallo
a memoria, recursos, Firebase, perfil ni a una API incompatible.

Con depuracion USB habilitada y el dispositivo autorizado:

```powershell
adb devices -l
adb -s <serial-tablet> logcat -v threadtime AndroidRuntime:E '*:S'
```

Abrir Basta y reproducir el cierre. Enviar el bloque completo FATAL EXCEPTION,
Process com.example.basta_game y todos los Caused by; conservar OutOfMemoryError,
NoSuchMethodError o InflateException si aparecen. No basta la ultima linea.
Si no se captura en vivo:

```powershell
adb -s <serial-tablet> logcat -b crash -d -v threadtime
```

No borrar registros antes de obtenerlos. Adjuntar version del APK (usar el nuevo
debug para repetir), momento del fallo y si el perfil nuevo ya habia sido guardado.
No hace falta publicar tokens, correos completos ni datos de usuarios.

## Presencia por sesiones: requiere autorizacion de esquema

Propuesta comunicada, todavia NO implementada:
`salas/{idSala}/jugadores/{uid}/sesiones/{idSesion}/conectado` (boolean) y
`.../desconectadoDesde` (timestamp servidor). Cada conexion escribe y registra
onDisconnect solo sobre su propio nodo. Una conexion vieja no puede apagar otra.
La presencia agregada, elegibilidad de host y expiracion deben derivarse de todas
las sesiones, en la misma transaccion de sala; no basta agregar estos campos sin
cambiar sus consumidores. Los campos antiguos de presencia serian una proyeccion
de compatibilidad, nunca la autoridad de los clientes nuevos.

Cada reconexion necesita identidad de conexion y registro confirmado en servidor;
una actividad recreada debe reutilizar/controlar su sesion sin dejar una marca
activa huerfana. Revisar cancelacion, callbacks antiguos, limpieza de sesiones,
permisos por UID y continuidad sin coordinador cliente. Sigue pendiente decidir
si el coordinador autoritativo debe ser servidor para garantizar el avance.

Los clientes antiguos siguen escribiendo onDisconnect en el nodo compartido y
no interpretan sesiones: no garantizar salas mixtas. Requiere actualizar todos los
participantes y validar reglas reales antes de cualquier despliegue. Las reglas
reales no estan disponibles aqui. La prueba presenceLimit de Mision 4 sigue siendo
un requisito incumplido; aumentar la gracia a 90 s NO lo arregla.

## Supervivencia: requiere autorizacion y garantia de servidor

No implementada. Propuesta para evaluar, no esquema aplicado:
`usuarios/{uid}/supervivenciaAplicada/{idPartida}` (recibo independiente) y
`usuarios/{uid}/supervivenciaDiaria/{diaUTC}/puntos` (acumulado de esa categoria).
Mantener el recibo normal booleano recompensasAplicadas sin cambiar su tipo.
Una sola transaccion por usuario debe comprobar recibo, elegibilidad y acumulado,
aplicar hasta el maximo diario de 30 puntos y escribir recibo/acumulado/puntos
atomicamente. La recompensa normal puede haberse aplicado antes, por lo que su
marca no basta para identificar este bono independiente.

Antes hay que definir que contador de puntos recibe el bono, zona del dia y como
demostrar de forma autoritativa partida iniciada, una ronda valida completada y
supervivencia por abandonos definitivos. Ni el candado rondasProcesadas ni el
timestamp aportado por un telefono prueban esos requisitos. Se necesita validacion
con reglas basadas en tiempo de servidor o servicio autoritativo; un cliente puede
alterar fechas y contadores. Solicitar autorizacion para ese esquema y diseno de
seguridad antes de implementarlo. No se conceden +5 locales ni monedas nuevas.
