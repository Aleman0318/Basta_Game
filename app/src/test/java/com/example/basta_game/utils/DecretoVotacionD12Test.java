package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DecretoVotacionD12Test {
    private MutableData sala() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");
        s.child("rondaActual").setValue(1);
        s.child("fase").setValue("jugando");
        s.child("letraActual").setValue("A");
        s.child("categoriasActivas").setValue(Arrays.asList("Animal","Cosa"));
        for (String uid: Arrays.asList("a","b","c")) {
            s.child("jugadores/"+uid+"/estaConectado").setValue(true);
            s.child("respuestas/ronda_1/"+uid+"/Animal").setValue(uid.equals("a")?"Abeja":"Armadillo");
            s.child("respuestas/ronda_1/"+uid+"/Cosa").setValue("Sin respuesta");
        }
        s.child("decretoSilencio/ronda_1/idPartida").setValue("p");
        s.child("decretoSilencio/ronda_1/vocal").setValue("E");
        s.child("decretoSilencio/ronda_1/categorias").setValue(Arrays.asList("Animal","Cosa"));
        s.child("decretoSilencio/ronda_1/recibido/a").setValue(true);
        return s;
    }
    @Test public void permiteVotarUnaRespuestaConMultaSinSumarlePuntos() {
        MutableData s=sala();
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));
        assertEquals(10,((Number)s.child("validaciones/ronda_1/a/Animal/puntos").getValue()).intValue());
        MutableData r=s.child(RelojRevision.ruta(1)+"/reloj");
        r.child("modo").setValue("propuestas"); r.child("inicio").setValue(1000L);
        Map<String,Object> datos=new HashMap<>();
        datos.put("activa",true);datos.put("idVotacion","v");datos.put("claveVotacion","clave");
        datos.put("uidObjetivo","a");datos.put("categoria","Animal");datos.put("respuesta","Abeja");
        datos.put("turnoRevision",r.child("turno").getValue());
        assertTrue(VotacionRevision.crear(s,"p",1,"b",datos,1500L));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(2000L);
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","b","si",2500));
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","c","si",2500));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(2000L);
        Map<String,Object> cambios=new HashMap<>();
        cambios.put("validaciones/ronda_1/a/Animal/puntos",10L);
        cambios.put("validaciones/ronda_1/a/Animal/correcta",true);
        cambios.put("votacionActiva/ronda_1",null);
        assertTrue(VotacionRevision.resolver(s,"p",1,"v","a",s.child("votacionActiva/ronda_1/votos").getValue(),cambios,8000));
        assertEquals(0,((Number)s.child("validaciones/ronda_1/a/Animal/puntos").getValue()).intValue());
        assertEquals(false,s.child("validaciones/ronda_1/a/Animal/correcta").getValue());
    }
    @Test public void noVotarNoPerdonaVocalPeroMantieneTresSegundosDeResultado() {
        MutableData s=sala();
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));
        // Antes de votar, conservamos los puntos provisionales y el aviso de multa.
        assertEquals(10,((Number)s.child("validaciones/ronda_1/a/Animal/puntos").getValue()).intValue());
        assertTrue(Boolean.TRUE.equals(s.child("decretoSilencio/ronda_1/evaluaciones/Animal/a/infraccion").getValue()));
        MutableData r=s.child(RelojRevision.ruta(1)+"/reloj");
        r.child("modo").setValue("propuestas");r.child("inicio").setValue(1000L);
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",6500));
        assertEquals("resultado",r.child("modo").getValue());
        assertEquals(3000,((Number)r.child("duracionMs").getValue()).intValue());
        assertEquals(0,((Number)s.child("validaciones/ronda_1/a/Animal/puntos").getValue()).intValue());
        assertEquals(5,((Number)s.child("validaciones/ronda_1/b/Animal/puntos").getValue()).intValue());
    }
    @Test public void unaCasillaVaciaNuncaDaDiezPuntos() {
        MutableData s=sala();
        s.child("validaciones/ronda_1/a/Cosa/puntos").setValue(10L);
        s.child("validaciones/ronda_1/a/Cosa/correcta").setValue(true);
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));
        s.child(RelojRevision.ruta(1)+"/reloj/modo").setValue("revision");
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000L);
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",6000));
        // Revisión de Animal; avanzar a Cosa para comprobar el 0 incluso si estaba guardado un +10.
        s.child(RelojRevision.ruta(1)+"/indiceActual").setValue(1L);
        s.child(RelojRevision.ruta(1)+"/reloj").setValue(null);
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",6100));
        assertEquals(0,((Number)s.child("validaciones/ronda_1/a/Cosa/puntos").getValue()).intValue());
    }
}
