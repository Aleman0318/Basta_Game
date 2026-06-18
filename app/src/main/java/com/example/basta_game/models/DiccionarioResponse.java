package com.example.basta_game.models;

import java.util.List;

public class DiccionarioResponse {
    private String status;
    private int total_registros;

    private List<Palabra> data;

    public String getStatus() {
        return status;
    }

    public int getTotal_registros() {
        return total_registros;
    }

    public List<Palabra> getData() {
        return data;
    }
}
