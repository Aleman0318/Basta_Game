package com.example.basta_game.utils;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LetraSalaTest {
    @Test public void enieEsUnaInicialDistintaDeEne() {
        assertTrue(ValidacionBasica.correcta("Ñ","Ñandú"));
        assertFalse(ValidacionBasica.correcta("Ñ","Nandú"));
        assertFalse(ValidacionBasica.correcta("N","Ñandú"));
    }
    @Test public void unaDiezYVeintisieteRondasSinRepetirIncluyenEnie() {
        for(int rondas:new int[]{1,10,27}) {
            List<String> letras=LetraSala.nuevaPartida();
            assertEquals(27,letras.size());assertEquals(rondas,new HashSet<>(letras.subList(0,rondas)).size());
            assertTrue(letras.contains("Ñ"));
            assertEquals(new HashSet<>(Arrays.asList("ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".split(""))),new HashSet<>(letras));
        }
    }
    @Test public void invitacionConservaOYCeroSinCambiarElCodigo() {
        assertEquals("¡JUEGA BASTA CONMIGO! El código de la sala es: O0AB12",CodigoSala.invitacion("O0AB12"));
    }
}
