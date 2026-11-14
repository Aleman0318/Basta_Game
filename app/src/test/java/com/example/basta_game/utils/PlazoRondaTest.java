package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PlazoRondaTest {
    @Test public void rondaRespetaDuracionSinAgregarTiempoDeCarga() {
        assertEquals(61000L, PlazoRonda.calcularFin(1000L, 60, null));
    }

    @Test public void bastaDaDiezSegundosSinExtenderElFinOriginal() {
        assertEquals(31000L, PlazoRonda.calcularFin(1000L, 60, 21000L));
        assertEquals(61000L, PlazoRonda.calcularFin(1000L, 60, 59000L));
    }

    @Test public void clientesConDistintaLatenciaConservanElMismoVencimiento() {
        long fin = PlazoRonda.calcularFin(1000L, 60, 21000L);
        assertEquals(9000L, PlazoRonda.restante(fin, 22000L, 0L));
        assertEquals(6500L, PlazoRonda.restante(fin, 24500L, 0L));
        assertEquals(fin, 24500L + PlazoRonda.restante(fin, 24500L, 0L));
    }

    @Test public void relojLocalDesfasadoUsaOffsetDelServidor() {
        assertEquals(9000L, PlazoRonda.restante(31000L, 17000L, 5000L));
        assertEquals(9000L, PlazoRonda.restante(31000L, 27000L, -5000L));
    }

    @Test public void reabrirDespuesDelPlazoNoReiniciaLaCuenta() {
        long fin = PlazoRonda.calcularFin(1000L, 60, 21000L);
        assertEquals(0L, PlazoRonda.restante(fin, 31000L, 0L));
        assertEquals(0L, PlazoRonda.restante(fin, 40000L, 0L));
    }
}
