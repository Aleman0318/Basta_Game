package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class CorreoInstitucionalTest {
    @Test public void aceptaDominioExactoSinDistinguirMayusculas() {
        assertTrue(CorreoInstitucional.esValido("alumno@ues.edu.sv"));
        assertTrue(CorreoInstitucional.esValido("Alumno@UES.EDU.SV"));
    }
    @Test public void rechazaOtrosDominiosYDominiosEnganosos() {
        for (String correo : new String[]{"a@gmail.com", "a@outlook.com", "a@sub.ues.edu.sv",
                "a@ues.edu.sv.evil.com", "a@fakeues.edu.sv", "a@ues.edu.sv@ues.edu.sv", "a@ue\u017f.edu.sv"})
            assertFalse(correo, CorreoInstitucional.esValido(correo));
    }
    @Test public void rechazaVaciosYFormatoInvalido() {
        for (String correo : new String[]{"", "@ues.edu.sv", "a b@ues.edu.sv", "a\nb\nc@ues.edu.sv", " a@ues.edu.sv", "a@ues.edu.sv "})
            assertFalse(CorreoInstitucional.esValido(correo));
        assertFalse(CorreoInstitucional.esValido(null));
    }
}
