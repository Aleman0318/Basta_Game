package com.example.basta_game.models;

public class ItemRespuesta {

    private String categoria;
    private String palabra;

    public ItemRespuesta(String categoria, String palabra) {
        this.categoria = categoria;
        this.palabra = palabra;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getPalabra() {
        return palabra;
    }
}