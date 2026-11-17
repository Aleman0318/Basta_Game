package com.example.basta_game.utils;

import java.util.Locale;

/** The existing offline classic validation, also used when a client never finishes preparing. */
public final class ValidacionBasica {
    private ValidacionBasica() { }
    private static String normalizar(String s) {
        return s == null ? "" : java.text.Normalizer.normalize(s.trim(),java.text.Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replace('\u00e1','a').replace('\u00e9','e').replace('\u00ed','i')
                .replace('\u00f3','o').replace('\u00fa','u').replace('\u00fc','u');
    }
    public static boolean correcta(String letra,String palabra) {
        if(EstadoRevision.vacia(palabra)) return false;
        String l=normalizar(letra), p=normalizar(palabra);
        int letras=0;
        for(int i=0;i<p.length();i++) if(Character.isLetter(p.charAt(i))) letras++;
        return !l.isEmpty() && letras>=3 && p.startsWith(l.substring(0,1));
    }
}
