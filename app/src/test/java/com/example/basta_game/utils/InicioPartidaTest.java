package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class InicioPartidaTest {
    @Test public void cambioDeHostConservaOrdenDeLetrasYPartidaNuevaLoReemplaza() {
        MutableData s=sala(); Map<String,Object> inicio=cambios();
        java.util.List<String> plan=LetraSala.nuevaPartida();inicio.put("letrasPartida",plan);
        assertTrue(InicioPartida.aplicar(s,"p","host",inicio));
        assertEquals(plan.get(0),s.child("letraActual").getValue());
        assertEquals(10000L,s.child("preparacionInicialMs").getValue());
        s.child("hostUID").setValue("otro");s.child("fase").setValue("revision_puntuaciones");
        s.child("revisionLista/ronda_1/host").setValue(true);s.child("revisionLista/ronda_1/otro").setValue(true);
        Map<String,Object> siguiente=cambios();siguiente.put("rondaActual",2);siguiente.put("letraActual","Z");
        assertTrue(InicioPartida.avanzar(s,"otro","p",1,siguiente));
        assertEquals(plan.get(1),s.child("letraActual").getValue());
        s.child("estadoPartida").setValue("En espera");
        s.child("jugadores/host/estaListo").setValue(true);s.child("jugadores/otro/estaListo").setValue(true);
        java.util.List<String> nuevo=new java.util.ArrayList<>(plan);java.util.Collections.rotate(nuevo,1);
        inicio.put("letrasPartida",nuevo);
        assertTrue(InicioPartida.aplicar(s,"p2","otro",inicio));
        assertEquals(nuevo.get(0),s.child("letraActual").getValue());
    }
    private MutableData sala() {
        MutableData datos = MutableDataLocal.desde(new HashMap<>());
        datos.child("estadoPartida").setValue("En espera");
        datos.child("hostUID").setValue("host");
        datos.child("jugadores/host/estaListo").setValue(true);
        datos.child("jugadores/host/estaConectado").setValue(true);
        datos.child("jugadores/otro/estaListo").setValue(true);
        datos.child("jugadores/otro/estaConectado").setValue(true);
        datos.child("jugadores/host/nombre").setValue("Anfitrion");
        datos.child("tiempoRonda").setValue(60);
        return datos;
    }

    private Map<String, Object> cambios() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("estadoPartida", "En juego");
        datos.put("fase", "jugando");
        datos.put("rondaActual", 1);
        datos.put("puntajesRondas", null);
        return datos;
    }

    @Test public void partidaNuevaEliminaEscudosYEspejosViejosSinPerderJugadores() {
        MutableData sala = sala();
        sala.child("poderesRonda/ronda_1/idPartida").setValue("partidaVieja");
        sala.child("poderesRonda/ronda_1/escudos/host/activo").setValue(true);
        sala.child("efectosVisuales/espejo/ronda_1/otro/idPartida").setValue("partidaVieja");
        assertTrue(InicioPartida.aplicar(sala,"partidaNueva","host",cambios()));
        assertNull(sala.child("poderesRonda").getValue());
        assertNull(sala.child("efectosVisuales").getValue());
        assertTrue(sala.child("jugadores").hasChild("host"));
        assertTrue(sala.child("jugadores").hasChild("otro"));
        assertEquals("partidaNueva",sala.child("idPartida").getValue());
    }

    @Test public void nuevaPartidaLimpiaOtrosComodinesDeSesionAbandonada() {
        MutableData sala=sala();
        sala.child("poderesRonda/ronda_1/escudos/host/idPartida").setValue("anterior");
        sala.child("comodinesActivos/ronda_1/congelador/activo").setValue(true);
        sala.child("comodinesPendientes/ronda_1/host/doble_o_nada").setValue(true);
        sala.child("comodinesResultados/ronda_1/flujo1/resultado").setValue("intercambio");
        sala.child("eventoCaosGlobal/ronda_1/idUso").setValue("antiguo");
        sala.child("minijuegoActual/activo").setValue(false);
        sala.child("prestamosImpuesto/uso/estado").setValue("pendiente");
        sala.child("usosCongeladorPartida").setValue(2);
        assertTrue(InicioPartida.aplicar(sala,"nueva","host",cambios()));
        for(String ruta:new String[]{"poderesRonda","comodinesActivos","comodinesPendientes",
                "comodinesResultados","eventoCaosGlobal","minijuegoActual","prestamosImpuesto"})
            assertNull(ruta,sala.child(ruta).getValue());
        assertEquals(0,((Number)sala.child("usosCongeladorPartida").getValue()).intValue());
        assertEquals("nueva",sala.child("idPartida").getValue());
    }

    @Test public void siguienteRondaReiniciaEfectosSinBorrarResumenDeLaAnterior() {
        MutableData sala=revision();
        sala.child("comodinesUsados/ronda_1/x/tipo").setValue("congelador");
        sala.child("efectosVisuales/espejo/ronda_1/u/idUso").setValue("espejo");
        sala.child("poderesRonda/ronda_1/idPartida").setValue("actual");
        sala.child("poderesRonda/ronda_2/escudos/host/activo").setValue(true);
        sala.child("comodinesPendientes/ronda_2/host/doble_o_nada").setValue(true);
        sala.child("minijuegoActual/activo").setValue(false);
        sala.child("usosCongeladorPartida").setValue(1);
        Map<String,Object> siguiente=cambios();
        siguiente.put("rondaActual",2);siguiente.put("fase","jugando");
        assertTrue(InicioPartida.avanzar(sala,"host","actual",1,siguiente));
        assertNull(sala.child("poderesRonda/ronda_2").getValue());
        assertNull(sala.child("comodinesPendientes/ronda_2").getValue());
        assertNull(sala.child("minijuegoActual").getValue());
        assertNotNull(sala.child("poderesRonda/ronda_1").getValue());
        assertNotNull(sala.child("comodinesUsados/ronda_1").getValue());
        assertNotNull(sala.child("efectosVisuales/espejo/ronda_1").getValue());
        assertEquals(1,((Number)sala.child("usosCongeladorPartida").getValue()).intValue());
    }

    @Test public void inicioDuplicadoConservaElIdDelPrimero() {
        MutableData sala = sala();
        assertTrue(InicioPartida.aplicar(sala, "partida1", "host", cambios()));
        Object antes = sala.getValue();
        assertFalse(InicioPartida.aplicar(sala, "partida2", "host", cambios()));
        assertEquals(antes, sala.getValue());
        assertEquals("partida1", sala.child("idPartida").getValue());
    }

    @Test public void dosCandidatosSimulanConflictoYReintentoConUnSoloId() {
        MutableData servidor = sala();
        MutableData candidatoB = MutableDataLocal.desde(servidor.getValue());
        assertTrue(InicioPartida.aplicar(candidatoB, "partida2", "host", cambios()));
        assertTrue(InicioPartida.aplicar(servidor, "partida1", "host", cambios()));
        // Candidato B pierde el commit y reejecuta contra el estado confirmado.
        assertFalse(InicioPartida.aplicar(servidor, "partida2", "host", cambios()));
        assertEquals("partida1", servidor.child("idPartida").getValue());
    }

    @Test public void nuevaPartidaReemplazaIdAnteriorSinCambiarConfiguracion() {
        MutableData sala = sala();
        assertTrue(InicioPartida.aplicar(sala, "partida1", "host", cambios()));
        sala.child("estadoPartida").setValue("Esperando jugadores");
        sala.child("jugadores/host/estaListo").setValue(true);
        sala.child("jugadores/otro/estaListo").setValue(true);
        assertTrue(InicioPartida.aplicar(sala, "partida2", "host", cambios()));
        assertEquals("partida2", sala.child("idPartida").getValue());
        assertEquals(60L, sala.child("tiempoRonda").getValue());
        assertEquals("Anfitrion", sala.child("jugadores/host/nombre").getValue());
    }

    @Test public void cambioDeHostOJugadorNoListoAbortaSinCambios() {
        MutableData sala = sala();
        Object antes = sala.getValue();
        assertFalse(InicioPartida.aplicar(sala, "partida1", "intruso", cambios()));
        assertEquals(antes, sala.getValue());
        sala.child("jugadores/host/estaListo").setValue(false);
        antes = sala.getValue();
        assertFalse(InicioPartida.aplicar(sala, "partida1", "host", cambios()));
        assertEquals(antes, sala.getValue());
    }

    @Test public void cacheVaciaNoCreaSalaParcial() {
        MutableData sala = MutableDataLocal.desde(null);
        assertFalse(InicioPartida.aplicar(sala, "partida1", "host", cambios()));
        assertNull(sala.getValue());
    }

    private MutableData revision() {
        MutableData sala = sala();
        sala.child("jugadores/otro").setValue(null);
        sala.child("idPartida").setValue("actual");
        sala.child("fase").setValue("revision_puntuaciones");
        sala.child("rondaActual").setValue(1);
        sala.child("revisionLista/ronda_1/host").setValue(true);
        return sala;
    }

    @Test public void inicioRequiereDosConectadosYPreparados() {
        MutableData s=sala(); s.child("jugadores/otro/estaConectado").setValue(false);
        Object antes=s.getValue();
        assertFalse(InicioPartida.aplicar(s,"p","host",cambios())); assertEquals(antes,s.getValue());
        s.child("jugadores/otro").setValue(null);
        assertFalse(InicioPartida.aplicar(s,"p","host",cambios()));
    }

    @Test public void entradaTardiaNoReiniciaPlazoPublicado() {
        MutableData s=sala(); assertTrue(InicioPartida.aplicar(s,"p","host",cambios()));
        s.child("inicioRonda").setValue(1000);
        assertFalse(InicioPartida.confirmarInicio(s,"p",1,"otro"));
        assertEquals(1000L,s.child("inicioRonda").getValue());
        s.child("inicioRondaListoDe").setValue(null);
        assertTrue(InicioPartida.confirmarInicio(s,"p",1,"otro"));
        assertEquals(1000L,s.child("inicioRonda").getValue());
        assertFalse(InicioPartida.confirmarInicio(s,"vieja",1,"otro"));
    }

    @Test public void expulsarSoloHostEnEsperaYConservaDatos() {
        MutableData s=sala();s.child("configuracion").setValue("conservar");
        assertFalse(InicioPartida.expulsarEnEspera(s,"otro","host"));
        assertFalse(InicioPartida.expulsarEnEspera(s,"host","host"));
        s.child("estadoPartida").setValue("En juego");
        assertFalse(InicioPartida.expulsarEnEspera(s,"host","otro"));
        s.child("estadoPartida").setValue("En espera");
        assertTrue(InicioPartida.expulsarEnEspera(s,"host","otro"));
        assertFalse(s.child("jugadores").hasChild("otro"));
        assertEquals("conservar",s.child("configuracion").getValue());
    }

    @Test public void avanceDuplicadoNoReiniciaRondaNiCambiaLetra() {
        MutableData sala = revision();
        Map<String, Object> nuevo = cambios();
        nuevo.put("rondaActual", 2);
        nuevo.put("letraActual", "B");
        assertTrue(InicioPartida.avanzar(sala, "host", "actual", 1, nuevo));
        Object antes = sala.getValue();
        nuevo.put("letraActual", "C");
        assertFalse(InicioPartida.avanzar(sala, "host", "actual", 1, nuevo));
        assertEquals(antes, sala.getValue());
    }

    @Test public void hostAnteriorNoPuedeAvanzarTrasTransferencia() {
        MutableData sala = revision();
        sala.child("hostUID").setValue("nuevo");
        Object antes = sala.getValue();
        assertFalse(InicioPartida.avanzar(sala, "host", "actual", 1, cambios()));
        assertEquals(antes, sala.getValue());
    }

    @Test public void avanceEsperaSoloAQuienesSiguenParticipando() {
        MutableData sala = revision();
        sala.child("jugadores/otro/estaListo").setValue(false);
        assertFalse(InicioPartida.avanzar(sala, "host", "actual", 1, cambios()));
        sala.child("jugadores/otro").setValue(null);
        assertTrue(InicioPartida.avanzar(sala, "host", "actual", 1, cambios()));
    }

    @Test public void revisionDePartidaAnteriorNoPuedeAvanzarLaNueva() {
        MutableData sala = revision();
        Object antes = sala.getValue();
        assertFalse(InicioPartida.avanzar(sala, "host", "anterior", 1, cambios()));
        assertEquals(antes, sala.getValue());
    }

    @Test public void dosYTresJugadoresAvanzanSoloUnaVezTrasPrepararse() {
        for (int cantidad : new int[]{2, 3}) {
            MutableData sala = revision();
            for (int i = 1; i < cantidad; i++) sala.child("jugadores/u" + i + "/nombre").setValue("Jugador");
            assertFalse(InicioPartida.avanzar(sala, "host", "actual", 1, cambios()));
            for (int i = 1; i < cantidad; i++) sala.child("revisionLista/ronda_1/u" + i).setValue(true);
            sala.child("puntajesRondas/ronda_1/host").setValue(50);
            Map<String, Object> siguiente = new HashMap<>();
            siguiente.put("rondaActual", 2);
            siguiente.put("fase", "jugando");
            assertTrue(InicioPartida.avanzar(sala, "host", "actual", 1, siguiente));
            assertFalse(InicioPartida.avanzar(sala, "host", "actual", 1, siguiente));
            assertEquals(50L, sala.child("puntajesRondas/ronda_1/host").getValue());
        }
    }

    @Test public void resumenTardioNoReabreElCierreDelUltimoJugador() {
        MutableData sala = revision();
        sala.child("estadoPartida").setValue("En juego");
        sala.child("cantidadRondas").setValue(5);
        TransferenciaHost.reconciliar(sala);
        assertEquals("resultados_finales", sala.child("fase").getValue());
        Object antes = sala.getValue();
        assertFalse(InicioPartida.publicarResumen(sala, "host", "actual", 1));
        assertEquals(antes, sala.getValue());
    }

    @Test public void resumenCompruebaPartidaHostYRondaAntesDePublicar() {
        MutableData sala = revision();
        sala.child("jugadores/otro/nombre").setValue("Otro");
        sala.child("cantidadRondas").setValue(3);
        assertFalse(InicioPartida.publicarResumen(sala, "host", "vieja", 1));
        assertFalse(InicioPartida.publicarResumen(sala, "anterior", "actual", 1));
        assertFalse(InicioPartida.publicarResumen(sala, "host", "actual", 2));
        assertTrue(InicioPartida.publicarResumen(sala, "host", "actual", 1));
        assertEquals(true, sala.child("resumenRondaListo/ronda_1").getValue());
    }
}
