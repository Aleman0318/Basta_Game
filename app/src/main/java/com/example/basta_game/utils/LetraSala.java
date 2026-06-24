package com.example.basta_game.utils;

import java.util.Random;

public class LetraSala {

    private static String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String ElegirLetraAlAzar() {
        Random random = new Random();
        int posicionAleatoria = random.nextInt(letras.length());
        return String.valueOf(letras.charAt(posicionAleatoria));
    }

    public static String ElegirLetraAlAzar(String letraAnterior) {
        String nuevaLetra;

        do {
            nuevaLetra = ElegirLetraAlAzar();
        } while (
                letraAnterior != null &&
                        nuevaLetra.equalsIgnoreCase(letraAnterior) &&
                        letras.length() > 1
        );

        return nuevaLetra;
    }
}