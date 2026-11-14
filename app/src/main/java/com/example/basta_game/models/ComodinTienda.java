package com.example.basta_game.models;

public class ComodinTienda {

    private String id;
    private String nombre;
    private String descripcion;
    private String rareza; // Para agruparlos en los carriles (Control, Riesgo, Caóticas, Legendarias)
    private String etiquetaTactiva; // "ataque", "defensa", "efecto_global", "estrategia" (Para el color de la tarjeta)
    private int iconoResId;
    private int precio;
    private String tipoMoneda; // "monedas" o "gemas"
    private String estadisticas; // Reglas separadas por salto de línea (\n)

    public ComodinTienda(String id, String nombre, String descripcion, String rareza, String etiquetaTactiva, int iconoResId, int precio, String tipoMoneda, String estadisticas) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.rareza = rareza;
        this.etiquetaTactiva = etiquetaTactiva;
        this.iconoResId = iconoResId;
        this.precio = precio;
        this.tipoMoneda = tipoMoneda;
        this.estadisticas = estadisticas;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getRareza() { return rareza; }
    public String getEtiquetaTactiva() { return etiquetaTactiva; }
    public int getIconoResId() { return iconoResId; }
    public int getPrecio() { return precio; }
    public String getTipoMoneda() { return tipoMoneda; }
    public String getEstadisticas() { return estadisticas; }

    // --- SETTERS ---
    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setRareza(String rareza) { this.rareza = rareza; }
    public void setEtiquetaTactiva(String etiquetaTactiva) { this.etiquetaTactiva = etiquetaTactiva; }
    public void setIconoResId(int iconoResId) { this.iconoResId = iconoResId; }
    public void setPrecio(int precio) { this.precio = precio; }
    public void setTipoMoneda(String tipoMoneda) { this.tipoMoneda = tipoMoneda; }
    public void setEstadisticas(String estadisticas) { this.estadisticas = estadisticas; }
}