package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class FaseJuegoTest {
    @Test public void estadoEnJuegoNoReabreFaseDeResultados() {
        for (String fase : new String[]{"resultados_finales","revision_puntuaciones","lobby"})
            assertFalse(FaseJuego.permiteResponder(fase,"En juego",1));
    }
    @Test public void faseJugandoYRondaActivaPermitenResponder() {
        assertTrue(FaseJuego.permiteResponder("jugando","En juego",1));
        assertTrue(FaseJuego.permiteResponder(null,"En juego",1));
        assertFalse(FaseJuego.permiteResponder("jugando","En juego",0));
    }
}
