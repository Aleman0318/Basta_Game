package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;

/**
 * Defensa de sala resuelta dentro de la MISMA transacción que intenta publicar
 * un ataque. Un atacante bloqueado ya consumió su propia unidad. El escudo
 * se gasta al equiparlo, pero permanece activo hasta terminar la revisión.
 */
public final class DefensaEscudoDorado {
    public static final String ID = "escudo_dorado";
    private DefensaEscudoDorado() { }

    /** Una protección de la partida anterior nunca bloquea la nueva, aunque se repita ronda 1. */
    public static boolean yaEquipadoEnPartida(String idPartidaSolicitada, Object idPartidaGuardada) {
        return idPartidaSolicitada != null && idPartidaSolicitada.equals(idPartidaGuardada);
    }

    public static boolean puedeEquipar(String fase, boolean basta, boolean yaEquipado,
                                       int rondaActual, int rondaSolicitada) {
        return "jugando".equals(fase) && !basta && !yaEquipado &&
                rondaActual > 0 && rondaActual == rondaSolicitada;
    }

    public static boolean registrarBloqueo(MutableData sala, String partida, int ronda,
                                            String uidVictima, String uidAtacante,
                                            String nombreAtacante, String nombreVictima,
                                            String idUsoAtaque, String tipo) {
        if (sala == null || partida == null || uidVictima == null || uidAtacante == null ||
                idUsoAtaque == null || uidVictima.equals(uidAtacante) ||
                !("inversion_letras".equals(tipo) || "marca_de_la_muerte".equals(tipo) ||
                        "uno_reversa".equals(tipo))) return false;
        MutableData poderes = sala.child("poderesRonda").child("ronda_" + ronda);
        if (!partida.equals(poderes.child("idPartida").getValue())) return false;
        MutableData escudo = poderes.child("escudos").child(uidVictima);
        if (!partida.equals(escudo.child("idPartida").getValue()) ||
                !Boolean.TRUE.equals(escudo.child("activo").getValue())) return false;
        // idUso funciona como clave idempotente incluso si Firebase reejecuta la transacción.
        MutableData bloqueo = poderes.child("bloqueos").child(idUsoAtaque);
        if (bloqueo.getValue() != null) return true;
        bloqueo.child("tipo").setValue(tipo);
        bloqueo.child("uidAtacante").setValue(uidAtacante);
        bloqueo.child("uidVictima").setValue(uidVictima);
        bloqueo.child("nombreAtacante").setValue(nombreAtacante == null ? "Un jugador" : nombreAtacante);
        bloqueo.child("nombreVictima").setValue(nombreVictima == null ? "Un jugador" : nombreVictima);
        bloqueo.child("timestamp").setValue(ServerValue.TIMESTAMP);
        escudo.child("intentos").child(idUsoAtaque).setValue(true);
        return true;
    }
}
