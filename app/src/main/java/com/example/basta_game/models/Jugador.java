package com.example.basta_game.models;

public class Jugador {
    private String nombre;
    private boolean estaListo;

    private String urlPerfil;

    public Jugador(String nombre, boolean estaListo, String urlPerfil) {
        this.nombre = nombre;
        this.estaListo = estaListo;
        this.urlPerfil = urlPerfil;
    }

    public String getNombre() { return nombre; }
    public boolean isEstaListo() { return estaListo; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEstaListo(boolean estaListo) {
        this.estaListo = estaListo;
    }

    public String getUrlPerfil() {
        return urlPerfil;
    }

    public void setUrlPerfil(String urlPerfil) {
        this.urlPerfil = urlPerfil;
    }
}