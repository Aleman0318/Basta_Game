package com.example.basta_game.utils;

public final class ResultadoVotos {
    private ResultadoVotos() { }
    public static String resolver(int si,int no) {
        if(si==0 && no==0) return "abstencion";
        if(si==no) return "empate";
        return si>no ? "si" : "no";
    }
}
