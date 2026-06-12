package com.example.basta_game.models;

public class Avatar {
    private String nombreArchivo;
    private String urlDescarga;
    private String nombrePersonalizado;


    public Avatar(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
        this.urlDescarga = "";
    }


    public Avatar(String nombreArchivo, String nombrePersonalizado) {
        this.nombreArchivo = nombreArchivo;
        this.nombrePersonalizado = nombrePersonalizado;
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

    public String getNombrePersonalizado() {
        return nombrePersonalizado;
    }

    public void setNombrePersonalizado(String nombrePersonalizado) {
        this.nombrePersonalizado = nombrePersonalizado;
    }
}
