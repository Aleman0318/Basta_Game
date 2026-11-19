package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresenciaSalaTest {
    @Test public void sesionAntiguaNoDesconectaNiExpulsaSesionActiva() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("jugadores/u/estaConectado").setValue(true);
        s.child("jugadores/u/estaListo").setValue(true);
        s.child("sesiones/u/antigua/conectada").setValue(false);
        s.child("sesiones/u/antigua/desconectadoDesde").setValue(1000);
        s.child("sesiones/u/actual/conectada").setValue(true);
        PresenciaSala.actualizar(s);
        assertTrue(PresenciaSala.conectada(s,"u"));
        assertEquals(true,s.child("jugadores/u/estaListo").getValue());
        TransferenciaHost.expirar(s,100000,90000);
        assertTrue(s.child("jugadores").hasChild("u"));
    }
    @Test public void ultimaSesionLimpiaListoYReconexionExigeConfirmacion() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("jugadores/u/estaConectado").setValue(true);s.child("jugadores/u/estaListo").setValue(true);
        s.child("sesiones/u/a/conectada").setValue(false);s.child("sesiones/u/a/desconectadoDesde").setValue(1000);
        assertTrue(PresenciaSala.actualizar(s));
        assertEquals(false,s.child("jugadores/u/estaListo").getValue());
        assertEquals(1000L,s.child("jugadores/u/desconectadoDesde").getValue());
        s.child("sesiones/u/b/conectada").setValue(true);PresenciaSala.actualizar(s);
        assertEquals(true,s.child("jugadores/u/estaConectado").getValue());
        assertEquals(false,s.child("jugadores/u/estaListo").getValue());
        assertNull(s.child("jugadores/u/desconectadoDesde").getValue());
    }
}
