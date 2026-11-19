package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

public final class IdentidadJugador {
    private IdentidadJugador() { }

    public static boolean nombreValido(String nombre) {
        return nombre != null && !nombre.trim().isEmpty() && !"null".equalsIgnoreCase(nombre.trim());
    }

    public static boolean aplicar(MutableData jugador, String uid, String nombre, String foto) {
        if (uid == null || uid.isEmpty() || !nombreValido(nombre)) return false;
        if (jugador.getValue() == null) {
            jugador.child("puntajePartida").setValue(0);
            jugador.child("estaListo").setValue(false);
            jugador.child("esHost").setValue(false);
            jugador.child("estaConectado").setValue(true);
        }
        jugador.child("uid").setValue(uid);
        jugador.child("nombre").setValue(nombre);
        if (foto != null && !foto.trim().isEmpty()) jugador.child("urlAvatar").setValue(foto);
        return true;
    }
}
