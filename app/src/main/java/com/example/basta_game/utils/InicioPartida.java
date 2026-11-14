package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;

public final class InicioPartida {
    private InicioPartida() { }

    public static boolean publicarResumen(MutableData sala, String uidHost, String idPartida, int rondaCerrada) {
        Object ronda = sala.child("rondaActual").getValue();
        Object maximo = sala.child("cantidadRondas").getValue();
        if (idPartida == null || !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                uidHost == null || !uidHost.equals(sala.child("hostUID").getValue(String.class)) ||
                !(ronda instanceof Number) || ((Number) ronda).intValue() != rondaCerrada ||
                !(maximo instanceof Number) || sala.child("jugadores").getChildrenCount() <= 1 ||
                "resultados_finales".equals(sala.child("fase").getValue(String.class))) return false;
        sala.child("resumenRondaListo/ronda_" + rondaCerrada).setValue(true);
        sala.child("fase").setValue(rondaCerrada >= ((Number) maximo).intValue()
                ? "resultados_finales" : "revision_puntuaciones");
        return true;
    }

    public static boolean aplicar(MutableData sala, String idPartida, String uidHost,
                                  Map<String, Object> cambios) {
        if (!RecompensasPartida.idValido(idPartida) || uidHost == null || sala.getValue() == null) return false;
        String estado = sala.child("estadoPartida").getValue(String.class);
        if (!"En espera".equals(estado) && !"Esperando jugadores".equals(estado)) return false;
        if (!uidHost.equals(sala.child("hostUID").getValue(String.class))) return false;
        if (!sala.child("jugadores").hasChildren()) return false;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            if (!Boolean.TRUE.equals(jugador.child("estaListo").getValue(Boolean.class))) return false;
        }
        for (Map.Entry<String, Object> cambio : cambios.entrySet()) {
            sala.child(cambio.getKey()).setValue(cambio.getValue());
        }
        sala.child("idPartida").setValue(idPartida);
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            jugador.child("puntajePartida").setValue(0);
            jugador.child("estaListo").setValue(false);
        }
        return true;
    }

    public static boolean avanzar(MutableData sala, String uidHost, String idPartida, int rondaCerrada,
                                  Map<String, Object> cambios) {
        if (sala.getValue() == null || uidHost == null ||
                idPartida == null || !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                !uidHost.equals(sala.child("hostUID").getValue(String.class)) ||
                !"revision_puntuaciones".equals(sala.child("fase").getValue(String.class))) return false;
        Object ronda = sala.child("rondaActual").getValue();
        if (!(ronda instanceof Number) || ((Number) ronda).intValue() != rondaCerrada) return false;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            if (!Boolean.TRUE.equals(sala.child("revisionLista/ronda_" + rondaCerrada)
                    .child(jugador.getKey()).getValue(Boolean.class))) return false;
        }
        for (Map.Entry<String, Object> cambio : cambios.entrySet()) sala.child(cambio.getKey()).setValue(cambio.getValue());
        return true;
    }
}
