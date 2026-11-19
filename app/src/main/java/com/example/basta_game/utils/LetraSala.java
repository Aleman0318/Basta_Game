package com.example.basta_game.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class LetraSala {

    private static final String letras = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
    public static ArrayList<String> nuevaPartida() {
        ArrayList<String> orden=new ArrayList<>();
        for(int i=0;i<letras.length();i++) orden.add(String.valueOf(letras.charAt(i)));
        Collections.shuffle(orden);return orden;
    }

    private static ArrayList<String> bolsaLetras = new ArrayList<>();
    private static ArrayList<String> letrasUsadas = new ArrayList<>();

    private static final Random random = new Random();

    public static String ElegirLetraAlAzar() {
        prepararBolsaSiEsNecesario();

        int posicionAleatoria = random.nextInt(bolsaLetras.size());
        String letraElegida = bolsaLetras.get(posicionAleatoria);

        bolsaLetras.remove(posicionAleatoria);
        letrasUsadas.add(letraElegida);

        return letraElegida;
    }

    public static String ElegirLetraAlAzar(String letraAnterior) {
        if (letraAnterior != null && !letraAnterior.trim().isEmpty()) {
            registrarLetraUsada(letraAnterior);
        }

        prepararBolsaSiEsNecesario();

        String nuevaLetra;

        do {
            int posicionAleatoria = random.nextInt(bolsaLetras.size());
            nuevaLetra = bolsaLetras.get(posicionAleatoria);
        } while (
                letraAnterior != null &&
                        nuevaLetra.equalsIgnoreCase(letraAnterior) &&
                        bolsaLetras.size() > 1
        );

        bolsaLetras.remove(nuevaLetra);
        letrasUsadas.add(nuevaLetra);

        return nuevaLetra;
    }

    private static void prepararBolsaSiEsNecesario() {
        if (!bolsaLetras.isEmpty()) {
            return;
        }

        bolsaLetras.clear();

        for (int i = 0; i < letras.length(); i++) {
            String letra = String.valueOf(letras.charAt(i));

            if (!letrasUsadas.contains(letra)) {
                bolsaLetras.add(letra);
            }
        }

        if (bolsaLetras.isEmpty()) {
            letrasUsadas.clear();

            for (int i = 0; i < letras.length(); i++) {
                bolsaLetras.add(String.valueOf(letras.charAt(i)));
            }
        }

        Collections.shuffle(bolsaLetras);
    }

    private static void registrarLetraUsada(String letra) {
        letra = letra.toUpperCase();

        if (!letrasUsadas.contains(letra)) {
            letrasUsadas.add(letra);
        }

        bolsaLetras.remove(letra);
    }

    public static void ReiniciarHistorialLetras() {
        bolsaLetras.clear();
        letrasUsadas.clear();
    }
}
