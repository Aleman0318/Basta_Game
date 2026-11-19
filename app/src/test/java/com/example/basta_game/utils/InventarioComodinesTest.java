package com.example.basta_game.utils;

import com.example.basta_game.models.ComodinPartida;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventarioComodinesTest {
    private List<ComodinPartida> catalogo() {
        return Arrays.asList(new ComodinPartida("congelador","Congelador","Original","Control",7,0,true,false),
                new ComodinPartida("pendiente","Pendiente","Original pendiente","Premium",8,0,false,true));
    }
    @Test public void muestraImplementadosPendientesYDesconocidosSinModificarInventario() {
        Map<String,Object> datos=new LinkedHashMap<>();
        datos.put("congelador",Collections.singletonMap("cantidad",2));
        datos.put("pendiente",Collections.singletonMap("cantidad",5));
        Map<String,Object> desconocido=new HashMap<>();desconocido.put("cantidad",3);desconocido.put("nombre","Nombre registrado");datos.put("otro",desconocido);
        String antes=datos.toString();List<ComodinPartida> items=InventarioComodines.listar(datos,catalogo(),99);
        assertEquals(3,items.size());assertTrue(items.get(0).isImplementado());assertEquals(2,items.get(0).getCantidad());
        assertFalse(items.get(1).isImplementado());assertEquals(8,items.get(1).getIconoResId());assertEquals(5,items.get(1).getCantidad());
        assertFalse(items.get(2).isImplementado());assertEquals("Nombre registrado",items.get(2).getNombre());assertEquals(99,items.get(2).getIconoResId());
        assertEquals(antes,datos.toString());
    }
    @Test public void noConcedeUnidadesNiOcultaCantidadesLegitimasEnTexto() {
        Map<String,Object> datos=new HashMap<>();
        datos.put("congelador",Collections.singletonMap("cantidad",0));datos.put("pendiente",Collections.singletonMap("cantidad",-1));
        datos.put("fraccion",Collections.singletonMap("cantidad",1.5));datos.put("desborde",Collections.singletonMap("cantidad",Long.MAX_VALUE));
        datos.put("texto",Collections.singletonMap("cantidad","12"));
        List<ComodinPartida> items=InventarioComodines.listar(datos,catalogo(),99);assertEquals(1,items.size());assertEquals(12,items.get(0).getCantidad());
        assertTrue(InventarioComodines.listar(null,catalogo(),99).isEmpty());
        assertTrue(InventarioComodines.listar(Collections.emptyMap(),catalogo(),99).isEmpty());
    }
    @Test public void agrupaLosDosIdsDelMismoComodinSinInventarUnidades() {
        Map<String,Object> datos=new HashMap<>();datos.put("cambio_flujo",Collections.singletonMap("cantidad",4));datos.put("uno_reversa",Collections.singletonMap("cantidad",2));
        List<ComodinPartida> tipos=Collections.singletonList(new ComodinPartida("uno_reversa","Cambio","Efecto","Caótico",9,0,true,false));
        List<ComodinPartida> items=InventarioComodines.listar(datos,tipos,99);
        assertEquals(1,items.size());assertEquals("uno_reversa",items.get(0).getId());assertEquals(6,items.get(0).getCantidad());assertTrue(items.get(0).isImplementado());
    }
}
