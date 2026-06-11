package com.example.basta_game.models;

public class JugadorSala {

    private String uid;
    private String nombre;
    private int puntajePartida;
    private boolean estaListo;
    private boolean esHost;
    private boolean estaConectado;

    public JugadorSala() {
    }

    public JugadorSala(String uid, String nombre, int puntajePartida, boolean estaListo, boolean esHost, boolean estaConectado) {
        this.uid = uid;
        this.nombre = nombre;
        this.puntajePartida = puntajePartida;
        this.estaListo = estaListo;
        this.esHost = esHost;
        this.estaConectado = estaConectado;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getPuntajePartida() {
        return puntajePartida;
    }

    public void setPuntajePartida(int puntajePartida) {
        this.puntajePartida = puntajePartida;
    }

    public boolean isEstaListo() {
        return estaListo;
    }

    public void setEstaListo(boolean estaListo) {
        this.estaListo = estaListo;
    }

    public boolean isEsHost() {
        return esHost;
    }

    public void setEsHost(boolean esHost) {
        this.esHost = esHost;
    }

    public boolean isEstaConectado() {
        return estaConectado;
    }

    public void setEstaConectado(boolean estaConectado) {
        this.estaConectado = estaConectado;
    }
}
