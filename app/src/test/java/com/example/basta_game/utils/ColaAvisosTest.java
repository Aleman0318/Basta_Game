package com.example.basta_game.utils;
import org.junit.Test;
import static org.junit.Assert.*;

public class ColaAvisosTest {
    @Test public void cambiarPantallaNoReiniciaNiDuplicaAviso() {
        ColaAvisos c=new ColaAvisos(); c.agregar(null,"Hola",1,100);
        ColaAvisos.Aviso antes=c.actual(200);
        assertSame(antes,c.actual(1000)); assertEquals(2100,c.restante(1000));
        assertEquals(1,c.pendientes()); assertNull(c.actual(3100));
    }
    @Test public void comprasReemplazanUnSoloAvisoSinOcultarOtrosEventos() {
        ColaAvisos c=new ColaAvisos(); c.agregar(null,"Host salio",3,0);
        for(int i=1;i<=70;i++) c.agregar("compras","Compraste "+i,1,i);
        assertEquals(2,c.pendientes()); assertEquals("Host salio",c.actual(100).texto);
        assertEquals("Compraste 70",c.actual(3000).texto); assertEquals(1,c.pendientes());
    }
    @Test public void deslizarDescartaSoloVisibleYAvanzaEnCola() {
        ColaAvisos c = new ColaAvisos();
        c.agregar(null, "Primero", 1, 0);
        c.agregar(null, "Segundo", 4, 20);
        ColaAvisos.Aviso primero = c.actual(25);
        assertTrue(c.descartar(primero));
        assertEquals("Segundo", c.actual(26).texto);
        assertEquals(1, c.pendientes());
        assertFalse(c.descartar(primero));
        assertEquals("Segundo", c.actual(100).texto);
    }
    @Test public void avisoViejoNoDescartaActualizacionDeLaMismaCompra() {
        ColaAvisos c = new ColaAvisos();
        c.agregar("compras", "Uno", 1, 0);
        ColaAvisos.Aviso viejo = c.actual(10);
        c.agregar("compras", "Dos", 1, 100);
        assertFalse(c.descartar(viejo));
        assertEquals("Dos", c.actual(200).texto);
    }
    @Test public void mismoEventoNoSeDuplicaAlRecibirloOtraVez() {
        ColaAvisos c=new ColaAvisos(); c.agregar(null,"Aviso",1,0); c.agregar(null,"Aviso",1,200);
        assertEquals(1,c.pendientes()); assertEquals(2800,c.restante(200));
    }
}
