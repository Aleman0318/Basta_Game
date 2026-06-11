package com.example.basta_game.utils;

import java.util.Random;

public class LetraSala {
    private static String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String ElegirLetraAlAzar(){
        Random random = new Random();
        int posicionAleatoria = random.nextInt(letras.length());
        String letraElegida = String.valueOf(letras.charAt(posicionAleatoria));

        return letraElegida;
    }

}
