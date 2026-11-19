package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class VientoDelCaosOrdenTest {
    @Test public void semillaCompartidaDaMismoOrden() {
        List<String> base = Arrays.asList("Nombre", "Apellido", "Animal", "Pais", "Color", "Fruta");
        assertEquals(VientoDelCaosOrden.generar(base, 12345L),
                VientoDelCaosOrden.generar(base, 12345L));
    }
    @Test public void ningunaCategoriaSeQuedaEnSuLugarParaMuchasSemillas() {
        for (int n = 2; n <= 12; n++) {
            List<String> base = new ArrayList<>();
            for (int i = 0; i < n; i++) base.add("Categoria_" + i);
            for (long seed = 0; seed < 250; seed++) {
                List<String> resultado = VientoDelCaosOrden.generar(base, seed);
                assertEquals(n, resultado.size());
                assertEquals(new HashSet<>(base), new HashSet<>(resultado));
                for (int i = 0; i < n; i++)
                    assertNotEquals("n=" + n + " seed=" + seed + " pos=" + i, base.get(i), resultado.get(i));
            }
        }
    }
    @Test public void conservaOriginalYRespetaOrdenRealDeLaSala() {
        List<String> base = new ArrayList<>(Arrays.asList("Nombre", "Apellido", "Pais", "Animal"));
        List<String> original = new ArrayList<>(base);
        List<String> resultado = VientoDelCaosOrden.generar(base, 7L);
        assertEquals(original, base);
        assertEquals(4, resultado.size());
        for (int i = 0; i < base.size(); i++) assertNotEquals(base.get(i), resultado.get(i));
    }
    @Test public void unaCategoriaNoSePuedePermutar() {
        assertEquals(Collections.singletonList("Nombre"),
                VientoDelCaosOrden.generar(Collections.singletonList("Nombre"), 1L));
    }
}
