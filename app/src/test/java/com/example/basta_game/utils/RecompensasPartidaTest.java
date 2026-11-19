package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecompensasPartidaTest {
    private MutableData usuario() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("uid", "jugador");
        datos.put("monedas", 20L);
        datos.put("partidasJugadas", 2L);
        datos.put("partidasGanadas", 1L);
        datos.put("puntosTotalesHistoricos", 80L);
        datos.put("avatarID", "avatar_01");
        datos.put("campoFuturo", "conservar");
        return MutableDataLocal.desde(datos);
    }

    private Map<String, Object> historial(String id) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("idPartida", id);
        datos.put("miUid", "jugador");
        datos.put("miTotal", 50L);
        datos.put("miPosicion", 1L);
        datos.put("monedas", 100L);
        datos.put("fecha", 1000L);
        return datos;
    }

    private long numero(MutableData datos, String ruta) {
        return ((Number) datos.child(ruta).getValue()).longValue();
    }

    @Test public void historialMonedasYMarcaSeGuardanJuntos() {
        MutableData datos = usuario();
        assertTrue(RecompensasPartida.aplicar(datos, "partida1", historial("partida1")));
        assertEquals(120L, numero(datos, "monedas"));
        assertEquals(3L, numero(datos, "partidasJugadas"));
        assertEquals(2L, numero(datos, "partidasGanadas"));
        assertEquals(130L, numero(datos, "puntosTotalesHistoricos"));
        assertEquals("partida1", datos.child("historialPartidas/partida1/idPartida").getValue());
        assertEquals(true, datos.child("recompensasAplicadas/partida1").getValue());
        assertEquals("conservar", datos.child("campoFuturo").getValue());
    }

    @Test public void eventoRepetidoNoVuelveAIncrementar() {
        MutableData datos = usuario();
        RecompensasPartida.aplicar(datos, "partida1", historial("partida1"));
        Object antes = datos.getValue();
        assertFalse(RecompensasPartida.aplicar(datos, "partida1", historial("partida1")));
        assertEquals(antes, datos.getValue());
    }

    @Test public void reentradaConOtraInstanciaConservaLaProteccion() {
        MutableData datos = usuario();
        RecompensasPartida.aplicar(datos, "partida1", historial("partida1"));
        MutableData reentrada = MutableDataLocal.desde(datos.getValue());
        Map<String, Object> distinto = historial("partida1");
        distinto.put("monedas", 999L);
        assertFalse(RecompensasPartida.aplicar(reentrada, "partida1", distinto));
        assertEquals(datos.getValue(), reentrada.getValue());
    }

    @Test public void recortarHistorialNoEliminaMarca() {
        MutableData datos = usuario();
        RecompensasPartida.aplicar(datos, "partida1", historial("partida1"));
        datos.child("historialPartidas/partida1").setValue(null);
        assertFalse(RecompensasPartida.aplicar(datos, "partida1", historial("partida1")));
        assertEquals(120L, numero(datos, "monedas"));
    }

    @Test public void otraPartidaEnLaMismaSalaPuedePagar() {
        MutableData datos = usuario();
        assertTrue(RecompensasPartida.aplicar(datos, "partida1", historial("partida1")));
        assertTrue(RecompensasPartida.aplicar(datos, "partida2", historial("partida2")));
        assertEquals(220L, numero(datos, "monedas"));
        assertEquals(4L, numero(datos, "partidasJugadas"));
    }

    @Test public void transaccionRechazadaNoPublicaNingunCambioYPermiteReintento() {
        MutableData servidor = usuario();
        Object antes = servidor.getValue();
        MutableData intento = MutableDataLocal.desde(antes);
        assertTrue(RecompensasPartida.aplicar(intento, "partida1", historial("partida1")));
        // Simula rechazo del commit: el snapshot de trabajo se descarta entero.
        assertEquals(antes, servidor.getValue());
        assertNull(servidor.child("recompensasAplicadas/partida1").getValue());
        assertTrue(RecompensasPartida.aplicar(servidor, "partida1", historial("partida1")));
        assertEquals(120L, numero(servidor, "monedas"));
    }

    @Test public void dosSolicitudesConcurrentesConRetryLocalSoloPublicanUna() throws Exception {
        AtomicReference<Object> servidor = new AtomicReference<>(usuario().getValue());
        CyclicBarrier ambosLeyeron = new CyclicBarrier(2);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        Callable<Boolean> solicitud = () -> {
            Object leido = servidor.get();
            ambosLeyeron.await(5, TimeUnit.SECONDS);
            while (true) {
                MutableData intento = MutableDataLocal.desde(leido);
                if (!RecompensasPartida.aplicar(intento, "partida1", historial("partida1"))) return false;
                if (servidor.compareAndSet(leido, intento.getValue())) return true;
                leido = servidor.get();
            }
        };
        try {
            Future<Boolean> a = ejecutor.submit(solicitud);
            Future<Boolean> b = ejecutor.submit(solicitud);
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(120L, numero(MutableDataLocal.desde(servidor.get()), "monedas"));
        } finally { ejecutor.shutdownNow(); }
    }

    @Test public void cacheVaciaNoCreaUnUsuarioParcial() {
        MutableData datos = MutableDataLocal.desde(null);
        assertFalse(RecompensasPartida.aplicar(datos, "partida1", historial("partida1")));
        assertNull(datos.getValue());
    }

    @Test public void puntosNegativosYPartidaSinMonedasConservanReglas() {
        MutableData datos = usuario();
        Map<String, Object> resultado = historial("partida1");
        resultado.put("monedas", 0L);
        resultado.put("miTotal", -15L);
        resultado.put("miPosicion", 2L);
        assertTrue(RecompensasPartida.aplicar(datos, "partida1", resultado));
        assertEquals(20L, numero(datos, "monedas"));
        assertEquals(65L, numero(datos, "puntosTotalesHistoricos"));
        assertEquals(1L, numero(datos, "partidasGanadas"));
    }

    @Test public void reintentosDescartadosNoAcumulanCambios() {
        Object base = usuario().getValue();
        for (int i = 0; i < 4; i++) {
            MutableData intento = MutableDataLocal.desde(base);
            assertTrue(RecompensasPartida.aplicar(intento, "partida1", historial("partida1")));
            assertEquals(120L, numero(intento, "monedas"));
        }
        assertEquals(20L, numero(MutableDataLocal.desde(base), "monedas"));
    }

    @Test public void reintentoTrasCompraConservaSaldoInventarioYMarcasAnteriores() {
        MutableData servidor = usuario();
        servidor.child("recompensasAplicadas/anterior").setValue(true);
        MutableData obsoleto = MutableDataLocal.desde(servidor.getValue());
        RecompensasPartida.aplicar(obsoleto, "partida1", historial("partida1"));
        // Simula que una compra gana el commit y obliga a reintentar con este snapshot.
        servidor.child("monedas").setValue(10L);
        servidor.child("inventarioComodines/congelador/cantidad").setValue(1L);
        assertTrue(RecompensasPartida.aplicar(servidor, "partida1", historial("partida1")));
        assertEquals(110L, numero(servidor, "monedas"));
        assertEquals(1L, numero(servidor, "inventarioComodines/congelador/cantidad"));
        assertEquals(true, servidor.child("recompensasAplicadas/anterior").getValue());
        assertEquals("avatar_01", servidor.child("avatarID").getValue());
    }

    @Test public void payloadAnidadoConTimestampDelServidorEsCompatible() {
        MutableData datos = usuario();
        Map<String, Object> resultado = historial("partida1");
        Map<String, Object> top = new HashMap<>();
        top.put("puesto_1", historial("partida1"));
        resultado.put("top3", top);
        resultado.put("fecha", com.google.firebase.database.ServerValue.TIMESTAMP);
        assertTrue(RecompensasPartida.aplicar(datos, "partida1", resultado));
        assertEquals("jugador", datos.child("historialPartidas/partida1/top3/puesto_1/miUid").getValue());
    }

    @Test public void idAusenteOIncompatibleNoModificaElUsuario() {
        for (String id : new String[]{null, "", "partida/1", "otraPartida"}) {
            MutableData datos = usuario();
            Object antes = datos.getValue();
            try {
                RecompensasPartida.aplicar(datos, id, historial("partida1"));
                fail("Debio rechazar ID invalido o distinto al historial");
            } catch (IllegalArgumentException esperado) { }
            assertEquals(antes, datos.getValue());
        }
    }

    @Test public void usuarioDistintoNoPuedeRecibirElHistorial() {
        MutableData datos = usuario();
        Object antes = datos.getValue();
        Map<String, Object> resultado = historial("partida1");
        resultado.put("miUid", "otro");
        try {
            RecompensasPartida.aplicar(datos, "partida1", resultado);
            fail("Debio rechazar un UID distinto");
        } catch (IllegalArgumentException esperado) { }
        assertEquals(antes, datos.getValue());
    }

    @Test public void overflowNoDejaHistorialNiMarcaParcial() {
        MutableData datos = usuario();
        datos.child("puntosTotalesHistoricos").setValue(Long.MAX_VALUE);
        Object antes = datos.getValue();
        try {
            RecompensasPartida.aplicar(datos, "partida1", historial("partida1"));
            fail("Debio rechazar overflow antes de modificar el snapshot");
        } catch (ArithmeticException esperado) { }
        assertEquals(antes, datos.getValue());
    }

    @Test public void saldoCorruptoNoSeSobrescribeConCero() {
        MutableData datos = usuario();
        datos.child("monedas").setValue("saldo_invalido");
        Object antes = datos.getValue();
        try {
            RecompensasPartida.aplicar(datos, "partida1", historial("partida1"));
            fail("Debio rechazar datos corruptos");
        } catch (ClassCastException esperado) { }
        assertEquals(antes, datos.getValue());
    }
}
