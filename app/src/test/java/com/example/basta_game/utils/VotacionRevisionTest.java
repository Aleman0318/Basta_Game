package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class VotacionRevisionTest {
    private MutableData sala() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p"); s.child("rondaActual").setValue(1);
        s.child("hostUID").setValue("u"); s.child("jugadores/u/nombre").setValue("Diego");
        s.child("votacionActiva/ronda_1/idVotacion").setValue("v");
        s.child("votacionActiva/ronda_1/activa").setValue(true);
        return s;
    }
    @Test public void unVotoPorUsuarioYNoModificaOtraVotacion() {
        MutableData s=sala();
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","u","si"));
        Object antes=s.getValue();
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","u","no"));
        assertFalse(VotacionRevision.registrar(s,"p",1,"vieja","u","si"));
        assertFalse(VotacionRevision.registrar(s,"otra",1,"v","u","si"));
        assertEquals(antes,s.getValue());
    }
    @Test public void noAceptaSalientesNiRondasFinalizadas() {
        MutableData s=sala();
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","ausente","si"));
        s.child("fase").setValue("resultados_finales");
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","u","si"));
    }
    @Test public void cierreRepetidoOVotosCambiadosNoReaplicanResultado() {
        MutableData s=sala();
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","u","si"));
        Map<String,Object> cambios=new HashMap<>();
        cambios.put("votacionActiva/ronda_1",null);
        cambios.put("historialVotaciones/ronda_1/v/puntosFinales",10);
        assertFalse(VotacionRevision.resolver(s,"p",1,"v","anterior",Collections.singletonMap("u","si"),cambios));
        assertFalse(VotacionRevision.resolver(s,"p",1,"v","u",Collections.singletonMap("u","no"),cambios));
        assertTrue(VotacionRevision.resolver(s,"p",1,"v","u",Collections.singletonMap("u","si"),cambios));
        Object antes=s.getValue();
        assertFalse(VotacionRevision.resolver(s,"p",1,"v","u",Collections.singletonMap("u","si"),cambios));
        assertEquals(antes,s.getValue());
    }
    @Test public void palabraResueltaNoSeReabreTrasLecturaAntigua() {
        MutableData s=sala();
        s.child("votacionActiva").setValue(null);
        s.child("resultadosVotacionPalabras/ronda_1/clave/resuelta").setValue(true);
        Map<String,Object> datos=new HashMap<>(); datos.put("claveVotacion","clave"); datos.put("idVotacion","nueva");
        datos.put("uidObjetivo","u"); datos.put("respuesta","Manzana"); datos.put("categoria","Animal");
        assertFalse(VotacionRevision.crear(s,"p",1,"u",datos));
    }
    @Test public void vaciasYRepetidasNoAbrenVotacionAunqueSeLlameDirectamente() {
        MutableData s=sala(); s.child("votacionActiva").setValue(null);
        Map<String,Object> datos=new HashMap<>();
        datos.put("claveVotacion","k"); datos.put("idVotacion","v"); datos.put("activa",true);
        datos.put("uidObjetivo","u"); datos.put("categoria","Animal"); datos.put("respuesta"," ");
        assertFalse(VotacionRevision.crear(s,"p",1,"u",datos));
        datos.put("respuesta","Manzana");
        s.child("validaciones/ronda_1/u/Animal/puntos").setValue(5);
        assertFalse(VotacionRevision.crear(s,"p",1,"u",datos));
        s.child("validaciones/ronda_1/u/Animal/puntos").setValue(10);
        assertTrue(VotacionRevision.crear(s,"p",1,"u",datos));
    }
}
