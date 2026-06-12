package com.example.basta_game.models;

public class Usuario {

    private String uid;
    private String nombre;
    private String correo;
    private int partidasJugadas;
    private int partidasGanadas;
    private int partidasPerdidas;
    private int puntajeGlobal;
    private String avatarID;
    private long fechaRegistro;
    private String fotoPerfilUrl;


    public Usuario() {
    }

    public Usuario(String uid, String nombre, String correo, int partidasJugadas, int partidasGanadas, int partidasPerdidas, int puntajeGlobal, String avatarID, long fechaRegistro, String fotoPerfilUrl) {
        this.uid = uid;
        this.nombre = nombre;
        this.correo = correo;
        this.partidasJugadas = partidasJugadas;
        this.partidasGanadas = partidasGanadas;
        this.partidasPerdidas = partidasPerdidas;
        this.puntajeGlobal = puntajeGlobal;
        this.avatarID = avatarID;
        this.fechaRegistro = fechaRegistro;
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
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

    public int getPartidasJugadas() {
        return partidasJugadas;
    }

    public void setPartidasJugadas(int partidasJugadas) {
        this.partidasJugadas = partidasJugadas;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public void setPartidasGanadas(int partidasGanadas) {
        this.partidasGanadas = partidasGanadas;
    }

    public int getPartidasPerdidas() {
        return partidasPerdidas;
    }

    public void setPartidasPerdidas(int partidasPerdidas) {
        this.partidasPerdidas = partidasPerdidas;
    }

    public int getPuntajeGlobal() {
        return puntajeGlobal;
    }

    public void setPuntajeGlobal(int puntajeGlobal) {
        this.puntajeGlobal = puntajeGlobal;
    }

    public String getAvatarID() {
        return avatarID;
    }

    public void setAvatarID(String avatarID) {
        this.avatarID = avatarID;
    }

    public long getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(long fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
