package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class DefensaEscudoDoradoTest {
    @Test public void proteccionDePartidaAnteriorNoBloqueaReinicio() {
        assertTrue(DefensaEscudoDorado.yaEquipadoEnPartida("partida2","partida2"));
        assertFalse(DefensaEscudoDorado.yaEquipadoEnPartida("partida2","partida1"));
        assertFalse(DefensaEscudoDorado.yaEquipadoEnPartida("partida2",null));
        assertFalse(DefensaEscudoDorado.yaEquipadoEnPartida(null,"partida2"));
    }

    @Test public void soloSeEquipaEnJuegoAntesDeBasta() {
        assertTrue(DefensaEscudoDorado.puedeEquipar("jugando", false, false, 1, 1));
        assertFalse(DefensaEscudoDorado.puedeEquipar("jugando", true, false, 1, 1));
        assertFalse(DefensaEscudoDorado.puedeEquipar("jugando", false, true, 1, 1));
        assertFalse(DefensaEscudoDorado.puedeEquipar("revision_puntuaciones", false, false, 1, 1));
        assertFalse(DefensaEscudoDorado.puedeEquipar("jugando", false, false, 2, 1));
        assertFalse(DefensaEscudoDorado.puedeEquipar("jugando", false, false, 0, 0));
    }
}
