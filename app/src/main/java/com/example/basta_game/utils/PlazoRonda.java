package com.example.basta_game.utils;

public final class PlazoRonda {
    private PlazoRonda() { }

    public static long calcularFin(long inicioRonda, int segundos, Long bastaTimestamp) {
        long fin = inicioRonda + segundos * 1000L;
        return bastaTimestamp == null ? fin : Math.min(fin, bastaTimestamp + 10000L);
    }

    public static long restante(long fin, long horaLocal, long offsetServidor) {
        return Math.max(0L, fin - (horaLocal + offsetServidor));
    }
}
