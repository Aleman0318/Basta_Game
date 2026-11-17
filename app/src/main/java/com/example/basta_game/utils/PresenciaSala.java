package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Objects;

/** Session callbacks only own their session; derive the legacy aggregate atomically. */
public final class PresenciaSala {
    private PresenciaSala() { }
    public static boolean conectada(MutableData sala,String uid) {
        MutableData sesiones=sala.child("sesiones").child(uid);
        if(!sesiones.hasChildren()) return Boolean.TRUE.equals(sala.child("jugadores").child(uid).child("estaConectado").getValue());
        for(MutableData sesion:sesiones.getChildren()) if(Boolean.TRUE.equals(sesion.child("conectada").getValue())) return true;
        return false;
    }
    public static boolean actualizar(MutableData sala) {
        Object antes=sala.getValue();
        for(MutableData jugador:sala.child("jugadores").getChildren()) {
            if(Boolean.TRUE.equals(sala.child("expulsados").child(jugador.getKey()).getValue())) {jugador.setValue(null);continue;}
            MutableData sesiones=sala.child("sesiones").child(jugador.getKey());
            if(!sesiones.hasChildren()) continue; // Legacy clients need migration; do not invent a session.
            boolean conectada=false; long desde=0;
            for(MutableData sesion:sesiones.getChildren()) {
                conectada |= Boolean.TRUE.equals(sesion.child("conectada").getValue());
                Object tiempo=sesion.child("desconectadoDesde").getValue();
                if(tiempo instanceof Number) desde=Math.max(desde,((Number)tiempo).longValue());
            }
            if(!conectada || Boolean.FALSE.equals(jugador.child("estaConectado").getValue())) jugador.child("estaListo").setValue(false);
            jugador.child("estaConectado").setValue(conectada);
            jugador.child("desconectadoDesde").setValue(conectada || desde==0 ? null : desde);
        }
        return !Objects.equals(antes,sala.getValue());
    }
}
