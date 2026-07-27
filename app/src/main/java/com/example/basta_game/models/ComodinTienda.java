package com.example.basta_game.models;

public class ComodinTienda {

    private String id;
    private String nombre;
    private String descripcion;
    private String estadisticas; // <--- NUEVO CAMPO
    private String rareza;
    private String tipoMoneda;
    private int precio;
    private int iconoResId;
    private boolean premium;

    public ComodinTienda(
            String id, String nombre, String descripcion, String estadisticas,
            String rareza, String tipoMoneda, int precio, int iconoResId, boolean premium
    ) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estadisticas = estadisticas; // <--- ASIGNAR
        this.rareza = rareza;
        this.tipoMoneda = tipoMoneda;
        this.precio = precio;
        this.iconoResId = iconoResId;
        this.premium = premium;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getEstadisticas() { return estadisticas; } // <--- GETTER
    public String getRareza() { return rareza; }
    public String getTipoMoneda() { return tipoMoneda; }
    public int getPrecio() { return precio; }
    public int getIconoResId() { return iconoResId; }
    public boolean isPremium() { return premium; }
}