package com.example.basta_game.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ResumenCompras {
    private final Map<String,Integer> cantidades = new LinkedHashMap<>();
    private long ultima = Long.MIN_VALUE;

    public String agregar(String nombre, long ahora) {
        if (ultima == Long.MIN_VALUE || ahora - ultima > 4000 || ahora < ultima) cantidades.clear();
        ultima = ahora;
        Integer cantidad = cantidades.get(nombre);
        cantidades.put(nombre, cantidad == null ? 1 : cantidad + 1);
        StringBuilder texto = new StringBuilder("¡Compras realizadas!");
        for (Map.Entry<String,Integer> compra : cantidades.entrySet()) {
            texto.append("\n").append(compra.getKey()).append(": ").append(compra.getValue()).append(" comprados");
        }
        return texto.toString();
    }
}
