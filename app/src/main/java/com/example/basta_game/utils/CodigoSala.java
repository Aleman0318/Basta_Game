package com.example.basta_game.utils;

import java.util.ArrayList;
import java.util.Random;

public class CodigoSala {
    public static String invitacion(String codigo) { return "¡JUEGA BASTA CONMIGO! El código de la sala es: "+codigo; }

    private static String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static int tamCodigo = 6;

    private static ArrayList<String> listaCodigosGeneradosRecientemente = new ArrayList<>();

    public static String GenerarCodigoSala(){

        Random random = new Random();
        StringBuilder codigo = new StringBuilder();

        for (int i = 0; i < tamCodigo; i++) {
            int posicionAleatoria = random.nextInt(caracteres.length());
            char caracterAleatorio = caracteres.charAt(posicionAleatoria);
            codigo.append(caracterAleatorio);
        }


        listaCodigosGeneradosRecientemente.add(codigo.toString());
        return codigo.toString();
    }


    public static ArrayList<String> listaCodigosRecientes(){
        return listaCodigosGeneradosRecientemente;
    };



}
