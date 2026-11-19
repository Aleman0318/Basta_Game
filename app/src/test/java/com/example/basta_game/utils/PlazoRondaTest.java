package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PlazoRondaTest {
    @Test public void bastaAceptadoPublicaBanderaAutorYTimestampUnaVezYNoDuranteBienvenida() {
        com.google.firebase.database.MutableData s=com.google.firebase.database.MutableDataLocal.desde(new java.util.HashMap<>());
        s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("fase").setValue("jugando");
        s.child("inicioRonda").setValue(1000);s.child("tiempoRonda").setValue(60);s.child("preparacionInicialMs").setValue(10000);
        s.child("jugadores/a/estaConectado").setValue(true);s.child("jugadores/b/estaConectado").setValue(true);
        assertEquals(false,PlazoRonda.aceptarBasta(s,"p",1,"a",10999));
        assertEquals(true,PlazoRonda.aceptarBasta(s,"p",1,"a",41000));
        org.junit.Assert.assertNotNull(s.child("bastaTimeStamp").getValue());assertEquals("a",s.child("bastaPresionadoPor").getValue());
        Object antes=s.getValue();assertEquals(false,PlazoRonda.aceptarBasta(s,"p",1,"b",41001));assertEquals(antes,s.getValue());
    }
        @Test public void bastaRespetaTreintaSegundosYUltimosDiezSinReinicioLocal(){
        assertEquals(false,PlazoRonda.bastaDisponible(11000,71000,40999));
        assertEquals(true,PlazoRonda.bastaDisponible(11000,71000,41000));
        assertEquals(true,PlazoRonda.bastaDisponible(11000,71000,60999));
        assertEquals(false,PlazoRonda.bastaDisponible(11000,71000,61000));
        assertEquals(false,PlazoRonda.bastaDisponible(11000,71000,71000));
    }
    @Test public void rondaRespetaDuracionSinAgregarTiempoDeCarga() {
        assertEquals(61000L, PlazoRonda.calcularFin(1000L, 60, null));
    }

    @Test public void bastaDaDiezSegundosInclusoCercaDelFinOriginal() {
        assertEquals(31000L, PlazoRonda.calcularFin(1000L, 60, 21000L));
        assertEquals(69000L, PlazoRonda.calcularFin(1000L, 60, 59000L));
    }
    @Test public void preparacionInicialSeparadaYLecturaViejaNoRevierteBasta() {
        assertEquals(11000L,PlazoRonda.inicioRespuestas(1000,1,10000));
        assertEquals(71000L,PlazoRonda.calcularFin(PlazoRonda.inicioRespuestas(1000,1,10000),60,null));
        assertEquals(1000L,PlazoRonda.inicioRespuestas(1000,2,10000));
        org.junit.Assert.assertFalse(PlazoRonda.lecturaAplicable(2000L,null));
        org.junit.Assert.assertTrue(PlazoRonda.lecturaAplicable(2000L,2000L));
        assertEquals(0L,PlazoRonda.restante(11000,12000,0));
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
