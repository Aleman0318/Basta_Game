package com.example.basta_game.utils;
import static org.junit.Assert.*;
import org.junit.Test;
public class NombresUnicosTest {
    @Test public void ignoraMayusculasYEspacios() {
        assertEquals(NombresUnicos.clave(" Roberto "),NombresUnicos.clave("roberto"));
        assertEquals(NombresUnicos.clave("roBERto"),NombresUnicos.clave("ROBERTO"));
        assertNotEquals(NombresUnicos.clave("Robertooo"),NombresUnicos.clave("Roberto"));
        assertNotEquals(NombresUnicos.clave("Roberto11"),NombresUnicos.clave("Roberto"));
    }
    @Test public void validaLongitud() {
        assertFalse(NombresUnicos.valido("R"));
        assertTrue(NombresUnicos.valido("Roberto"));
    }
}
