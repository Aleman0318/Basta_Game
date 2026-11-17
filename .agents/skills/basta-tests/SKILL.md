---
name: basta-tests
description: Comprueba que una mejora de Basta Online no rompe las partidas, la compilación Android, la web ni la sincronización; úsala antes de aceptar cambios o preparar demos.
---
# Verificación de Basta
1. Inspecciona archivos de build y scripts; no inventes nombres de tareas o comandos.
2. Ejecuta primero las pruebas más baratas (lint, tests específicos) y luego las de integración disponibles.
3. Si hay tareas Gradle, evita actualizar dependencias sin aprobación; muestra el comando y su salida relevante.
4. Revisa seguridad mínima: solo clientes autorizados modifican sala, puntuación y eventos; espectadores son lectura.
5. Propón casos de prueba para 2/4/8 jugadores, web+Android, reconexión, latencia, audio y duplicados.
6. Reporta: PASA/FALLA/NO PROBADO, causa, archivo y siguiente acción. Actualiza `docs/PROJECT_STATE.md`.
