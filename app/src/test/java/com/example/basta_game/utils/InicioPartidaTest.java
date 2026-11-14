package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class InicioPartidaTest {
    private MutableData sala() {
        MutableData datos = MutableDataLocal.desde(new HashMap<>());
        datos.child("estadoPartida").setValue("En espera");
        datos.child("hostUID").setValue("host");
        datos.child("jugadores/host/estaListo").setValue(true);
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
        sala.child("idPartida").setValue("actual");
        sala.child("fase").setValue("revision_puntuaciones");
        sala.child("rondaActual").setValue(1);
        sala.child("revisionLista/ronda_1/host").setValue(true);
        return sala;
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
