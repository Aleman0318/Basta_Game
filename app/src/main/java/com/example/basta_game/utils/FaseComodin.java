package com.example.basta_game.utils;

public final class FaseComodin {
    private FaseComodin() { }

    public static boolean deResultados(String id) {
        return "uno_reversa".equals(id) || "ruleta_caotica".equals(id) || "marca_de_la_muerte".equals(id);
    }

    public static boolean disponible(String id, String fase, int ronda, int maximo) {
        if (ronda <= 0 || maximo <= 0 || ronda > maximo) return false;
        if (deResultados(id)) return ronda < maximo && "revision_puntuaciones".equals(fase);
        return ("congelador".equals(id) || "doble_o_nada".equals(id)) && "jugando".equals(fase);
    }
}
