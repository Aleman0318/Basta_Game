package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;
import java.util.Objects;

public final class VotacionRevision {
    private VotacionRevision() { }

    private static boolean rondaVigente(MutableData sala, String partida, int ronda) {
        Object actual = sala.child("rondaActual").getValue();
        return partida != null && partida.equals(sala.child("idPartida").getValue(String.class)) &&
                actual instanceof Number && ((Number)actual).intValue() == ronda &&
                !"resultados_finales".equals(sala.child("fase").getValue(String.class)) &&
                !Boolean.TRUE.equals(sala.child("resumenRondaListo/ronda_" + ronda).getValue());
    }

    private static boolean activa(MutableData sala, String partida, int ronda, String id) {
        MutableData voto = sala.child("votacionActiva/ronda_" + ronda);
        return rondaVigente(sala,partida,ronda) && id != null && id.equals(voto.child("idVotacion").getValue()) &&
                Boolean.TRUE.equals(voto.child("activa").getValue()) && !Boolean.TRUE.equals(voto.child("resuelta").getValue());
    }

    public static boolean crear(MutableData sala, String partida, int ronda, String uid, Map<String,Object> datos) {
        MutableData voto = sala.child("votacionActiva/ronda_" + ronda);
        Object clave = datos.get("claveVotacion");
        Object objetivo = datos.get("uidObjetivo");
        Object categoria = datos.get("categoria");
        if (!rondaVigente(sala,partida,ronda) || uid == null || !sala.child("jugadores").hasChild(uid) ||
                !(objetivo instanceof String) || !sala.child("jugadores").hasChild((String)objetivo) ||
                !(datos.get("respuesta") instanceof String) || EstadoRevision.vacia((String)datos.get("respuesta")) ||
                !(categoria instanceof String) || !(clave instanceof String) || Boolean.TRUE.equals(voto.child("activa").getValue()) ||
                Boolean.TRUE.equals(sala.child("resultadosVotacionPalabras/ronda_" + ronda)
                        .child((String)clave).child("resuelta").getValue())) return false;
        Object puntos = sala.child("validaciones/ronda_" + ronda).child((String)objetivo)
                .child((String)categoria).child("puntos").getValue();
        if (puntos instanceof Number && ((Number)puntos).intValue() == 5) return false;
        voto.setValue(datos);
        return true;
    }

    public static boolean registrar(MutableData sala, String partida, int ronda, String id, String uid, String valor) {
        if (!activa(sala,partida,ronda,id) || uid == null || !sala.child("jugadores").hasChild(uid) ||
                !("si".equals(valor) || "no".equals(valor) || "abstenerse".equals(valor))) return false;
        MutableData votos = sala.child("votacionActiva/ronda_" + ronda + "/votos");
        if (votos.hasChild(uid)) return false;
        votos.child(uid).setValue(valor);
        return true;
    }

    public static boolean resolver(MutableData sala, String partida, int ronda, String id, String host,
                                   Object votosEsperados, Map<String,Object> cambios) {
        if (!activa(sala,partida,ronda,id) || host == null || !host.equals(sala.child("hostUID").getValue()) ||
                !sala.child("jugadores").hasChild(host) ||
                !Objects.equals(votosEsperados,sala.child("votacionActiva/ronda_" + ronda + "/votos").getValue())) return false;
        for (Map.Entry<String,Object> cambio : cambios.entrySet()) sala.child(cambio.getKey()).setValue(cambio.getValue());
        return true;
    }
}
