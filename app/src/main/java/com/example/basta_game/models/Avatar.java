package com.example.basta_game.models;

public class Avatar {
    private String nombreArchivo;
    private String urlDescarga;

    public Avatar(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
        this.urlDescarga = "";
    }

    public Avatar(String nombreArchivo, String urlDescarga) {
        this.nombreArchivo = nombreArchivo;
        this.urlDescarga = urlDescarga;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUrlDescarga() {
        return urlDescarga;
    }

    public void setUrlDescarga(String urlDescarga) {
        this.urlDescarga = urlDescarga;
    }
}
