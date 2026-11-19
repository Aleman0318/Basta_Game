package com.example.basta_game.utils;

public final class CorreoInstitucional {
    private CorreoInstitucional() { }

    public static boolean esValido(String correo) {
        if (correo == null || !correo.equals(correo.trim())) return false;
        int arroba = correo.indexOf('@');
        return arroba > 0 && correo.substring(arroba).toLowerCase(java.util.Locale.ROOT).equals("@ues.edu.sv")
                && correo.substring(0, arroba).matches("[^\\s@]+");
    }
}
