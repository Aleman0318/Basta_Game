package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransferenciaHostTest {
    private MutableData sala(String... uids) {
        MutableData sala = MutableDataLocal.desde(new HashMap<>());
        sala.child("hostUID").setValue("h");
        sala.child("hostUid").setValue("h");
        sala.child("hostNombre").setValue("Nombre h");
        sala.child("idPartida").setValue("partida1");
        sala.child("rondaActual").setValue(2);
        sala.child("cantidadRondas").setValue(5);
        sala.child("inicioRonda").setValue(1000L);
        sala.child("fase").setValue("jugando");
        sala.child("estadoPartida").setValue("En juego");
        sala.child("puntajesRondas/ronda_1/b").setValue(50);
        for (String uid : uids) {
            sala.child("jugadores/" + uid + "/nombre").setValue("Nombre " + uid);
            sala.child("jugadores/" + uid + "/estaConectado").setValue(true);
            sala.child("jugadores/" + uid + "/esHost").setValue(uid.equals("h"));
        }
        return sala;
    }

    private void corona(MutableData sala, String uid) {
        assertEquals(uid, sala.child("hostUID").getValue());
        assertEquals(uid, sala.child("hostUid").getValue());
        int coronas = 0;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            if (Boolean.TRUE.equals(jugador.child("esHost").getValue())) {
                coronas++;
                assertEquals(uid, jugador.getKey());
            }
        }
        assertEquals(uid == null ? 0 : 1, coronas);
    }

    @Test public void abandonoHostTransfiereSinReiniciarRonda() {
        MutableData sala = sala("h", "c", "b");
        assertTrue(TransferenciaHost.abandonar(sala, "h"));
        corona(sala, "b");
        assertNull(sala.child("jugadores/h").getValue());
        assertEquals(2L, sala.child("rondaActual").getValue());
        assertEquals(1000L, sala.child("inicioRonda").getValue());
        assertEquals("jugando", sala.child("fase").getValue());
        assertEquals("partida1", sala.child("idPartida").getValue());
        assertEquals(50L, sala.child("puntajesRondas/ronda_1/b").getValue());
    }

    @Test public void abandonoNoHostNoCambiaCorona() {
        MutableData sala = sala("h", "b", "c");
        TransferenciaHost.abandonar(sala, "b");
        corona(sala, "h");
    }

    @Test public void salidaRepetidaNoTocaResultados() {
        MutableData sala = sala("h", "b", "c");
        TransferenciaHost.abandonar(sala, "h");
        Object antes = sala.getValue();
        assertFalse(TransferenciaHost.abandonar(sala, "h"));
        assertEquals(antes, sala.getValue());
    }

    @Test public void hostAnteriorNoRecuperaCoronaAlVolver() {
        MutableData sala = sala("h", "b", "c");
        TransferenciaHost.abandonar(sala, "h");
        sala.child("jugadores/h/nombre").setValue("Anterior");
        sala.child("jugadores/h/estaConectado").setValue(true);
        sala.child("jugadores/h/esHost").setValue(true);
        TransferenciaHost.reconciliar(sala);
        corona(sala, "b");
    }

    @Test public void candidatosDesconectadosNoRecibenCorona() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/b/estaConectado").setValue(false);
        TransferenciaHost.abandonar(sala, "h");
        corona(sala, "c");
    }

    @Test public void sinCandidatosNoSeEliminaPartidaConParticipantes() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/b/estaConectado").setValue(false);
        sala.child("jugadores/c/estaConectado").setValue(false);
        TransferenciaHost.abandonar(sala, "h");
        corona(sala, null);
        assertEquals(2L, sala.child("jugadores").getChildrenCount());
        assertEquals("jugando", sala.child("fase").getValue());
    }

    @Test public void ultimoParticipanteCierraSinBonoYUnaSolaVez() {
        MutableData sala = sala("h", "b");
        TransferenciaHost.abandonar(sala, "h");
        assertEquals("resultados_finales", sala.child("fase").getValue());
        assertEquals(2L, sala.child("cantidadRondas").getValue());
        assertEquals(true, sala.child("resumenRondaListo/ronda_2").getValue());
        assertEquals(50L, sala.child("puntajesRondas/ronda_1/b").getValue());
        assertNull(sala.child("puntajesRondas/ronda_2/b").getValue());
        Object antes = sala.getValue();
        assertFalse(TransferenciaHost.reconciliar(sala));
        assertEquals(antes, sala.getValue());
    }

    @Test public void sinJugadoresSeRetiraSala() {
        MutableData sala = sala("h");
        TransferenciaHost.abandonar(sala, "h");
        assertNull(sala.getValue());
    }

    @Test public void graciaYReconexionNoExpulsanNiDevuelvenCorona() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/h/estaConectado").setValue(false);
        sala.child("jugadores/h/desconectadoDesde").setValue(1000L);
        assertFalse(TransferenciaHost.expirar(sala, 30999L, 30000L));
        assertTrue(TransferenciaHost.expirar(sala, 31000L, 30000L));
        corona(sala, "b");
        assertNull(sala.child("jugadores/h").getValue());
    }

    @Test public void snapshotObsoletoNoExpulsaAQuienYaReconecto() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/h/desconectadoDesde").setValue(1000L);
        sala.child("jugadores/h/estaConectado").setValue(true);
        assertFalse(TransferenciaHost.expirar(sala, 50000L, 30000L));
        corona(sala, "h");
    }

    @Test public void dosSalidasConcurrentesConRetryLocalConvergen() throws Exception {
        AtomicReference<Object> servidor = new AtomicReference<>(sala("h", "b", "c", "d").getValue());
        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = executor.submit(() -> salirConCAS(servidor, barrera, "h"));
            Future<?> b = executor.submit(() -> salirConCAS(servidor, barrera, "b"));
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
            MutableData finalSala = MutableDataLocal.desde(servidor.get());
            corona(finalSala, "c");
            assertEquals(2L, finalSala.child("jugadores").getChildrenCount());
        } finally { executor.shutdownNow(); }
    }

    @Test public void salidaRetiraVotosYListosPeroConservaPuntos() {
        MutableData sala = sala("h", "b", "c");
        sala.child("votacionActiva/ronda_2/votos/h").setValue("si");
        sala.child("revisionLista/ronda_2/h").setValue(true);
        sala.child("entradaPartida/ronda_2/h/dentro").setValue(true);
        TransferenciaHost.abandonar(sala, "h");
        assertNull(sala.child("votacionActiva/ronda_2/votos/h").getValue());
        assertNull(sala.child("revisionLista/ronda_2/h").getValue());
        assertNull(sala.child("entradaPartida/ronda_2/h").getValue());
        assertEquals(50L, sala.child("puntajesRondas/ronda_1/b").getValue());
    }

    @Test public void abandonoEnDueloUsaElResultadoDeDesconexionExistente() {
        MutableData sala = sala("h", "b", "c");
        sala.child("minijuegoActual/activo").setValue(true);
        sala.child("minijuegoActual/estado").setValue("jugando");
        sala.child("minijuegoActual/uidAtacante").setValue("h");
        sala.child("minijuegoActual/uidVictima").setValue("c");
        TransferenciaHost.abandonar(sala, "h");
        assertEquals("terminado", sala.child("minijuegoActual/estado").getValue());
        assertEquals("c", sala.child("minijuegoActual/ganadorUid").getValue());
        assertEquals(true, sala.child("minijuegoActual/porDesconexion").getValue());
    }

    @Test public void unaNuevaDesconexionRenuevaSuPropioPlazo() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/h/estaConectado").setValue(false);
        sala.child("jugadores/h/desconectadoDesde").setValue(40000L);
        assertFalse(TransferenciaHost.expirar(sala, 50000L, 30000L));
        assertNotNull(sala.child("jugadores/h").getValue());
        corona(sala, "h");
    }

    private void salirConCAS(AtomicReference<Object> servidor, CyclicBarrier barrera, String uid) {
        try {
            Object base = servidor.get();
            barrera.await(5, TimeUnit.SECONDS);
            while (true) {
                MutableData intento = MutableDataLocal.desde(base);
                if (!TransferenciaHost.abandonar(intento, uid)) return;
                if (servidor.compareAndSet(base, intento.getValue())) return;
                base = servidor.get();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test public void graciaDeNoventaSegundosNoExpulsaAntesDelPlazo() {
        MutableData sala = sala("h", "b", "c");
        sala.child("jugadores/c/estaConectado").setValue(false);
        sala.child("jugadores/c/desconectadoDesde").setValue(1000L);
        assertFalse(TransferenciaHost.expirar(sala, 90999L, 90000L));
        assertTrue(sala.child("jugadores").hasChild("c"));
        assertTrue(TransferenciaHost.expirar(sala, 91000L, 90000L));
        assertFalse(sala.child("jugadores").hasChild("c"));
    }
}
