package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

/** The existing review clock is the durable acknowledgement of answer closure. */
public final class CierreRespuestas {
    public static final long ESPERA_RESPUESTAS_MS = 5000;
    private CierreRespuestas() { }

    public static long fin(MutableData sala, int ronda) {
        Object inicio = sala.child("inicioRonda").getValue();
        Object segundos = sala.child("tiempoRonda").getValue();
        if (!(inicio instanceof Number) || !(segundos instanceof Number)) return Long.MAX_VALUE;
        long respuestas = PlazoRonda.inicioRespuestas(((Number) inicio).longValue(), ronda,
                RelojRevision.numero(sala.child("preparacionInicialMs").getValue(), 0));
        Object basta = sala.child("bastaTimeStamp").getValue();
        return PlazoRonda.calcularFin(respuestas, ((Number) segundos).intValue(),
                basta instanceof Number ? ((Number) basta).longValue() : null);
    }

    public static boolean aplicar(MutableData sala, String partida, int ronda, String uid, long ahora) {
        if (!VotacionRevision.rondaVigente(sala, partida, ronda) || uid == null ||
                !PresenciaSala.conectada(sala, uid) || !sala.child("jugadores").hasChild(uid) ||
                !"jugando".equals(sala.child("fase").getValue()) ||
                sala.child(RelojRevision.ruta(ronda) + "/reloj").getValue() != null) return false;
        long fin = fin(sala, ronda);
        if (fin == Long.MAX_VALUE || ahora < fin) return false;
        if (ahora - fin < ESPERA_RESPUESTAS_MS) {
            for (MutableData jugador : sala.child("jugadores").getChildren()) {
                if (PresenciaSala.conectada(sala, jugador.getKey()) &&
                        sala.child("respuestas/ronda_" + ronda).child(jugador.getKey()).getValue() == null) return false;
            }
        }
        return RelojRevision.coordinar(sala, partida, ronda, uid, ahora);
    }
}
