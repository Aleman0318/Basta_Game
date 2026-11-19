package com.example.basta_game.utils;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ResultadoMarcaTest {
    public static MutableData sala(long victima) {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("fase").setValue("revision_puntuaciones");
        s.child("jugadores/a/estaConectado").setValue(true);s.child("jugadores/v/estaConectado").setValue(true);
        s.child("puntajesRondas/ronda_1/a").setValue(40);s.child("puntajesRondas/ronda_1/v").setValue(victima);
        MutableData j=s.child("minijuegoActual");j.child("idUso").setValue("uso");j.child("idPartida").setValue("p");j.child("ronda").setValue(1);
        j.child("estado").setValue("terminado");j.child("uidAtacante").setValue("a");j.child("uidVictima").setValue("v");j.child("ganadorUid").setValue("a");return s;
    }
    @Test public void robaSoloLaMitadYNoRepite() {
        MutableData s=sala(100);assertTrue(ResultadoMarca.aplicar(s,"p",1,"uso","a"));
        assertEquals(50L,s.child("puntajesRondas/ronda_1/v").getValue());assertEquals(90L,s.child("puntajesRondas/ronda_1/a").getValue());
        assertFalse(ResultadoMarca.aplicar(s,"p",1,"uso","v"));
    }
    @Test public void imparRedondeaHaciaAbajoYCeroNoEsNegativo() {
        for(long puntos:new long[]{0,1,101}) {MutableData s=sala(puntos);assertTrue(ResultadoMarca.aplicar(s,"p",1,"uso","v"));
            assertEquals(puntos-puntos/2,s.child("puntajesRondas/ronda_1/v").getValue());assertEquals(40+puntos/2,s.child("puntajesRondas/ronda_1/a").getValue());}
    }
    @Test public void defensaConservaLaConsecuenciaAnteriorSinMultaNueva() {
        MutableData s=sala(100);s.child("minijuegoActual/ganadorUid").setValue("v");assertTrue(ResultadoMarca.aplicar(s,"p",1,"uso","a"));
        assertEquals(100L,s.child("puntajesRondas/ronda_1/v").getValue());assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());
    }
    @Test public void callbackAntiguoOtraPartidaYEspectadorNoCambianPuntos() {
        MutableData s=sala(100);assertFalse(ResultadoMarca.aplicar(s,"p",1,"viejo","a"));assertFalse(ResultadoMarca.aplicar(s,"otra",1,"uso","a"));
        assertFalse(ResultadoMarca.aplicar(s,"p",1,"uso","externo"));assertTrue(s.child("minijuegoActual").getValue()!=null);
    }
    @Test public void deudaDeDobleONadaNoDejaMarcaPegada() {
        MutableData s=sala(-15);
        s.child("puntajesRondas/ronda_1/a").setValue(-5);
        assertTrue(ResultadoMarca.aplicar(s,"p",1,"uso","v"));
        assertEquals(-15L,s.child("puntajesRondas/ronda_1/v").getValue());
        assertEquals(-5L,s.child("puntajesRondas/ronda_1/a").getValue());
        assertNull(s.child("minijuegoActual").getValue());
    }
    @Test public void comunismoConservaElRestoConOrdenDeterminista() {
        MutableData s=sala(2);s.child("puntajesRondas/ronda_1/a").setValue(5);
        assertTrue(EfectoRuleta.aplicar(s,1,"a","A","ruleta",0,.1,.2));
        assertEquals(4L,s.child("puntajesRondas/ronda_1/a").getValue());assertEquals(3L,s.child("puntajesRondas/ronda_1/v").getValue());
        assertFalse(EfectoRuleta.aplicar(s,1,"v","V","ruleta",0,.2,.3));
    }
}
