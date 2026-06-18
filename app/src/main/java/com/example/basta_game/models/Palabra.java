package com.example.basta_game.models;

import com.google.gson.annotations.SerializedName;

public class Palabra {

    @SerializedName("id")
    private int id;

    @SerializedName("palabra")
    private String palabra;
    @SerializedName("categoria")
    private String categoria;

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPalabra() {
        return palabra;
    }

    public void setPalabra(String palabra) {
        this.palabra = palabra;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
