package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ReinicioComodinesTest {
    private MutableData estado() {
        MutableData s = MutableDataLocal.desde(new HashMap<>());
        s.child("poderesRonda/ronda_1/escudos/u/activo").setValue(true);
        s.child("efectosVisuales/espejo/ronda_1/u/idUso").setValue("mirror");
        s.child("efectosVisuales/vientoCaos/ronda_1/idUso").setValue("wind");
        s.child("comodinesPendientes/ronda_1/u/doble_o_nada").setValue(true);
        s.child("comodinesActivos/ronda_1/congelador/activo").setValue(true);
        s.child("comodinesUsados/ronda_1/wind/tipo").setValue("viento_caos");
        s.child("comodinesResultados/ronda_1/u/doble_o_nada/activo").setValue(true);
        s.child("eventoCaosGlobal/ronda_1/idUso").setValue("flujo");
        s.child("minijuegoActual/activo").setValue(false);
        s.child("ultimoMinijuego").setValue("tres_en_raya");
        s.child("prestamosImpuesto/impuesto1/estado").setValue("conservado");
        s.child("rendimientoRondas/ronda_1/cerrado").setValue(true);
        s.child("decretoSilencio/ronda_1/idUso").setValue("decreto");
        s.child("usosCongeladorPartida").setValue(2);
        s.child("jugadores/u/puntajePartida").setValue(120);
        s.child("categoriasActivas/0").setValue("Animal");
        s.child("tiempoRonda").setValue(60);
        return s;
    }

    @Test public void nuevaPartidaEliminaTODOSLosEfectosSinBorrarConfiguracion() {
        MutableData s=estado();
        ReinicioComodines.nuevaPartida(s);
        for (String ruta: new String[]{"poderesRonda", "efectosVisuales", "comodinesPendientes",
                "comodinesActivos", "comodinesUsados", "comodinesResultados", "eventoCaosGlobal",
                "minijuegoActual", "ultimoMinijuego", "prestamosImpuesto", "rendimientoRondas", "decretoSilencio"}) {
            assertNull(ruta, s.child(ruta).getValue());
        }
        assertEquals(0, ((Number)s.child("usosCongeladorPartida").getValue()).intValue());
        assertEquals(120, ((Number)s.child("jugadores/u/puntajePartida").getValue()).intValue());
        assertEquals(60, ((Number)s.child("tiempoRonda").getValue()).intValue());
        assertEquals("Animal", s.child("categoriasActivas/0").getValue());
    }

    @Test public void siguienteRondaLimpiaSoloLaNuevaSinBorrarLoAnteriorNiLimiteCongelador() {
        MutableData s=estado();
        s.child("poderesRonda/ronda_2/escudos/u/activo").setValue(true);
        s.child("efectosVisuales/espejo/ronda_2/u/idUso").setValue("espejoVIEJO");
        s.child("efectosVisuales/vientoCaos/ronda_2/idUso").setValue("vientoVIEJO");
        s.child("comodinesPendientes/ronda_2/u/doble_o_nada").setValue(true);
        s.child("comodinesActivos/ronda_2/congelador/activo").setValue(true);
        s.child("comodinesUsados/ronda_2/uso/tipo").setValue("congelador");
        s.child("comodinesResultados/ronda_2/u/doble_o_nada/activo").setValue(true);
        s.child("eventoCaosGlobal/ronda_2/idUso").setValue("flujoViejo");
        s.child("decretoSilencio/ronda_2/idUso").setValue("decretoViejo");
        ReinicioComodines.nuevaRonda(s,2);
        for (String ruta:new String[]{"poderesRonda/ronda_2", "efectosVisuales/espejo/ronda_2",
                "efectosVisuales/vientoCaos/ronda_2", "comodinesPendientes/ronda_2",
                "comodinesActivos/ronda_2", "comodinesUsados/ronda_2", "comodinesResultados/ronda_2",
                "eventoCaosGlobal/ronda_2", "decretoSilencio/ronda_2", "minijuegoActual"}) {
            assertNull(ruta,s.child(ruta).getValue());
        }
        assertNotNull(s.child("poderesRonda/ronda_1").getValue());
        assertNotNull(s.child("efectosVisuales/espejo/ronda_1").getValue());
        assertNotNull(s.child("comodinesPendientes/ronda_1").getValue());
        assertNotNull(s.child("eventoCaosGlobal/ronda_1").getValue());
        assertEquals(2,((Number)s.child("usosCongeladorPartida").getValue()).intValue());
        assertEquals("tres_en_raya",s.child("ultimoMinijuego").getValue());
        assertEquals("conservado",s.child("prestamosImpuesto/impuesto1/estado").getValue());
        assertEquals(120,((Number)s.child("jugadores/u/puntajePartida").getValue()).intValue());
    }

    @Test public void nuncaReiniciaLaPrimeraRondaDentroDeLaMismaPartida() {
        MutableData s=estado();
        Object previo=s.getValue();
        ReinicioComodines.nuevaRonda(s,1);
        assertEquals(previo,s.getValue());
    }
}
