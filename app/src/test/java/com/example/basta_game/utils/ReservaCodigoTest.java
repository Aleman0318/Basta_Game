package com.example.basta_game.utils;

import com.google.firebase.database.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReservaCodigoTest {
    @Test public void limpiezaNoBorraOtraSalaNiPartidaActivaNiOtrosJugadores() {
        MutableData s=MutableDataLocal.desde(new java.util.HashMap<>());
        s.child("idSala").setValue("a");s.child("hostUID").setValue("h");s.child("estadoPartida").setValue("En espera");
        s.child("jugadores/h/nombre").setValue("H");
        assertFalse(ReservaCodigo.borrarSalaParcial(s,"otra","h"));
        s.child("estadoPartida").setValue("En juego");assertFalse(ReservaCodigo.borrarSalaParcial(s,"a","h"));
        s.child("estadoPartida").setValue("En espera");s.child("jugadores/b/nombre").setValue("B");
        assertFalse(ReservaCodigo.borrarSalaParcial(s,"a","h"));s.child("jugadores/b").setValue(null);
        assertTrue(ReservaCodigo.borrarSalaParcial(s,"a","h"));assertNull(s.getValue());
    }
    @Test public void colisionYReintentoSoloAdmitenUnPropietario() {
        MutableData c=MutableDataLocal.desde(null);
        assertTrue(ReservaCodigo.reservar(c,"ABC123","salaA"));
        Object antes=c.getValue();assertFalse(ReservaCodigo.reservar(c,"ABC123","salaB"));assertEquals(antes,c.getValue());
        assertFalse(ReservaCodigo.liberar(c,"salaB"));
        assertTrue(ReservaCodigo.activar(c,"salaA"));assertFalse(ReservaCodigo.liberar(c,"salaA"));
    }
    @Test public void reservaFallidaSeLiberaSinBorrarLaSiguiente() {
        MutableData c=MutableDataLocal.desde(null);
        assertTrue(ReservaCodigo.reservar(c,"ABC123","a"));assertTrue(ReservaCodigo.liberar(c,"a"));
        assertTrue(ReservaCodigo.reservar(c,"ABC123","b"));assertFalse(ReservaCodigo.liberar(c,"a"));
        assertEquals("b",c.child("idSala").getValue());
    }
}
