package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AliasCambioFlujoTest {
    @Test public void ambosIdsCompartenFaseYLlaveDeLimite() {
        for(String id:Arrays.asList("uno_reversa","cambio_flujo")) {
            assertTrue(AliasCambioFlujo.es(id));assertEquals("uno_reversa",AliasCambioFlujo.canonico(id));
            assertTrue(FaseComodin.disponible(id,"revision_puntuaciones",1,3));
            assertFalse(FaseComodin.disponible(id,"jugando",1,3));assertFalse(FaseComodin.disponible(id,"revision_puntuaciones",3,3));
        }
        assertEquals("congelador",AliasCambioFlujo.canonico("congelador"));
    }
    @Test public void descuentaUnaUnidadRealYConservaLosOtrosArticulos() {
        MutableData inventario=MutableDataLocal.desde(new HashMap<>());
        inventario.child("cambio_flujo/cantidad").setValue(2);inventario.child("uno_reversa/cantidad").setValue(1);
        inventario.child("cambio_flujo/nombre").setValue("Cambio de Flujo");inventario.child("congelador/cantidad").setValue(8);
        assertTrue(AliasCambioFlujo.descontar(inventario));assertEquals(1L,inventario.child("cambio_flujo/cantidad").getValue());
        assertEquals(1L,inventario.child("uno_reversa/cantidad").getValue());
        assertTrue(AliasCambioFlujo.descontar(inventario));assertTrue(AliasCambioFlujo.descontar(inventario));assertFalse(AliasCambioFlujo.descontar(inventario));
        assertEquals(0L,inventario.child("cambio_flujo/cantidad").getValue());assertEquals(0L,inventario.child("uno_reversa/cantidad").getValue());
        assertEquals(8L,inventario.child("congelador/cantidad").getValue());assertEquals("Cambio de Flujo",inventario.child("cambio_flujo/nombre").getValue());
    }
}
