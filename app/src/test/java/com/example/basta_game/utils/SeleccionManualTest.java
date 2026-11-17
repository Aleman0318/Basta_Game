package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import com.google.firebase.database.ServerValue;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeleccionManualTest {
    private static final String RELOJ="revisionCategorias/ronda_1/reloj";
    private MutableData sala(int n) {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("hostUID").setValue("u0");
        s.child("categoriasActivas").setValue(Arrays.asList("Animal","Color"));s.child("letraActual").setValue("A");
        for(int i=0;i<n;i++) {
            String uid="u"+i;s.child("jugadores/"+uid+"/estaConectado").setValue(true);
            s.child("respuestas/ronda_1/"+uid+"/Animal").setValue("Abeja "+i);
        }
        assertTrue(RelojRevision.coordinar(s,"p",1,"u0",1000));s.child(RELOJ+"/inicio").setValue(1000);
        assertTrue(RelojRevision.coordinar(s,"p",1,"u0",4500));s.child(RELOJ+"/inicio").setValue(4500);
        return s;
    }
    private Map<String,Object> propuesta(MutableData s,int objetivo,String id) {
        Map<String,Object> p=new HashMap<>();String uid="u"+objetivo,palabra="Abeja "+objetivo;
        p.put("idVotacion",id);p.put("activa",true);p.put("uidObjetivo",uid);p.put("categoria","Animal");
        p.put("respuesta",palabra);p.put("claveVotacion",ClaveRevision.de(1,uid,"Animal",palabra));
        p.put("turnoRevision",s.child(RELOJ+"/turno").getValue());return p;
    }
    private void cerrar(MutableData s,String id,String cliente,long ahora) {
        Object votos=s.child("votacionActiva/ronda_1/votos").getValue();
        Map<String,Object> cambios=new HashMap<>();cambios.put("votacionActiva/ronda_1",null);
        assertTrue(VotacionRevision.resolver(s,"p",1,id,cliente,votos,cambios,ahora));
    }
    @Test public void tresVotacionesReinicianSoloAlCerrarYNoReabrenRespuesta() {
        MutableData s=sala(5);long inicio=4500;
        for(int i=0;i<3;i++) {
            String id="v"+i;Map<String,Object> p=propuesta(s,i,id);
            assertTrue(VotacionRevision.crear(s,"p",1,"u1",p,inicio+100));
            assertEquals(inicio,s.child(RELOJ+"/seleccionInicio").getValue());
            s.child(RELOJ+"/inicio").setValue(inicio+100);
            Object reloj=s.child(RELOJ).getValue();
            for(int j=0;j<5;j++) assertTrue(VotacionRevision.registrar(s,"p",1,id,"u"+j,"si",inicio+101));
            assertEquals(reloj,s.child(RELOJ).getValue());
            assertFalse(VotacionRevision.registrar(s,"p",1,id,"u1","no",inicio+102));
            cerrar(s,id,"u1",inicio+102);
            assertEquals("propuestas",s.child(RELOJ+"/modo").getValue());
            assertEquals(ServerValue.TIMESTAMP,s.child(RELOJ+"/inicio").getValue());
            assertEquals(5000L,s.child(RELOJ+"/duracionMs").getValue());
            assertFalse(VotacionRevision.resolver(s,"p",1,id,"u1",null,Collections.emptyMap(),inicio+103));
            inicio+=102;s.child(RELOJ+"/inicio").setValue(inicio);
            p.put("turnoRevision",s.child(RELOJ+"/turno").getValue());
            assertFalse(VotacionRevision.crear(s,"p",1,"u2",p,inicio+1));
            assertFalse(RelojRevision.coordinar(s,"p",1,"u2",inicio+4999));
            assertEquals(inicio,s.child(RELOJ+"/inicio").getValue());
        }
    }
    @Test public void intentoSinVotosConservaPlazoYHostDesconectadoNoBloquea() {
        MutableData s=sala(5);Map<String,Object> p=propuesta(s,0,"v");
        assertTrue(VotacionRevision.crear(s,"p",1,"u1",p,9000));s.child(RELOJ+"/inicio").setValue(9000);
        s.child("jugadores/u0/estaConectado").setValue(false);
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","u2","si",14000));
        cerrar(s,"v","u1",14000);
        assertEquals(4500L,s.child(RELOJ+"/inicio").getValue());
        assertEquals(5000L,s.child(RELOJ+"/duracionMs").getValue());
        assertFalse(VotacionRevision.crear(s,"p",1,"u2",propuesta(s,1,"otra"),14000));
        assertTrue(RelojRevision.coordinar(s,"p",1,"u1",14000));
        assertEquals("resultado",s.child(RELOJ+"/modo").getValue());s.child(RELOJ+"/inicio").setValue(14000);
        assertFalse(RelojRevision.coordinar(s,"p",1,"u1",16999));
        assertTrue(RelojRevision.coordinar(s,"p",1,"u1",17000));
        assertEquals(1L,s.child(RelojRevision.ruta(1)+"/indiceActual").getValue());
    }
    @Test public void todosResueltosPresentanTresSegundosSinNuevaSeleccion() {
        MutableData s=sala(1);assertTrue(VotacionRevision.crear(s,"p",1,"u0",propuesta(s,0,"v"),4501));
        s.child(RELOJ+"/inicio").setValue(4501);
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","u0","si",4502));cerrar(s,"v","u0",4502);
        assertEquals("resultado",s.child(RELOJ+"/modo").getValue());
        s.child(RELOJ+"/inicio").setValue(4502);
        assertFalse(RelojRevision.coordinar(s,"p",1,"u0",7501));assertTrue(RelojRevision.coordinar(s,"p",1,"u0",7502));
    }
    @Test public void solicitudesSimuladasDistintasYAntiguasNoReinicianTurno() {
        MutableData s=sala(8);Map<String,Object> a=propuesta(s,0,"a"),b=propuesta(s,1,"b");
        assertEquals(10000L,s.child(RELOJ+"/duracionMs").getValue());
        assertTrue(VotacionRevision.crear(s,"p",1,"u1",a,4501));Object antes=s.getValue();
        assertFalse(VotacionRevision.crear(s,"p",1,"u2",b,4501));assertEquals(antes,s.getValue());
        s.child(RELOJ+"/inicio").setValue(4501);
        assertTrue(VotacionRevision.registrar(s,"p",1,"a","u1","si",4502));cerrar(s,"a","u2",14501);
        s.child(RELOJ+"/inicio").setValue(14501);
        assertFalse(VotacionRevision.crear(s,"p",1,"u2",b,14502));
        assertEquals(14501L,s.child(RELOJ+"/inicio").getValue());
    }
}
