package com.example.basta_game.models;

public class PuntuacionDummy {
    public int posicion;
    public String nombre;
    public int r1, r2, r3, r4, r5, total;

    public PuntuacionDummy(int posicion, String nombre, int r1, int r2, int r3, int r4, int r5) {
        this.posicion = posicion;
        this.nombre = nombre;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
        this.r5 = r5;
        this.total = r1 + r2 + r3 + r4 + r5;
    }
}