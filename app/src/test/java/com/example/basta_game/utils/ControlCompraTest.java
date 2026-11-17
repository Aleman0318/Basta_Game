package com.example.basta_game.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class ControlCompraTest {
    @Test public void bloqueaSoloPendientesYPermiteComprarTrasConfirmarOCancelar() {
        ControlCompra c=new ControlCompra();
        assertTrue(c.iniciar(100)); assertFalse(c.iniciar(101));
        c.terminar(true,200);
        assertTrue(c.iniciar(200)); assertFalse(c.iniciar(201));
        c.terminar(false,2300); assertTrue(c.iniciar(2300));
    }
}
