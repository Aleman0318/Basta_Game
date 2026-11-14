package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class EstadoRevisionTest {
    @Test public void vaciasNoOfrecenVotoAunqueSigaCargandoValidacion() {
        for (String palabra : new String[]{null, "", "  ", "Sin respuesta"})
            assertTrue(EstadoRevision.definitiva(palabra, false, 0, false, false));
    }
    @Test public void repetidaValidadaEsAutomaticaSinConfundirEmpateDeVotos() {
        assertTrue(EstadoRevision.definitiva("Manzana", true, 5, false, false));
        assertFalse(EstadoRevision.definitiva("Manzana", false, 5, false, false));
    }
    @Test public void resultadosDefinitivosDiezYCeroNoVuelvenAVotarse() {
        for (int puntos : new int[]{0,10}) {
            assertTrue(EstadoRevision.definitiva("Manzana", true, puntos, true, false));
            assertTrue(EstadoRevision.definitiva("Manzana", true, puntos, false, true));
        }
        assertFalse(EstadoRevision.definitiva("Manzana", true, 10, false, false));
    }
}
