package com.example.basta_game.Models;

public class Jugador {
    private String nombre;
    private boolean estaListo;

    public Jugador(String nombre, boolean estaListo) {
        this.nombre = nombre;
        this.estaListo = estaListo;
    }

    public String getNombre() { return nombre; }
    public boolean isEstaListo() { return estaListo; }
}