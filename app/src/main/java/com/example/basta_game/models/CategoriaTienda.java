package com.example.basta_game.models;

import java.util.List;

public class CategoriaTienda {
    private String nombre;
    private String subtitulo; // NUEVO: El texto de arriba a la derecha
    private int fondoResId;   // NUEVO: La imagen del contenedor completo
    private List<ComodinTienda> comodines;

    public CategoriaTienda(String nombre, String subtitulo, int fondoResId, List<ComodinTienda> comodines) {
        this.nombre = nombre;
        this.subtitulo = subtitulo;
        this.fondoResId = fondoResId;
        this.comodines = comodines;
    }

    public String getNombre() { return nombre; }
    public String getSubtitulo() { return subtitulo; }
    public int getFondoResId() { return fondoResId; }
    public List<ComodinTienda> getComodines() { return comodines; }
}