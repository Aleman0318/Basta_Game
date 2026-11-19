package com.example.basta_game.utils;

/** Estado visible de una sala pública. No concede permisos para ingresar. */
public final class EstadoSalaPublica {
    private EstadoSalaPublica() { }

    public static boolean enLobby(String estado) {
        return "En espera".equals(estado) || "Esperando jugadores".equals(estado);
    }

    public static boolean permiteUnirse(String estado, int jugadores, int maximos) {
        return enLobby(estado) && maximos > 0 && jugadores < maximos;
    }

    public static String etiqueta(String estado, int jugadores, int maximos) {
        String base = enLobby(estado) ? "En lobby" :
                "En juego".equalsIgnoreCase(estado) ? "En partida" : "No disponible";
        if (maximos > 0 && jugadores >= maximos) return base + " · LLENA";
        return base;
    }
}
