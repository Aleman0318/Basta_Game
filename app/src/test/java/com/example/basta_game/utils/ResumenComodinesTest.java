package com.example.basta_game.utils;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResumenComodinesTest {
    @Test public void escudoCuentaJugadoresDistintosSinInventarPuntos() {
        Map<String,Object> escudo=new HashMap<>();escudo.put("idUso","s");escudo.put("nombreJugador","Diego");
        escudo.put("intentos",new HashMap<String,Object>(){{put("a",true);put("b",true);put("invalido",false);}});
        Map<String,Object> sala=new HashMap<>();sala.put("idPartida","p");
        sala.put("poderesRonda",Collections.singletonMap("ronda_1",new HashMap<String,Object>(){{put("idPartida","p");put("escudos",Collections.singletonMap("u",escudo));}}));
        String mensaje=ResumenComodines.crear(sala,1,false).get(0).getDescripcion();assertTrue(mensaje.contains("2 ataques"));assertFalse(mensaje.contains("puntos"));
        escudo.remove("intentos");assertTrue(ResumenComodines.crear(sala,1,false).get(0).getDescripcion().contains("Nadie intentó"));
        sala.put("idPartida","nueva");assertTrue(ResumenComodines.crear(sala,1,false).isEmpty());
    }
    @Test public void parryDetallaAtacanteYTipoSinRevelarEscudoAntes() {
        Map<String,Object> escudo=new HashMap<>();escudo.put("idUso","s");
        escudo.put("idPartida","p");escudo.put("nombreJugador","Diego");
        Map<String,Object> bloqueo=new HashMap<>();bloqueo.put("uidVictima","uidDiego");
        bloqueo.put("nombreAtacante","Alemango");bloqueo.put("tipo","inversion_letras");
        Map<String,Object> poderes=new HashMap<>();poderes.put("idPartida","p");
        poderes.put("escudos",Collections.singletonMap("uidDiego",escudo));
        poderes.put("bloqueos",Collections.singletonMap("ataque1",bloqueo));
        Map<String,Object> sala=new HashMap<>();sala.put("idPartida","p");
        sala.put("poderesRonda",Collections.singletonMap("ronda_1",poderes));
        String texto=ResumenComodines.crear(sala,1,false).get(0).getDescripcion();
        assertTrue(texto.contains("El Espejo de Alemango"));
        assertTrue(texto.contains("PARRY"));
    }

    @Test public void narracionDeCastigoUsaSoloLaPerdidaRegistrada() {
        Map<String,Object> doble=new HashMap<>();doble.put("resultado","castigo");doble.put("puntosBase",40);doble.put("puntosFinales",25);
        Map<String,Object> sala=new HashMap<>();
        sala.put("jugadores",Collections.singletonMap("u",Collections.singletonMap("nombre","Alemango")));
        sala.put("comodinesResultados",Collections.singletonMap("ronda_1",Collections.singletonMap("u",Collections.singletonMap("doble_o_nada",doble))));
        String descripcion=ResumenComodines.crear(sala,1,true).get(0).getDescripcion();
        assertTrue(descripcion.contains("Alemango"));assertTrue(descripcion.contains("Perdió 15 puntos"));assertTrue(descripcion.contains("40 -> 25"));
        doble.remove("puntosBase");
        assertFalse(ResumenComodines.crear(sala,1,true).get(0).getDescripcion().contains("15"));
    }
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
    @Test public void vientoApareceEnResumenSoloParaPartidaActual() {
        Map<String,Object> evento = new HashMap<>();
        evento.put("idPartida", "p1"); evento.put("idUso", "uso1");
        evento.put("nombreJugador", "Diego"); evento.put("semilla", 123L);
        Map<String,Object> sala = new HashMap<>(); sala.put("idPartida", "p1");
        sala.put("efectosVisuales", Collections.singletonMap("vientoCaos",
                Collections.singletonMap("ronda_2", evento)));
        assertTrue(ResumenComodines.crear(sala, 1, false).isEmpty());
        assertEquals(1, ResumenComodines.crear(sala, 2, false).size());
        assertTrue(ResumenComodines.crear(sala, 2, false).get(0).getTitulo().contains("Viento del Caos"));
        assertTrue(ResumenComodines.crear(sala, 2, false).get(0).getDescripcion().contains("Diego"));
        assertEquals(1, ResumenComodines.crear(sala, 3, true).size());
        sala.put("idPartida", "nuevaPartida");
        assertTrue(ResumenComodines.crear(sala, 2, false).isEmpty());
    }

    @Test public void decretoDetallaVocalCategoriasYMultaAplicadaSinInventarDescuentos() {
        Map<String,Object> evento=new HashMap<>();
        evento.put("idPartida","p");evento.put("idUso","decreto123");evento.put("vocal","A");
        evento.put("categorias",Arrays.asList("Animal","Color"));
        evento.put("resultados",Collections.singletonMap("u",Collections.singletonMap("multaAplicada",5)));
        Map<String,Object> sala=new HashMap<>();sala.put("idPartida","p");
        sala.put("decretoSilencio",Collections.singletonMap("ronda_1",evento));
        sala.put("jugadores",Collections.singletonMap("u",Collections.singletonMap("nombre","Diego")));
        assertEquals(1,ResumenComodines.crear(sala,1,false).size());
        String descripcion=ResumenComodines.crear(sala,1,false).get(0).getDescripcion();
        assertTrue(descripcion.contains("Animal y Color"));
        assertTrue(descripcion.contains("vocal A"));
        assertTrue(descripcion.contains("Diego -5 pts"));
        sala.put("idPartida","siguiente");
        assertTrue(ResumenComodines.crear(sala,1,false).isEmpty());
    }
}
