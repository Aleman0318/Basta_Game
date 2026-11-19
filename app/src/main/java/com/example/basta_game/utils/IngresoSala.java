package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;

public final class IngresoSala {
    private IngresoSala() { }
    public static boolean reservaVigente(Object conectada,Object desde,Object sesiones,long ahora,long gracia) {
        boolean activa=Boolean.TRUE.equals(conectada);
        if(sesiones instanceof java.util.Map && !((java.util.Map<?,?>)sesiones).isEmpty()) {
            activa=false;long ultima=0;
            for(Object s:((java.util.Map<?,?>)sesiones).values()) if(s instanceof java.util.Map) {
                if(Boolean.TRUE.equals(((java.util.Map<?,?>)s).get("conectada"))) activa=true;
                Object marca=((java.util.Map<?,?>)s).get("desconectadoDesde");
                if(marca instanceof Number) ultima=Math.max(ultima,((Number)marca).longValue());
            }
            if(ultima>0) desde=ultima;
        }
        return activa || ahora==Long.MIN_VALUE || !(desde instanceof Number) || ahora-((Number)desde).longValue()<gracia;
    }
    public static boolean enLobby(MutableData sala) {
        String estado=sala.child("estadoPartida").getValue(String.class), fase=sala.child("fase").getValue(String.class);
        return ("En espera".equals(estado) || "Esperando jugadores".equals(estado)) && (fase==null || "espera".equals(fase) || "lobby".equals(fase));
    }
    public static boolean ingresar(MutableData sala,String uid,String nombre,String foto) {
        return ingresar(sala,uid,nombre,foto,Long.MIN_VALUE,90000);
    }
    public static boolean ingresar(MutableData sala,String uid,String nombre,String foto,long ahora,long gracia) {
        if(sala.getValue()==null || uid==null || Boolean.TRUE.equals(sala.child("expulsados").child(uid).getValue())) return false;
        MutableData jugador=sala.child("jugadores").child(uid);
        if(jugador.getValue()!=null) {
            Object desde=jugador.child("desconectadoDesde").getValue();
            if(!reservaVigente(jugador.child("estaConectado").getValue(),desde,sala.child("sesiones").child(uid).getValue(),ahora,gracia)) return false;
            return IdentidadJugador.aplicar(jugador,uid,nombre,foto);
        }
        Object max=sala.child("maxJugadores").getValue();
        String estado=sala.child("estadoPartida").getValue(String.class);
        if(!enLobby(sala) || !(max instanceof Number) || sala.child("jugadores").getChildrenCount()>=((Number)max).longValue() ||
                !("En espera".equals(estado) || "Esperando jugadores".equals(estado))) return false;
        if(!IdentidadJugador.aplicar(jugador,uid,nombre,foto)) return false;
        jugador.child("estaConectado").setValue(false);
        jugador.child("desconectadoDesde").setValue(ServerValue.TIMESTAMP);
        return true;
    }
}
