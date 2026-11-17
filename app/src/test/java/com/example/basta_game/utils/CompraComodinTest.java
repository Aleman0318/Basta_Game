package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompraComodinTest {
    private MutableData usuario() {
        MutableData u = MutableDataLocal.desde(new HashMap<>());
        u.child("monedas").setValue(500); u.child("gemas").setValue(20);
        u.child("recompensasAplicadas/p").setValue(true); u.child("nombre").setValue("Diego");
        return u;
    }
    @Test public void comprasSeguidasDeDistintosTiposConservanInventarioYMarcas() {
        MutableData u=usuario();
        assertTrue(CompraComodin.aplicar(u,"congelador","Congelador","monedas",250));
        assertTrue(CompraComodin.aplicar(u,"marca","Marca","gemas",10));
        assertTrue(CompraComodin.aplicar(u,"congelador","Congelador","monedas",250));
        assertEquals(2L,u.child("inventarioComodines/congelador/cantidad").getValue());
        assertEquals(1L,u.child("inventarioComodines/marca/cantidad").getValue());
        assertEquals(0L,u.child("monedas").getValue()); assertEquals(10L,u.child("gemas").getValue());
        assertEquals(true,u.child("recompensasAplicadas/p").getValue());
    }
    @Test public void sinSaldoODatosInvalidosAbortaSinCambios() {
        MutableData u=usuario(); Object antes=u.getValue();
        assertFalse(CompraComodin.aplicar(u,"x","X","gemas",21));
        assertFalse(CompraComodin.aplicar(u,"x","X","otra",1));
        assertFalse(CompraComodin.aplicar(u,"x","X","monedas",-1));
        assertEquals(antes,u.getValue());
        u.child("inventarioComodines/x/cantidad").setValue(Integer.MAX_VALUE);
        antes=u.getValue(); assertFalse(CompraComodin.aplicar(u,"x","X","monedas",1)); assertEquals(antes,u.getValue());
    }
    @Test public void agregacionNoCreaSetentaAvisosNiMezclaSesionesLejanas() {
        ResumenCompras resumen=new ResumenCompras(); String mensaje="";
        for (int i=0;i<70;i++) mensaje=resumen.agregar("Congelador",i*100);
        assertEquals("¡Compras realizadas!\nCongelador: 70 comprados",mensaje);
        assertEquals("¡Compras realizadas!\nCongelador: 70 comprados\nMarca: 1 comprados",resumen.agregar("Marca",7000));
        assertEquals("¡Compras realizadas!\nCongelador: 1 comprados",resumen.agregar("Congelador",12000));
    }
}
