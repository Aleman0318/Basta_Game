package com.example.basta_game.utils;
import com.google.firebase.database.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class Mision11Test {
    private MutableData sala() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("cantidadRondas").setValue(3);
        s.child("hostUID").setValue("a");s.child("fase").setValue("jugando");s.child("letraActual").setValue("O");
        s.child("estadoPartida").setValue("En juego");s.child("maxJugadores").setValue(5);
        s.child("categoriasActivas").setValue(Collections.singletonList("Animal"));
        for(String uid:Arrays.asList("a","b")) {s.child("jugadores/"+uid+"/estaConectado").setValue(true);s.child("jugadores/"+uid+"/puntajePartida").setValue(20);}
        return s;
    }
    @Test public void estructuraNormalizaAcentosConservandoEnie() {
        assertFalse(ValidacionBasica.correcta("O"," UVA "));assertFalse(ValidacionBasica.correcta("O","Ox"));
        assertFalse(ValidacionBasica.correcta("O","  "));assertTrue(ValidacionBasica.correcta("O"," Órgano "));
        assertTrue(ValidacionBasica.correcta("Ñ"," ñandú "));assertFalse(ValidacionBasica.correcta("N","Ñandú"));
        assertTrue(ValidacionBasica.correcta("Ñ","n\u0303andu"));
    }
    @Test public void estructuraInvalidaEsDefinitivaYSolicitudSeRechaza() {
        MutableData s=sala();s.child("validaciones/ronda_1/a/Animal/puntos").setValue(0);
        assertTrue(EstadoRevision.definitiva("Uva",true,0,false,false));
        Map<String,Object> p=new HashMap<>();p.put("respuesta","Uva");p.put("uidObjetivo","a");p.put("categoria","Animal");p.put("claveVotacion","k");p.put("idVotacion","v");p.put("activa",true);
        assertFalse(VotacionRevision.crear(s,"p",1,"b",p,1000));
    }
    @Test public void cuentaAusentesElegiblesSinEspectadoresNiAfectarMayoria() {
        Map<String,Object> elegibles=new HashMap<>();for(int i=0;i<5;i++) elegibles.put("u"+i,true);
        Map<String,Object> votos=new HashMap<>();votos.put("u0","si");votos.put("u1","si");votos.put("u2","no");votos.put("espectador","no");
        assertArrayEquals(new int[]{2,1,2},ConteoVotos.contar(elegibles,votos));
        assertEquals("si",ResultadoVotos.resolver(2,1));
    }
    @Test public void tomaDeHostConservaReservaYNoDevuelveCoronaAlReconectar() {
        MutableData s=sala();s.child("jugadores/a/estaConectado").setValue(false);s.child("jugadores/a/desconectadoDesde").setValue(1000);
        assertTrue(TransferenciaHost.reconciliar(s));assertEquals("b",s.child("hostUID").getValue());assertTrue(s.child("jugadores").hasChild("a"));
        s.child("jugadores/a/estaConectado").setValue(true);TransferenciaHost.reconciliar(s);assertEquals("b",s.child("hostUID").getValue());
    }
    @Test public void cierreAtomicoNoDependeDeHostYNoSumaDosVeces() {
        MutableData s=sala();s.child("jugadores/a/estaConectado").setValue(false);s.child("revisionCategorias/ronda_1/completa").setValue(true);
        s.child("validaciones/ronda_1/b/Animal/puntos").setValue(10);
        assertTrue(CierreRevision.aplicar(s,"p",1,"b"));assertEquals(30L,s.child("jugadores/b/puntajePartida").getValue());
        assertEquals("p",s.child("rendimientoRondas/ronda_1/idPartida").getValue());
        assertEquals(10L,s.child("rendimientoRondas/ronda_1/puntos/b").getValue());
        s.child("validaciones/ronda_1/b/Animal/puntos").setValue(0);
        assertEquals(true,s.child("resumenRondaListo/ronda_1").getValue());assertEquals("revision_puntuaciones",s.child("fase").getValue());
        assertFalse(CierreRevision.aplicar(s,"p",1,"a"));assertFalse(CierreRevision.aplicar(s,"p",1,"b"));
        assertEquals(30L,s.child("jugadores/b/puntajePartida").getValue());
        assertEquals(10L,s.child("rendimientoRondas/ronda_1/puntos/b").getValue());
    }
    @Test public void reconexionNoOcupaOtraPlazaYPartidaActivaNoAdmiteNuevo() {
        MutableData s=sala();assertTrue(IngresoSala.ingresar(s,"a","Ana","foto"));assertEquals(2,s.child("jugadores").getChildrenCount());
        assertFalse(IngresoSala.ingresar(s,"nuevo","Nuevo","foto"));
        s.child("expulsados/a").setValue(true);assertFalse(IngresoSala.ingresar(s,"a","Ana","foto"));
    }
    @Test public void esperaResultadosIgnoraSoloDesconexionConfirmada() {
        MutableData s=sala();s.child("fase").setValue("revision_puntuaciones");s.child("hostUID").setValue("b");
        s.child("jugadores/a/estaConectado").setValue(false);s.child("revisionLista/ronda_1/b").setValue(true);
        Map<String,Object> siguiente=new HashMap<>();siguiente.put("rondaActual",2);siguiente.put("fase","jugando");
        assertTrue(InicioPartida.avanzar(s,"b","p",1,siguiente));assertFalse(InicioPartida.avanzar(s,"a","p",1,siguiente));
    }
    @Test public void reservaExpiradaNoAdmiteReconexionNiCreaOtraPlaza() {
        MutableData s=sala();s.child("jugadores/a/estaConectado").setValue(false);s.child("jugadores/a/desconectadoDesde").setValue(1000);
        assertTrue(IngresoSala.ingresar(s,"a","Ana","foto",90999,90000));
        assertFalse(IngresoSala.ingresar(s,"a","Ana","foto",91000,90000));
        assertEquals(2,s.child("jugadores").getChildrenCount());
        Map<String,Object> sesiones=new HashMap<>();sesiones.put("antigua",Collections.singletonMap("conectada",false));
        sesiones.put("actual",Collections.singletonMap("conectada",true));
        assertTrue(IngresoSala.reservaVigente(false,1000,sesiones,91000,90000));
        sesiones.remove("actual");assertFalse(IngresoSala.reservaVigente(false,1000,sesiones,91000,90000));
    }
    @Test public void respuestaPendienteSeDescartaTrasCambioDeRondaORevision() {
        MutableData s=sala();Map<String,String> r=Collections.singletonMap("Animal","Oso");
        assertFalse(InicioPartida.enviarRespuestas(s,"otra",1,"a",r));
        assertTrue(InicioPartida.enviarRespuestas(s,"p",1,"a",r));assertFalse(InicioPartida.enviarRespuestas(s,"p",1,"a",r));
        s.child("revisionCategorias/ronda_1/reloj/modo").setValue("revision");
        assertFalse(InicioPartida.enviarRespuestas(s,"p",1,"b",r));
        assertNull(s.child("respuestas/ronda_1/b").getValue());
    }
    @Test public void cincoEfectosDeRuletaTienenReciboUnicoSinBorradoTotal() {
        for(int efecto=0;efecto<5;efecto++) {
            MutableData s=sala();s.child("fase").setValue("revision_puntuaciones");
            s.child("puntajesRondas/ronda_1/a").setValue(15);s.child("puntajesRondas/ronda_1/b").setValue(10);
            assertTrue(EfectoRuleta.aplicar(s,1,"a","Ana","uso",efecto,0,0));Object antes=s.getValue();
            assertFalse(EfectoRuleta.aplicar(s,1,"a","Ana","uso",efecto,0,0));assertEquals(antes,s.getValue());
            assertTrue(RelojRevision.numero(s.child("puntajesRondas/ronda_1/a").getValue(),-1)>0);
            assertTrue(RelojRevision.numero(s.child("puntajesRondas/ronda_1/b").getValue(),-1)>0);
            assertNotNull(s.child("eventoCaosGlobal/ronda_1/timestamp").getValue());
        }
    }
    @Test public void cierreConservaDobleONadaSinDuplicarlo() {
        for(int base:new int[]{5,10}) {
            MutableData s=sala();s.child("revisionCategorias/ronda_1/completa").setValue(true);
            s.child("validaciones/ronda_1/b/Animal/puntos").setValue(base);
            s.child("comodinesPendientes/ronda_1/b/doble_o_nada").setValue(true);
            assertTrue(CierreRevision.aplicar(s,"p",1,"b"));
            assertEquals((long)(base==10 ? 20 : -10),s.child("puntajesRondas/ronda_1/b").getValue());
            assertFalse(CierreRevision.aplicar(s,"p",1,"a"));
        }
    }
    @Test public void abandonoNoBorraUnVotoYaEmitido() {
        MutableData s=sala();s.child("jugadores/c/estaConectado").setValue(true);
        s.child("votacionActiva/ronda_1/votos/a").setValue("si");
        assertTrue(TransferenciaHost.abandonar(s,"a"));
        assertEquals("si",s.child("votacionActiva/ronda_1/votos/a").getValue());
    }
    @Test public void mayoriaNoConvierteRespuestaEstructuralInvalidaEnDiezPuntos() {
        MutableData s=sala();Map<String,Object> voto=new HashMap<>();
        voto.put("activa",true);voto.put("idVotacion","v");voto.put("uidObjetivo","a");voto.put("claveVotacion","k");voto.put("categoria","Animal");voto.put("respuesta","Uva");
        voto.put("votos",Collections.singletonMap("b","si"));s.child("votacionActiva/ronda_1").setValue(voto);
        Map<String,Object> cambios=new HashMap<>();cambios.put("votacionActiva/ronda_1",null);cambios.put("validaciones/ronda_1/a/Animal/puntos",10);
        assertTrue(VotacionRevision.resolver(s,"p",1,"v","a",Collections.singletonMap("b","si"),cambios,10000));
        assertEquals(0L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        assertEquals(false,s.child("resultadosVotacionPalabras/ronda_1/k/correctaFinal").getValue());
    }
    @Test public void listoAntiguoNoModificaPartidaNiRecreaJugadorExpulsado() {
        MutableData s=sala();assertFalse(InicioPartida.marcarListo(s,"a",true));
        s.child("fase").setValue("lobby");s.child("estadoPartida").setValue("En espera");
        assertTrue(InicioPartida.marcarListo(s,"a",true));s.child("expulsados/a").setValue(true);
        assertFalse(InicioPartida.marcarListo(s,"a",true));s.child("jugadores/a").setValue(null);
        assertFalse(InicioPartida.marcarListo(s,"a",true));assertFalse(s.child("jugadores").hasChild("a"));
    }
}
