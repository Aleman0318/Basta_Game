package com.example.basta_game.Models;

public class Categoria {
    private String nombre;
    private int iconoId;
    private boolean habilitada;

    public Categoria(String nombre, int iconoId) {
        this.nombre = nombre;
        this.iconoId = iconoId;
        this.habilitada = true;
    }

    public String getNombre() { return nombre; }
    public int getIconoId() { return iconoId; }
    public boolean isHabilitada() { return habilitada; }
    public void setHabilitada(boolean habilitada) { this.habilitada = habilitada; }
}