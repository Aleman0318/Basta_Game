package com.example.basta_game.models;

public class CodigoSalaSearch {

    private String codigoSala;
    private String idSala;
    private boolean estaActiva;

    public CodigoSalaSearch() {
    }

    public CodigoSalaSearch(String codigoSala, String idSala, boolean estaActiva) {
        this.codigoSala = codigoSala;
        this.idSala = idSala;
        this.estaActiva = estaActiva;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public boolean isEstaActiva() {
        return estaActiva;
    }

    public void setEstaActiva(boolean estaActiva) {
        this.estaActiva = estaActiva;
    }
}
