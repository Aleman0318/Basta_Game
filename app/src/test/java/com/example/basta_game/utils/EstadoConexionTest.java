package com.example.basta_game.utils;
import org.junit.Test;
import static org.junit.Assert.*;
public class EstadoConexionTest {
    @Test public void recuperarTransporteNoHabilitaUnaSalaTodaviaSincronizando(){
        assertFalse(EstadoConexion.accionesDisponibles(true,true,true,true,false));
        assertTrue(EstadoConexion.accionesDisponibles(true,true,true,true,true));
        assertTrue(EstadoConexion.accionesDisponibles(true,true,true,false,false));
        assertFalse(EstadoConexion.accionesDisponibles(false,true,true,true,true));
        assertFalse(EstadoConexion.accionesDisponibles(true,true,false,false,true));
    }
    @Test public void volverAlMenuConRedNoConfundeReconectarConPerderInternet(){assertFalse(EstadoConexion.bloquear(true,false,false,0));assertFalse(EstadoConexion.bloquear(true,false,false,19999));}
    @Test public void sinRedBloqueaYRecuperacionDiscretaNoExigeReinicio(){assertTrue(EstadoConexion.bloquear(false,false,false,0));assertFalse(EstadoConexion.bloquear(true,false,false,1));}
    @Test public void salaMantieneProteccionMientrasRecuperaSuEstado(){assertFalse(EstadoConexion.bloquear(true,true,false,0));}
    @Test public void indisponibilidadPersistenteOErrorNoSeOcultan(){assertFalse(EstadoConexion.bloquear(true,false,false,20000));assertFalse(EstadoConexion.bloquear(true,false,true,0));}
}
