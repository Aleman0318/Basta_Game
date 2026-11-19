package com.example.basta_game.utils;
import org.junit.Test;
import static org.junit.Assert.*;

public class VibracionRondaTest {
    @Test public void finNaturalUsaElMismoPlazoCompartido() {
        long inicio=PlazoRonda.inicioRespuestas(1000,1,10000),fin=PlazoRonda.calcularFin(inicio,60,null);
        assertFalse(VibracionRonda.esMomento(PlazoRonda.restante(fin,60999,0),false));
        assertTrue(VibracionRonda.esMomento(PlazoRonda.restante(fin,61000,0),false));
        assertFalse(VibracionRonda.esMomento(PlazoRonda.restante(fin,62000,0),true));
    }
    @Test public void bastaAceptadoSenalaUnaVezSinExtenderElReloj() {
        long fin=PlazoRonda.calcularFin(1000,90,45000L);
        assertEquals(55000,fin);assertTrue(VibracionRonda.esMomento(fin-45000,false));
        assertFalse(VibracionRonda.esMomento(fin-45000,true));
        assertFalse(VibracionRonda.esMomento(fin-47000,true));
    }
    @Test public void noSenalaAntesNiDespuesDelIntervalo() {
        assertFalse(VibracionRonda.esMomento(10001,false));assertFalse(VibracionRonda.esMomento(0,false));
        assertFalse(VibracionRonda.esMomento(-1,false));assertTrue(VibracionRonda.esMomento(1,false));
    }
}
