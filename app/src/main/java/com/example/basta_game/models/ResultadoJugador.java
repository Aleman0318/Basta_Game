package com.example.basta_game.models;

public class ResultadoJugador {

    private String uidJugador;
    private String nombreJugador;
    private int puntos;

    public ResultadoJugador(){}

    public ResultadoJugador(
            String uidJugador,
            String nombreJugador,
            int puntos
    ) {
        this.uidJugador = uidJugador;
        this.nombreJugador = nombreJugador;
        this.puntos = puntos;
    }

    public String getUidJugador() {
        return uidJugador;
    }

    public String getNombreJugador() {
        return nombreJugador;
    }

    public int getPuntos() {
        return puntos;
    }
}