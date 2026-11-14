package com.example.basta_game.utils;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResumenComodinesTest {
    @Test public void sinRegistrosNoInventaUsos() {
        assertTrue(ResumenComodines.crear(Collections.emptyMap(),3,true).isEmpty());
    }
    @Test public void congeladorNoInventaPuntosNiDuracionesDeOtroUso() {
        Map<String,Object> uso = new HashMap<>(); uso.put("tipo","congelador"); uso.put("nombreJugador","Diego");
        Map<String,Object> sala = new HashMap<>();
        sala.put("comodinesUsados",Collections.singletonMap("ronda_1",Collections.singletonMap("uso1",uso)));
        sala.put("comodinesActivos",Collections.singletonMap("ronda_1",Collections.singletonMap("congelador",
                new HashMap<String,Object>() {{ put("idUso","otro"); put("duracionMs",8000); }})));
        String descripcion = ResumenComodines.crear(sala,1,true).get(0).getDescripcion();
        assertTrue(descripcion.contains("no registrada")); assertFalse(descripcion.contains("puntos"));
        uso.put("duracionMs",8000);
        assertTrue(ResumenComodines.crear(sala,1,true).get(0).getDescripcion().contains("8.0 s"));
        assertTrue(ResumenComodines.crear(sala,1,true).get(0).isNeutral());
    }
    @Test public void finalIncluyeRondasAnterioresSinRepetirDobleONada() {
        Map<String,Object> sala = new HashMap<>();
        Map<String,Object> uso = new HashMap<>(); uso.put("tipo","doble_o_nada"); uso.put("uid","u");
        sala.put("comodinesUsados",Collections.singletonMap("ronda_1",Collections.singletonMap("uso",uso)));
        sala.put("comodinesResultados",Collections.singletonMap("ronda_1",Collections.singletonMap("u",
                Collections.singletonMap("doble_o_nada",Collections.singletonMap("resultado","duplicado")))));
        assertEquals(1,ResumenComodines.crear(sala,2,true).size());
        assertTrue(ResumenComodines.crear(sala,2,false).isEmpty());
    }
}
