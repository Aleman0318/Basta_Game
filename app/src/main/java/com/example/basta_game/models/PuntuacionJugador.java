package com.example.basta_game.models;

import java.util.List;

public class PuntuacionJugador {

    private int posicion;
    private String uid;
    private String nombre;
    private List<Integer> puntajesRondas;
    private int total;

    public PuntuacionJugador(
            int posicion,
            String uid,
            String nombre,
            List<Integer> puntajesRondas,
            int total
    ) {
        this.posicion = posicion;
        this.uid = uid;
        this.nombre = nombre;
        this.puntajesRondas = puntajesRondas;
        this.total = total;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public String getUid() {
        return uid;
    }

    public String getNombre() {
        return nombre;
    }

    public List<Integer> getPuntajesRondas() {
        return puntajesRondas;
    }

    public int getTotal() {
        return total;
    }
}