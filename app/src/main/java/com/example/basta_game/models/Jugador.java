package com.example.basta_game.models;

public class Jugador {
    private String nombre;
    private boolean estaListo;
    private String urlPerfil;

    private boolean esHost;

    public Jugador(String nombre, boolean estaListo, String urlPerfil, boolean esHost) {
        this.nombre = nombre;
        this.estaListo = estaListo;
        this.urlPerfil = urlPerfil;
        this.esHost = esHost;
    }

    public boolean isEsHost() {
        return esHost;
    }

    public void setEsHost(boolean esHost) {
        this.esHost = esHost;
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