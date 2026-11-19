package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CierreRespuestasTest {
    private MutableData sala() {
        MutableData s = MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p"); s.child("rondaActual").setValue(1);
        s.child("fase").setValue("jugando"); s.child("inicioRonda").setValue(1000);
        s.child("preparacionInicialMs").setValue(10000); s.child("tiempoRonda").setValue(60);
        s.child("categoriasActivas").setValue(Arrays.asList("Animal"));
        s.child("letraActual").setValue("A");
        for (String uid : Arrays.asList("a", "b")) s.child("jugadores/" + uid + "/estaConectado").setValue(true);
        return s;
    }
    @Test public void cierreNoDependeDelCallbackLocalYEsUnico() {
        MutableData s=sala();
        for(String u:Arrays.asList("a","b")) s.child("respuestas/ronda_1/"+u+"/Animal").setValue("Abeja");
        assertFalse(CierreRespuestas.aplicar(s,"p",1,"a",70999));
        assertTrue(CierreRespuestas.aplicar(s,"p",1,"b",71000));
        Object antes=s.getValue();
        assertFalse(CierreRespuestas.aplicar(s,"p",1,"a",80000));assertEquals(antes,s.getValue());
    }
    @Test public void ausenteNoBloqueaTrasPlazoSinInventarRespuesta() {
        MutableData s=sala();
        assertFalse(CierreRespuestas.aplicar(s,"p",1,"a",75999));
        assertTrue(CierreRespuestas.aplicar(s,"p",1,"a",76000));
        assertNull(s.child("respuestas/ronda_1/b").getValue());
    }
    @Test public void hostAusenteYPeticionesViejas() {
        MutableData s=sala();s.child("jugadores/b/estaConectado").setValue(false);
        s.child("hostUID").setValue("b");s.child("respuestas/ronda_1/a/Animal").setValue("Abeja");
        assertFalse(CierreRespuestas.aplicar(s,"otra",1,"a",71000));
        assertFalse(CierreRespuestas.aplicar(s,"p",2,"a",71000));
        assertTrue(CierreRespuestas.aplicar(s,"p",1,"a",71000));
    }
    @Test public void bastaYPreparacionConservanSusPlazos() {
        MutableData s=sala();assertEquals(71000,CierreRespuestas.fin(s,1));
        s.child("bastaTimeStamp").setValue(50000);assertEquals(60000,CierreRespuestas.fin(s,1));
        s.child("bastaTimeStamp").setValue(null);assertEquals(61000,CierreRespuestas.fin(s,2));
    }
    @Test public void entradaTardiaNoRecreaJugadorNiReiniciaMarca() {
        MutableData s=sala();assertTrue(InicioPartida.registrarEntrada(s,"p",1,"a"));
        Object antes=s.getValue();assertFalse(InicioPartida.registrarEntrada(s,"p",1,"a"));assertEquals(antes,s.getValue());
        TransferenciaHost.abandonar(s,"a");
        assertFalse(InicioPartida.registrarEntrada(s,"p",1,"a"));assertFalse(s.child("jugadores").hasChild("a"));
    }
}
