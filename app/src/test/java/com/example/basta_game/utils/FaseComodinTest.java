package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class FaseComodinTest {
    @Test public void juegoSoloMientrasResponden() {
        for (String id : new String[]{"congelador","doble_o_nada","decreto_silencio"}) {
            assertTrue(FaseComodin.disponible(id,"jugando",3,3));
            assertFalse(FaseComodin.disponible(id,"revision_puntuaciones",1,3));
            assertFalse(FaseComodin.disponible(id,"resultados_finales",3,3));
        }
    }
    @Test public void resultadosSoloSiQuedaOtraRondaJugable() {
        for (String id : new String[]{"uno_reversa","ruleta_caotica","marca_de_la_muerte"}) {
            assertTrue(FaseComodin.disponible(id,"revision_puntuaciones",1,3));
            assertFalse(FaseComodin.disponible(id,"revision_puntuaciones",3,3));
            assertFalse(FaseComodin.disponible(id,"jugando",1,3));
            assertFalse(FaseComodin.disponible(id,"resultados_finales",1,3));
        }
    }
    @Test public void lobbyDesconocidosYRondasInvalidasNoActivan() {
        assertFalse(FaseComodin.disponible("congelador","lobby",0,3));
        assertFalse(FaseComodin.disponible("inventado","jugando",1,3));
        assertFalse(FaseComodin.disponible("congelador","jugando",4,3));
    }
}
