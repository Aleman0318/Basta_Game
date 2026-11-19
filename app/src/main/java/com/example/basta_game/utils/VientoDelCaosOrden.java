package com.example.basta_game.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Permutacion aleatoria determinista sin puntos fijos, respecto al orden ORIGINAL
 * de la ronda (la lista compartida por la sala). Nunca modifica respuestas. */
public final class VientoDelCaosOrden {
    private VientoDelCaosOrden() { }

    public static List<String> generar(List<String> nombres, long semilla) {
        List<String> original = new ArrayList<>(nombres);
        if (original.size() < 2) return original;
        // Las categorias de una ronda deben tener nombres unicos.
        // Evitamos un bucle imposible si llegan datos duplicados de una sala antigua.
        if (new java.util.HashSet<>(original).size() != original.size()) return original;
        if (original.size() == 2) {
            Collections.swap(original, 0, 1);
            return original;
        }
        // Barajar hasta obtener una permutacion sin elementos en su sitio.
        // Probabilidad ~37% por intento para n>=4: con 2-3 intentos suele bastar.
        Random aleatorio = new Random(semilla);
        List<String> mezcla = new ArrayList<>(original);
        for (int intento = 0; intento < 128; intento++) {
            Collections.shuffle(mezcla, aleatorio);
            if (ningunaEnSuSitio(original, mezcla)) return mezcla;
        }
        // Salida de seguridad determinista: tambien garantiza cero posiciones fijas.
        Collections.rotate(original, 1);
        return original;
    }

    private static boolean ningunaEnSuSitio(List<String> original, List<String> mezcla) {
        for (int i = 0; i < original.size(); i++) {
            if (original.get(i).equals(mezcla.get(i))) return false;
        }
        return true;
    }
}
