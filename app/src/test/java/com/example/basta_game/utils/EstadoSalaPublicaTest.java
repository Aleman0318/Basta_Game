package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class EstadoSalaPublicaTest {
    @Test public void soloPermiteEntrarALobbyConCupo() {
        assertTrue(EstadoSalaPublica.permiteUnirse("En espera", 3, 8));
        assertTrue(EstadoSalaPublica.permiteUnirse("Esperando jugadores", 1, 2));
        assertFalse(EstadoSalaPublica.permiteUnirse("En espera", 8, 8));
        assertFalse(EstadoSalaPublica.permiteUnirse("En juego", 3, 8));
        assertFalse(EstadoSalaPublica.permiteUnirse("En espera", 0, 0));
    }
    @Test public void conservaSalasLlenasYEnPartidaConEstadoVisible() {
        assertEquals("En lobby · LLENA", EstadoSalaPublica.etiqueta("En espera", 8, 8));
        assertEquals("En partida", EstadoSalaPublica.etiqueta("En juego", 5, 8));
        assertEquals("En partida · LLENA", EstadoSalaPublica.etiqueta("En juego", 8, 8));
    }
}
