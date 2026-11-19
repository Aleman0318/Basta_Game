package com.example.basta_game.utils;

public final class EstadoRevision {
    private EstadoRevision() { }

    public static boolean vacia(String palabra) {
        return palabra == null || palabra.trim().isEmpty() || "Sin respuesta".equalsIgnoreCase(palabra.trim());
    }

    public static boolean definitiva(String palabra, boolean validada, int puntos, boolean resuelta, boolean mostrada) {
        return vacia(palabra) || (validada && (puntos == 0 || puntos == 5 || resuelta || mostrada));
    }
}
