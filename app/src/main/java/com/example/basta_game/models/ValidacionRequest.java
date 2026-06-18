package com.example.basta_game.models;

import java.util.List;

public class ValidacionRequest {

    private String letra;
    private List<ItemRespuesta> respuestas;

    public ValidacionRequest(String letra, List<ItemRespuesta> respuestas) {
        this.letra = letra;
        this.respuestas = respuestas;
    }

    public String getLetra() {
        return letra;
    }

    public List<ItemRespuesta> getRespuestas() {
        return respuestas;
    }
}