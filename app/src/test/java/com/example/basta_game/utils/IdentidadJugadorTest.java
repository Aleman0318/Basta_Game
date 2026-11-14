package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdentidadJugadorTest {
    @Test public void perfilIncompletoNoEscribeJugadorNull() {
        MutableData jugador = MutableDataLocal.desde(null);
        assertFalse(IdentidadJugador.aplicar(jugador, "u", null, null));
        assertFalse(IdentidadJugador.aplicar(jugador, "u", "null", null));
        assertNull(jugador.getValue());
    }
    @Test public void reentradaConservaPuntosCoronaPresenciaYAvatarSiFaltaFoto() {
        MutableData jugador = MutableDataLocal.desde(null);
        assertTrue(IdentidadJugador.aplicar(jugador, "u", "Diego", "foto"));
        jugador.child("puntajePartida").setValue(50);
        jugador.child("esHost").setValue(true);
        jugador.child("desconectadoDesde").setValue(1000);
        jugador.child("campoExtra").setValue("conservar");
        assertTrue(IdentidadJugador.aplicar(jugador, "u", "Diego", null));
        assertEquals("foto", jugador.child("urlAvatar").getValue());
        assertEquals(50L, jugador.child("puntajePartida").getValue());
        assertEquals(true, jugador.child("esHost").getValue());
        assertEquals(1000L, jugador.child("desconectadoDesde").getValue());
        assertEquals("conservar", jugador.child("campoExtra").getValue());
    }
    @Test public void perfilInvalidoNoBorraIdentidadExistente() {
        MutableData jugador = MutableDataLocal.desde(null);
        assertTrue(IdentidadJugador.aplicar(jugador, "u", "Diego", "foto"));
        Object antes = jugador.getValue();
        assertFalse(IdentidadJugador.aplicar(jugador, "u", " ", ""));
        assertEquals(antes, jugador.getValue());
    }
}
