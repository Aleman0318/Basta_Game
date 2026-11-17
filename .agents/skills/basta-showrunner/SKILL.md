---
name: basta-showrunner
description: Diseña e implementa de manera incremental la pantalla de espectador, animador, efectos y locuciones de Basta Online para exhibiciones universitarias.
---
# Showrunner de Basta
1. Lee `AGENTS.md`, `docs/SHOW_PLAN.md` y verifica el stack Android/web real.
2. Construye primero un plan de UI con estados visibles: lobby, cuenta regresiva, ronda, puntuación, peligro/eliminación y campeón.
3. Implementa una vista de espectador de solo lectura con controles locales de volumen y activar audio.
4. La sincronización de espectador debe escuchar la misma fuente de verdad que los clientes sin acciones de escritura.
5. Presenta animador visual ligero por estados, no dependiente de IA de voz en vivo ni servicios pagos.
6. Usa frases grabadas por categoría de evento con precarga y deduplicación por evento. Respeta autoplay web.
7. Verifica legibilidad en pantalla grande y desempeño en equipos modestos; implementa fallback sin audio y sin animación.
8. Después de cada cambio, invoca pruebas pertinentes y documenta estado y pendientes.
