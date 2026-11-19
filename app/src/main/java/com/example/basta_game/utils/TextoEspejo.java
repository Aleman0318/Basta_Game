package com.example.basta_game.utils;

/** Invierte caracteres; D8 confirma la inversion en respuestas al terminar el efecto. */
public final class TextoEspejo {
    private TextoEspejo() {}
    public static String invertir(CharSequence texto) {
        return texto == null ? "" : new StringBuilder(texto).reverse().toString();
    }
}
