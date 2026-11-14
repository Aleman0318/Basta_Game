package com.example.basta_game.models;

public class ComodinPartida {

    private String id;
    private String nombre;
    private String descripcion;
    private String rareza;
    private int iconoResId;
    private int cantidad;
    private boolean implementado;
    private boolean premium;

    public ComodinPartida(
            String id,
            String nombre,
            String descripcion,
            String rareza,
            int iconoResId,
            int cantidad,
            boolean implementado,
            boolean premium
    ) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.rareza = rareza;
        this.iconoResId = iconoResId;
        this.cantidad = cantidad;
        this.implementado = implementado;
        this.premium = premium;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getRareza() {
        return rareza;
    }

    public int getIconoResId() {
        return iconoResId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public boolean isImplementado() {
        return implementado;
    }

    public boolean isPremium() {
        return premium;
    }
}
