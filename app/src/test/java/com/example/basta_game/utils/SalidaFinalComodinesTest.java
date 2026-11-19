package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class SalidaFinalComodinesTest {
    @Test public void noBorraResumenMientrasQuedaUnJugadorViendoResultados() {
        MutableData sala=MutableDataLocal.desde(new HashMap<>());
        sala.child("idPartida").setValue("p1");
        sala.child("fase").setValue("resultados_finales");
        sala.child("rondaActual").setValue(2);
        sala.child("jugadores/a/nombre").setValue("Ana");
        sala.child("jugadores/b/nombre").setValue("Beto");
        sala.child("poderesRonda/ronda_2/idPartida").setValue("p1");
        sala.child("efectosVisuales/espejo/ronda_2/a/idPartida").setValue("p1");
        sala.child("comodinesUsados/ronda_2/u/tipo").setValue("congelador");
        sala.child("usosCongeladorPartida").setValue(2);

        assertTrue(SalidaFinal.registrar(sala,"p1","a","Z"));
        assertEquals("resultados_finales",sala.child("fase").getValue());
        assertNotNull(sala.child("poderesRonda").getValue());
        assertNotNull(sala.child("comodinesUsados").getValue());

        assertTrue(SalidaFinal.registrar(sala,"p1","b","Z"));
        assertEquals("lobby",sala.child("fase").getValue());
        assertEquals("Esperando jugadores",sala.child("estadoPartida").getValue());
        assertNull(sala.child("poderesRonda").getValue());
        assertNull(sala.child("efectosVisuales").getValue());
        assertNull(sala.child("comodinesUsados").getValue());
        assertEquals(0,((Number)sala.child("usosCongeladorPartida").getValue()).intValue());
        assertEquals("Ana",sala.child("jugadores/a/nombre").getValue());
        assertEquals("Beto",sala.child("jugadores/b/nombre").getValue());
    }
}
