package com.example.basta_game.models;

public class EventoComodin {
    private String titulo;
    private String descripcion;
    private int iconoResId;
    private boolean esBeneficio; // true si ganó puntos, false si fue castigado

    public EventoComodin(String titulo, String descripcion, int iconoResId, boolean esBeneficio) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.iconoResId = iconoResId;
        this.esBeneficio = esBeneficio;
    }

    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public int getIconoResId() { return iconoResId; }
    public boolean isEsBeneficio() { return esBeneficio; }
}