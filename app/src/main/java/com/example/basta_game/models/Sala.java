package com.example.basta_game.models;

import java.util.ArrayList;

public class Sala {

    private String idSala;
    private String idPartida;
    private boolean publica;
    private String codigoSala;
    private String hostUID;
    private String hostNombre;
    private String estadoPartida;
    private String letraActual;
    private int rondaActual;
    private int cantidadRondas;
    private long fechaCreacion;
    private int maxJugadores;
    private int tiempoRonda;
    private ArrayList<String> categoriasActivas;

    public Sala() {
    }

    public Sala(String idSala, String codigoSala, String hostUID, String hostNombre, String estadoPartida, String letraActual, int rondaActual, int cantidadRondas, long fechaCreacion, int maxJugadores, int tiempoRonda, ArrayList<String> categoriasActivas) {
        this.idSala = idSala;
        this.codigoSala = codigoSala;
        this.hostUID = hostUID;
        this.hostNombre = hostNombre;
        this.estadoPartida = estadoPartida;
        this.letraActual = letraActual;
        this.rondaActual = rondaActual;
        this.cantidadRondas = cantidadRondas;
        this.fechaCreacion = fechaCreacion;
        this.maxJugadores = maxJugadores;
        this.tiempoRonda = tiempoRonda;
        this.categoriasActivas = categoriasActivas;
    }

    public boolean isPublica() { return publica; }
    public void setPublica(boolean publica) { this.publica = publica; }

    public String getIdPartida() { return idPartida; }

    public void setIdPartida(String idPartida) { this.idPartida = idPartida; }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getHostUID() {
        return hostUID;
    }

    public void setHostUID(String hostUID) {
        this.hostUID = hostUID;
    }

    public String getHostNombre() {
        return hostNombre;
    }

    public void setHostNombre(String hostNombre) {
        this.hostNombre = hostNombre;
    }

    public String getEstadoPartida() {
        return estadoPartida;
    }

    public void setEstadoPartida(String estadoPartida) {
        this.estadoPartida = estadoPartida;
    }

    public String getLetraActual() {
        return letraActual;
    }

    public void setLetraActual(String letraActual) {
        this.letraActual = letraActual;
    }

    public int getRondaActual() {
        return rondaActual;
    }

    public void setRondaActual(int rondaActual) {
        this.rondaActual = rondaActual;
    }

    public int getCantidadRondas() {
        return cantidadRondas;
    }

    public void setCantidadRondas(int cantidadRondas) {
        this.cantidadRondas = cantidadRondas;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public int getMaxJugadores() {
        return maxJugadores;
    }

    public void setMaxJugadores(int maxJugadores) {
        this.maxJugadores = maxJugadores;
    }

    public int getTiempoRonda() {
        return tiempoRonda;
    }

    public void setTiempoRonda(int tiempoRonda) {
        this.tiempoRonda = tiempoRonda;
    }

    public ArrayList<String> getCategoriasActivas() {
        return categoriasActivas;
    }

    public void setCategoriasActivas(ArrayList<String> categoriasActivas) {
        this.categoriasActivas = categoriasActivas;
    }
}
