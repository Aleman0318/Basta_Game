package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

/**
 * Reinicia UNICAMENTE estado efimero de comodines y minijuegos.
 * Nunca modifica los inventarios en /usuarios ni la configuracion de la sala.
 * Todos los cambios se hacen DENTRO de la transaccion de sala del llamador.
 */
public final class ReinicioComodines {
    private ReinicioComodines() { }

    /** Inicio de partida NUEVA o regreso definitivo al lobby, incluso si se abandono. */
    public static void nuevaPartida(MutableData sala) {
        if (sala == null) return;
        for (String ruta : new String[]{
                "poderesRonda",                // Escudo Dorado, PARRY
                "efectosVisuales",             // Espejo, Viento del Caos
                "comodinesPendientes",         // Doble o Nada
                "comodinesActivos",            // Congelador
                "comodinesUsados",             // Marcas y registros de uso
                "comodinesResultados",         // Cambio de Flujo, Ruleta, Doble o Nada
                "eventoCaosGlobal",            // Animacion de Cambio de Flujo
                "minijuegoActual",             // Marca de la Muerte
                "ultimoMinijuego",             // Preferencia de minijuegos de la partida anterior
                "prestamosImpuesto",           // Impuesto Revolucionario (futuro)
                "rendimientoRondas",           // Resultados de Impuesto Revolucionario
                "decretoSilencio"              // Decreto del Silencio (futuro)
        }) sala.child(ruta).setValue(null);
        // Congelador: 2 usos por PARTIDA, no por ronda.
        sala.child("usosCongeladorPartida").setValue(0);
    }

    /**
     * Avance NORMAL de la ronda N a N+1. Conserva el historial de N para
     * resumen/puntuaciones y el contador de Congelador de la partida.
     */
    public static void nuevaRonda(MutableData sala, int rondaEntrante) {
        if (sala == null || rondaEntrante <= 1) return;
        String key = "ronda_" + rondaEntrante;
        for (String ruta : new String[]{
                "poderesRonda/" + key,
                "efectosVisuales/espejo/" + key,
                "efectosVisuales/vientoCaos/" + key,
                "comodinesPendientes/" + key,
                "comodinesActivos/" + key,
                "comodinesUsados/" + key,
                "comodinesResultados/" + key,
                "eventoCaosGlobal/" + key,
                "decretoSilencio/" + key
        }) sala.child(ruta).setValue(null);
        // El duelo de la ronda anterior ya debio terminar antes de avanzar.
        // Evita que un minijuego finalizado sea tomado por el de la siguiente.
        sala.child("minijuegoActual").setValue(null);
        // No tocar prestamosImpuesto, puntajes ni resultados anteriores.
        // No tocar usosCongeladorPartida hasta la proxima partida.
    }
}
