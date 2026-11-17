package com.example.basta_game.utils;
import java.util.Locale;
public final class ClaveRevision {
    private ClaveRevision() { }
    private static String normal(String s) {
        if(s==null) return "_";
        String n=s.trim().toLowerCase(Locale.ROOT).replace('á','a').replace('é','e').replace('í','i').replace('ó','o')
                .replace('ú','u').replace('ü','u').replace('ñ','n').replaceAll("[.#$\\[\\]/ ]","_");
        return n.isEmpty() ? "_" : n;
    }
    public static String de(int ronda,String uid,String categoria,String palabra) {return "r"+ronda+"_"+normal(uid)+"_"+normal(categoria)+"_"+normal(palabra);}
}
