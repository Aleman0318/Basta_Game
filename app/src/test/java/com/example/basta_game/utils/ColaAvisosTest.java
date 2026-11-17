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
    @Test public void mismoEventoNoSeDuplicaAlRecibirloOtraVez() {
        ColaAvisos c=new ColaAvisos(); c.agregar(null,"Aviso",1,0); c.agregar(null,"Aviso",1,200);
        assertEquals(1,c.pendientes()); assertEquals(2800,c.restante(200));
    }
}
