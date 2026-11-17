package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

public final class ReservaCodigo {
    private ReservaCodigo() { }
    public static boolean reservar(MutableData codigo,String valor,String sala) {
        if(codigo.getValue()!=null || !RecompensasPartida.idValido(sala)) return false;
        codigo.child("codigoSala").setValue(valor);
        codigo.child("idSala").setValue(sala);
        codigo.child("estaActiva").setValue(false);
        return true;
    }
    public static boolean activar(MutableData codigo,String sala) {
        if(!sala.equals(codigo.child("idSala").getValue())) return false;
        codigo.child("estaActiva").setValue(true); return true;
    }
    public static boolean liberar(MutableData codigo,String sala) {
        if(!sala.equals(codigo.child("idSala").getValue()) || Boolean.TRUE.equals(codigo.child("estaActiva").getValue())) return false;
        codigo.setValue(null); return true;
    }
    public static boolean borrarSalaParcial(MutableData sala,String id,String host) {
        if(!id.equals(sala.child("idSala").getValue()) || !host.equals(sala.child("hostUID").getValue()) ||
                (!"En espera".equals(sala.child("estadoPartida").getValue()) && !"Esperando jugadores".equals(sala.child("estadoPartida").getValue())) || sala.child("jugadores").getChildrenCount()>1) return false;
        if(sala.child("jugadores").hasChildren() && !sala.child("jugadores").hasChild(host)) return false;
        sala.setValue(null);return true;
    }
}
