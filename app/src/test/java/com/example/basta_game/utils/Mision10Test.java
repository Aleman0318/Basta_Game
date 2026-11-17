package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Mision10Test {
    @Test public void sinSolicitudesExpiraYPresentaTodosLosPuntosTresSegundos() {
        MutableData s=sala(5);String ruta=RelojRevision.ruta(1)+"/reloj";
        assertTrue(RelojRevision.coordinar(s,"p",1,"u0",1000));s.child(ruta+"/inicio").setValue(1000);
        assertTrue(RelojRevision.coordinar(s,"p",1,"u0",4500));s.child(ruta+"/inicio").setValue(4500);
        assertFalse(RelojRevision.coordinar(s,"p",1,"u1",9499));
        assertTrue(RelojRevision.coordinar(s,"p",1,"u1",9500));
        assertEquals("resultado",s.child(ruta+"/modo").getValue());s.child(ruta+"/inicio").setValue(9500);
        assertFalse(RelojRevision.prepararVoto(s,1,"tardia",9501,"Animal"));
        assertFalse(RelojRevision.coordinar(s,"p",1,"u1",12499));
        assertTrue(RelojRevision.coordinar(s,"p",1,"u1",12500));
        assertEquals(true,s.child(RelojRevision.ruta(1)+"/completa").getValue());
    }
    @Test public void espectadorNoPuedeElegirPorLaVictimaNiCambiarEleccion() {
        MutableData j=MutableDataLocal.desde(new HashMap<>());
        j.child("estado").setValue("jugando");j.child("tipoJuego").setValue("piedra_papel_tijera");
        j.child("uidAtacante").setValue("a");j.child("uidVictima").setValue("v");
        assertFalse(SeleccionMinijuego.elegirPPT(j,"espectador","piedra"));
        assertTrue(SeleccionMinijuego.elegirPPT(j,"a","piedra"));assertFalse(SeleccionMinijuego.elegirPPT(j,"a","papel"));
        assertTrue(SeleccionMinijuego.elegirPPT(j,"v","tijera"));
    }
    private MutableData sala(int n) {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("hostUID").setValue("u0");
        s.child("fase").setValue("jugando");s.child("letraActual").setValue("A");s.child("categoriasActivas").setValue(Collections.singletonList("Animal"));
        for(int i=0;i<n;i++) {
            String u="u"+i;s.child("jugadores/"+u+"/estaConectado").setValue(true);
            s.child("respuestas/ronda_1/"+u+"/Animal").setValue("Abeja "+u);
            s.child("validaciones/ronda_1/"+u+"/Animal/puntos").setValue(10);s.child("validaciones/ronda_1/"+u+"/Animal/correcta").setValue(true);
        }
        return s;
    }
    @Test public void cincoYOchoSolicitudesManualesTienenUnTurnoCadaUnaSinSegundaPreparacion() {
        for(int n:new int[]{5,8}) {
            MutableData s=sala(n);String ruta=RelojRevision.ruta(1)+"/reloj";
            assertTrue(RelojRevision.coordinar(s,"p",1,"u0",1000));s.child(ruta+"/inicio").setValue(1000);
            assertFalse(RelojRevision.coordinar(s,"p",1,"u0",4499));
            assertTrue(RelojRevision.coordinar(s,"p",1,"u0",4500));
            Set<String> resueltas=new HashSet<>();long ahora=5000;
            for(int turno=0;turno<n;turno++) {
                MutableData reloj=s.child(ruta);
                assertEquals("propuestas",reloj.child("modo").getValue());reloj.child("inicio").setValue(ahora);
                String elegido="u"+turno,palabra="Abeja "+elegido;
                Map<String,Object> propuesta=new HashMap<>();
                propuesta.put("activa",true);propuesta.put("idVotacion","v"+turno);propuesta.put("uidObjetivo",elegido);
                propuesta.put("categoria","Animal");propuesta.put("respuesta",palabra);propuesta.put("claveVotacion",ClaveRevision.de(1,elegido,"Animal",palabra));
                propuesta.put("turnoRevision",reloj.child("turno").getValue());
                assertTrue(VotacionRevision.crear(s,"p",1,"u0",propuesta,ahora));
                MutableData v=s.child("votacionActiva/ronda_1");
                assertEquals("votos",reloj.child("modo").getValue());assertNotNull(reloj.child("inicio").getValue());
                assertEquals(n>5 ? 10000L:5000L,reloj.child("duracionMs").getValue());reloj.child("inicio").setValue(ahora);
                String id=v.child("idVotacion").getValue(String.class), clave=v.child("claveVotacion").getValue(String.class), uid=v.child("uidObjetivo").getValue(String.class);
                assertTrue(resueltas.add(uid));
                for(int j=0;j<n;j++) assertTrue(VotacionRevision.registrar(s,"p",1,id,"u"+j,j%2==0 ? "si":"no",ahora+1));
                assertFalse(VotacionRevision.registrar(s,"p",1,id,"u0","no",ahora+2));
                Map<String,Object> cambios=new HashMap<>();cambios.put("votacionActiva/ronda_1",null);
                cambios.put("resultadosVotacionPalabras/ronda_1/"+clave+"/resuelta",true);
                cambios.put("validaciones/ronda_1/"+uid+"/Animal/puntos",n==8 ? 5:10);
                assertTrue(VotacionRevision.resolver(s,"p",1,id,"u0",v.child("votos").getValue(),cambios,ahora+2));
                assertEquals(10L,s.child("validaciones/ronda_1/"+uid+"/Animal/puntos").getValue());
                assertEquals(turno==n-1 ? "resultado":"propuestas",s.child(ruta+"/modo").getValue());
                s.child(ruta+"/inicio").setValue(ahora+2);
                assertFalse(RelojRevision.coordinar(s,"p",1,"u0",ahora+3));
                if(turno==n-1) assertTrue(RelojRevision.coordinar(s,"p",1,"u0",ahora+3002));
                ahora+=1000;
            }
            assertEquals(n,resueltas.size());assertEquals(true,s.child(RelojRevision.ruta(1)+"/completa").getValue());
        }
    }
    @Test public void entradaPendienteExpulsionYCallbackAntiguoNoRecreanJugador() {
        MutableData s=sala(1);s.child("estadoPartida").setValue("En espera");s.child("fase").setValue("espera");s.child("maxJugadores").setValue(8);
        assertTrue(IngresoSala.ingresar(s,"diana","Diana","foto"));
        assertEquals(false,s.child("jugadores/diana/estaConectado").getValue());
        assertNotNull(s.child("jugadores/diana/desconectadoDesde").getValue());
        assertTrue(InicioPartida.expulsarEnEspera(s,"u0","diana"));
        assertFalse(IngresoSala.ingresar(s,"diana","Diana","vieja"));
        s.child("sesiones/diana/antigua/conectada").setValue(true);PresenciaSala.actualizar(s);
        assertFalse(s.child("jugadores").hasChild("diana"));
        assertFalse(InicioPartida.expulsarEnEspera(s,"u0","u0"));
    }
    @Test public void selectorEsUniformeSinRepeticionInmediata() {
        for(String previo:new String[]{null,"tres_en_raya","duelo_reflejos","piedra_papel_tijera"}) {
            Map<String,Integer> conteo=new HashMap<>();
            for(int i=0;i<6;i++){String tipo=SeleccionMinijuego.elegir(previo,i);assertNotEquals(previo,tipo);conteo.put(tipo,conteo.getOrDefault(tipo,0)+1);}
            assertEquals(previo==null ? 3:2,conteo.size());for(int veces:conteo.values()) assertEquals(previo==null ? 2:3,veces);
        }
    }
    @Test public void saltarinesSonIdempotentesConservanSumaYNoTocanHistoria() {
        MutableData s=sala(3);s.child("fase").setValue("revision_puntuaciones");
        s.child("puntajesRondas/ronda_1/u0").setValue(3);s.child("puntajesRondas/ronda_1/u1").setValue(20);
        s.child("puntajesRondas/ronda_1/u2").setValue(0);s.child("puntosHistoricos").setValue(99);
        assertTrue(PuntosSaltarines.aplicar(s,1,"u0","Diego","uso",0,0));
        assertEquals(0L,s.child("puntajesRondas/ronda_1/u0").getValue());assertEquals(23L,s.child("puntajesRondas/ronda_1/u1").getValue());
        Object antes=s.getValue();assertFalse(PuntosSaltarines.aplicar(s,1,"u0","Diego","uso",0,0));assertEquals(antes,s.getValue());
        assertEquals(99L,s.child("puntosHistoricos").getValue());assertEquals(3L,s.child("comodinesResultados/ronda_1/uso/cantidad").getValue());
    }
}
