package com.example.basta_game.utils;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResultadoVotosTest {
    @Test public void empateRealDaResultadoDistintoDeAbstencion() {
        assertEquals("empate",ResultadoVotos.resolver(2,2));
        assertEquals("empate",ResultadoVotos.resolver(1,1));
    }
    @Test public void abstencionesYAusenciasNoAnulanMayoria() {
        assertEquals("si",ResultadoVotos.resolver(2,1));
        assertEquals("si",ResultadoVotos.resolver(1,0));
        assertEquals("no",ResultadoVotos.resolver(0,1));
    }
    @Test public void ceroVotosDecisivosNoEsEmpate() {
        assertEquals("abstencion",ResultadoVotos.resolver(0,0));
    }
}
