# Basta! Online — plan de exhibición

## Meta
Una experiencia ágil y emocionante con 8 participantes actuales como base, presentador audiovisual y visualización pública. Las características son propuestas, no funcionalidades ya implementadas.

## Etapa A — estabilidad obligatoria
- Una sala de prueba con Android y web, reconexión y temporizador alineado.
- Evento de estado con id, número de secuencia/version y hora de referencia.
- Validación: ningún dispositivo salta rondas, dobla puntuación ni queda en espera indefinida.

## Etapa B — pantalla del escenario (MVP)
- URL o vista /spectate/{codigoSala} (adaptar al stack real).
- Tablero con alias, puntos, ronda, estado y efectos de partida.
- Solo lectura, sin acciones administrativas ni datos sensibles.
- Botón 'Activar sonido' para cumplir políticas de reproducción de audio web.
- Proyección por HDMI o duplicación de pantalla; preparar alternativa si falla la red.

## Etapa C — Battle Royale (modo adicional)
- Selección de modo clásico / Battle Royale antes de iniciar.
- Fases sugeridas: preparación, ronda, puntuación, riesgo/eliminación, final.
- Reglas de eliminación claras, sin expulsar por latencia o desconexiones breves.
- Prueba con 2, 4 y 8 jugadores.

## Etapa D — animador y sonidos
- Avatar animado liviano (expresiones y efectos según evento), sin necesidad de IA en vivo.
- Audios pregrabados por tipo de evento y 2–4 variantes para evitar repetición.
- Sonidos emitidos una sola vez por evento en cada pantalla según configuración.
- Volumen general, mutear, precarga y modo 'silencioso de emergencia'.

## Locuciones de ejemplo (redactadas para Basta)
- Inicio: '¡Atención, jugadores! El escenario está listo.'
- Diez segundos: '¡Diez segundos! ¡Que no tiemble ese teclado!'
- Eliminación: '¡Una silla menos en la batalla!'
- Ruleta: '¡El caos acaba de entrar a la sala!'
- Remontada: '¡Increíble! ¡La tabla se acaba de dar vuelta!'
- Final: '¡Tenemos a nuestro campeón de Basta!'

## Plan de pruebas antes de la feria
- [ ] Android + web en sala real (preferir teléfonos reales)
- [ ] 8 jugadores simultáneos y un espectador
- [ ] Reconexión, cierre de pantalla, internet lento y cambios de orientación
- [ ] Temporizador alineado y evento reproducido una vez
- [ ] Audio sin reproducción automática bloqueada después de activar sonido
- [ ] Proyección a 1080p legible a varios metros
- [ ] Plan B local si se corta la conexión
