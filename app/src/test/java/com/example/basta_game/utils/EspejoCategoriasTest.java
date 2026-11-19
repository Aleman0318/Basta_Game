package com.example.basta_game.utils;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class EspejoCategoriasTest {
    @Test public void categoriasVaciasTambienSonCandidatas() {
        List<String> todas=Arrays.asList("Animal","Color","País","Fruta","Cosa");
        Map<String,String> respuestas=new HashMap<>();
        respuestas.put("Animal","Gato");
        List<String> elegidas=EspejoCategorias.elegir(todas,respuestas,null,"ataque-123");
        assertEquals(3,elegidas.size());
        assertTrue(todas.containsAll(elegidas));
    }
    @Test public void permiteUnaDosOTresCategoriasInclusoSinTexto() {
        assertEquals(1,EspejoCategorias.elegir(Arrays.asList("Animal"),
                Collections.emptyMap(),null,"uso-1").size());
        assertEquals(2,EspejoCategorias.elegir(Arrays.asList("Animal","Color"),
                Collections.emptyMap(),null,"uso-2").size());
        assertEquals(3,EspejoCategorias.elegir(Arrays.asList("Animal","Color","País"),
                Collections.emptyMap(),null,"uso-3").size());
    }
    @Test public void maximoTresDeterministasPorAtaque() {
        List<String> todas=Arrays.asList("A","B","C","D","E","F");
        Map<String,String> respuestas=new HashMap<>();
        for(String c:todas)respuestas.put(c,"texto");
        List<String> a=EspejoCategorias.elegir(todas,respuestas,null,"uso-1");
        assertEquals(3,a.size());assertEquals(a,EspejoCategorias.elegir(todas,respuestas,null,"uso-1"));
        assertEquals(3,new HashSet<>(a).size());
        Set<List<String>> distintas=new HashSet<>();
        for(int i=0;i<32;i++)distintas.add(EspejoCategorias.elegir(todas,respuestas,null,"uso-"+i));
        assertTrue(distintas.size()>1);
    }
    @Test public void sinTextoNiFocoIgualSeleccionaCampos() {
        assertEquals(2, EspejoCategorias.elegir(Arrays.asList("A","B"),
                Collections.emptyMap(),null,"uso-vacio").size());
        assertTrue(EspejoCategorias.elegir(Collections.emptyList(),
                Collections.emptyMap(),null,"uso-cero").isEmpty());
    }
    @Test public void inversionRealPermaneceHastaCorreccion() {
        assertEquals("otaG", TextoEspejo.invertir("Gato"));
        assertEquals("Gato", TextoEspejo.invertir("otaG"));
        assertEquals("", TextoEspejo.invertir(null));
    }

    @Test public void nuncaEligeLasTresPosicionesContiguasEnSalasAmplias() {
        for(int cantidad=4; cantidad<=12; cantidad++) {
            List<String> todas=new ArrayList<>();
            for(int i=0;i<cantidad;i++) todas.add("Categoria"+i);
            Set<List<String>> combinaciones=new HashSet<>();
            for(int id=0;id<120;id++) {
                List<String> seleccion=EspejoCategorias.elegir(todas,Collections.emptyMap(),null,"uso-"+id);
                assertEquals(3,seleccion.size());
                List<Integer> lugares=new ArrayList<>();
                for(String categoria:seleccion) lugares.add(todas.indexOf(categoria));
                Collections.sort(lugares);
                assertFalse(lugares.get(1)==lugares.get(0)+1 && lugares.get(2)==lugares.get(1)+1);
                combinaciones.add(seleccion);
            }
            assertTrue(combinaciones.size()>1);
        }
    }
}
