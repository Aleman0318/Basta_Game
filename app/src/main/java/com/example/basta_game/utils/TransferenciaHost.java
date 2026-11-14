package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Objects;

public final class TransferenciaHost {
    private TransferenciaHost() { }

    public static boolean abandonar(MutableData sala, String uid) {
        if (uid == null || !sala.child("jugadores").hasChild(uid)) return false;
        retirar(sala, uid);
        reconciliar(sala);
        return true;
    }

    public static boolean expirar(MutableData sala, long ahoraServidor, long graciaMs) {
        boolean cambio = false;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            Object desde = jugador.child("desconectadoDesde").getValue();
            if (Boolean.FALSE.equals(jugador.child("estaConectado").getValue()) && desde instanceof Number &&
                    ahoraServidor - ((Number) desde).longValue() >= graciaMs) {
                retirar(sala, jugador.getKey());
                cambio = true;
            }
        }
        if (cambio) reconciliar(sala);
        return cambio;
    }

    private static void retirar(MutableData sala, String uid) {
        sala.child("jugadores").child(uid).setValue(null);
        for (String lista : new String[]{"entradaPartida", "revisionLista"}) {
            for (MutableData ronda : sala.child(lista).getChildren()) ronda.child(uid).setValue(null);
        }
        for (MutableData votacion : sala.child("votacionActiva").getChildren()) {
            votacion.child("votos").child(uid).setValue(null);
        }
        MutableData duelo = sala.child("minijuegoActual");
        if (Boolean.TRUE.equals(duelo.child("activo").getValue()) &&
                !"terminado".equals(duelo.child("estado").getValue())) {
            Object atacante = duelo.child("uidAtacante").getValue();
            Object victima = duelo.child("uidVictima").getValue();
            if (uid.equals(atacante) || uid.equals(victima)) {
                duelo.child("estado").setValue("terminado");
                duelo.child("porDesconexion").setValue(true);
                duelo.child("ganadorUid").setValue(uid.equals(atacante) ? victima : atacante);
            }
        }
    }

    public static boolean reconciliar(MutableData sala) {
        if (sala.getValue() == null) return false;
        Object antes = sala.getValue();
        MutableData jugadores = sala.child("jugadores");
        if (!jugadores.hasChildren()) {
            sala.setValue(null);
            return true;
        }
        String host = sala.child("hostUID").getValue(String.class);
        // La presencia temporal no revoca una corona cuyo titular sigue en la sala.
        if (host == null || !jugadores.hasChild(host)) {
            host = null;
            for (MutableData jugador : jugadores.getChildren()) {
                if (Boolean.TRUE.equals(jugador.child("estaConectado").getValue()) &&
                        (host == null || jugador.getKey().compareTo(host) < 0)) host = jugador.getKey();
            }
        }
        for (MutableData jugador : jugadores.getChildren()) {
            jugador.child("esHost").setValue(jugador.getKey().equals(host));
        }
        sala.child("hostUID").setValue(host);
        sala.child("hostUid").setValue(host);
        Object nombre = host == null ? null : jugadores.child(host).child("nombre").getValue();
        sala.child("hostNombre").setValue(host == null ? null : nombre == null ? "Jugador" : nombre);

        Object ronda = sala.child("rondaActual").getValue();
        if (jugadores.getChildrenCount() == 1 && ronda instanceof Number && ((Number) ronda).intValue() > 0 &&
                "En juego".equals(sala.child("estadoPartida").getValue()) &&
                !"resultados_finales".equals(sala.child("fase").getValue())) {
            int actual = ((Number) ronda).intValue();
            // Cierre anticipado: conserva puntos confirmados; no calcula bonos ni respuestas incompletas.
            sala.child("cantidadRondas").setValue(actual);
            sala.child("fase").setValue("resultados_finales");
            sala.child("rondasProcesadas/ronda_" + actual).setValue(true);
            sala.child("resumenRondaListo/ronda_" + actual).setValue(true);
            sala.child("votacionActiva").setValue(null);
            sala.child("minijuegoActual").setValue(null);
        }
        return !Objects.equals(antes, sala.getValue());
    }
}
