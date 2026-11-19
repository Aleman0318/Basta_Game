package com.example.basta_game.utils;
import java.util.Map;
public final class ConteoVotos {
    private ConteoVotos(){}
    public static int[] contar(Object elegibles,Object votos) {
        int[] n=new int[3];if(!(elegibles instanceof Map)) return n;
        Map<?,?> v=votos instanceof Map ? (Map<?,?>)votos : java.util.Collections.emptyMap();
        for(Object uid:((Map<?,?>)elegibles).keySet()) {
            Object valor=v.get(uid);n["si".equals(valor) ? 0 : "no".equals(valor) ? 1 : 2]++;
        }
        return n;
    }
}
