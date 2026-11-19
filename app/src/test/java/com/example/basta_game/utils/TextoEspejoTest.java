package com.example.basta_game.utils;
import static org.junit.Assert.*;
import org.junit.Test;
public class TextoEspejoTest {
    @Test public void invierteSoloRepresentacion() {
        String original = "Gato";
        assertEquals("otaG",TextoEspejo.invertir(original));
        assertEquals("Gato",original);
    }
    @Test public void soportaVacios() {
        assertEquals("",TextoEspejo.invertir(null));
        assertEquals("",TextoEspejo.invertir(""));
    }
}
