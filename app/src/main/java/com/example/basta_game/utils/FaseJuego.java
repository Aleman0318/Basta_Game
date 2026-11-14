package com.example.basta_game.utils;

public final class FaseJuego {
    private FaseJuego() { }

    public static boolean permiteResponder(String fase, String estado, int ronda) {
        if (ronda <= 0) return false;
        return fase == null ? "En juego".equals(estado) : "jugando".equals(fase);
    }
}
