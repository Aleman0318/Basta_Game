---
name: basta-sync
description: Investiga y corrige desfases entre clientes Android, web y espectador de Basta Online; úsala cuando haya retrasos, estados distintos, esperas o eventos duplicados.
---
# Sincronización de Basta
1. Lee `AGENTS.md`, `docs/PROJECT_STATE.md` y detecta las fuentes de estado reales del proyecto.
2. Dibuja el flujo de estados de partida y listeners en Android y web, indicando rutas y archivos concretos.
3. Reproduce el fallo antes de editar; registra una secuencia de eventos con id, orden, hora y cliente.
4. Comprueba duplicados, listener suscrito varias veces, condiciones de carrera y relojes de cliente.
5. Diseña un cambio mínimo con fuente de verdad única; evita polling o lecturas por frame.
6. Agrega pruebas automáticas cuando sea viable y un guion de reproducción manual de 2 y 8 jugadores.
7. Nunca cambies las reglas del modo clásico sin autorización. Registra el resultado y pendientes en `docs/PROJECT_STATE.md`.
