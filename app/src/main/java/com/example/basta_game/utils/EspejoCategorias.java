package com.example.basta_game.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Seleccion impredecible por ataque, pero estable para el mismo idUso.
 * Incluye campos vacios: siempre hasta 3 categorias de la ronda, sin sesgo hacia el campo enfocado.
 */
public final class EspejoCategorias {
    private EspejoCategorias() { }

    public static List<String> elegir(List<String> categorias, Map<String,String> respuestas,
                                       String categoriaEnFoco, String idUso) {
        List<String> candidatas = new ArrayList<>();
        if (categorias == null || idUso == null) return candidatas;
        for (String nombre : categorias) {
            if (nombre != null && !nombre.trim().isEmpty() && !candidatas.contains(nombre)) {
                candidatas.add(nombre);
            }
        }
        // La semilla cambia cada vez que el atacante publica un UUID diferente.
        long semilla = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < idUso.length(); i++) semilla = (semilla ^ idUso.charAt(i)) * 0x100000001B3L;
        Random azar = new Random(semilla);
        if (candidatas.size() <= 3) {
            Collections.shuffle(candidatas, azar);
            return candidatas;
        }
        // Elegimos posiciones dentro del orden visible sin agrupar las tres juntas.
        // Cada idUso produce una distribucion diferente; nunca hay una terna consecutiva
        // si existen 4 o mas categorias. Entre 1 y 3 tomamos las disponibles.
        List<Integer> posiciones = new ArrayList<>();
        for (int i=0; i<candidatas.size(); i++) posiciones.add(i);
        List<Integer> elegidas = new ArrayList<>();
        for(int intento=0;intento<64;intento++) {
            Collections.shuffle(posiciones, azar);
            elegidas.clear();
            elegidas.addAll(posiciones.subList(0, 3));
            Collections.sort(elegidas);
            if(!(elegidas.get(1)==elegidas.get(0)+1 &&
                    elegidas.get(2)==elegidas.get(1)+1)) break;
        }
        // Fallback determinista: nunca bloquear el hilo de UI por un azar adverso.
        if(elegidas.get(1)==elegidas.get(0)+1 && elegidas.get(2)==elegidas.get(1)+1) {
            elegidas.clear();
            elegidas.add(0);elegidas.add(1);elegidas.add(candidatas.size()-1);
        }
        List<String> seleccion = new ArrayList<>();
        for (Integer posicion : elegidas) seleccion.add(candidatas.get(posicion));
        return seleccion;
    }
}
