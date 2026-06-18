package com.example.basta_game.models;

public class ResultadoApi {

    private String categoria;
    private String palabra;
    private boolean correcta;
    private String mensaje;

    public boolean isCorrecta() {
        return correcta;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getPalabra() {
        return palabra;
    }
}