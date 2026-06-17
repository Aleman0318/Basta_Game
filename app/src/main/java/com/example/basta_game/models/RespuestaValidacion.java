package com.example.basta_game.models;

public class RespuestaValidacion {
    private String nombreJugador;
    private String respuesta;
    private String fotoPerfilUrl;

    public RespuestaValidacion(String nombreJugador, String respuesta, String fotoPerfilUrl) {
        this.nombreJugador = nombreJugador;
        this.respuesta = respuesta;
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getNombreJugador() {
        return nombreJugador;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }
}