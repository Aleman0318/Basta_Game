package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SalidaSesionTest {
    private MutableData sala() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p");
        s.child("jugadores/u/estaConectado").setValue(true);
        s.child("jugadores/v/estaConectado").setValue(true);
        s.child("sesiones/u/actual/conectada").setValue(true);
        s.child("sesiones/u/antigua/conectada").setValue(false);
        s.child("sesiones/u/antigua/desconectadoDesde").setValue(1000);
        return s;
    }
    @Test public void abandonoIgnoraSesionesHistoricasConocidas() {
        MutableData s=sala();
        assertTrue(TransferenciaHost.abandonarSesion(s,"u","p","actual",Arrays.asList("actual","antigua")));
        assertFalse(s.child("jugadores").hasChild("u"));
        assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","actual",Arrays.asList("actual","antigua")));
    }
    @Test public void ticketNoBorraSesionNuevaNiOtraPartida() {
        MutableData s=sala();
        s.child("sesiones/u/nueva/conectada").setValue(false);
        Object antes=s.getValue();
        assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","actual",Arrays.asList("actual","antigua")));
        assertEquals(antes,s.getValue());
        assertFalse(TransferenciaHost.abandonarSesion(s,"u","otra","actual",Arrays.asList("actual","antigua","nueva")));
    }
    @Test public void noRetiraOtraConexionActivaConocida() {
        MutableData s=sala();s.child("sesiones/u/antigua/conectada").setValue(true);
        assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","actual",Arrays.asList("actual","antigua")));
    }
    @Test public void reservaLeeTimestampDeSesionAunqueAgregadoEsteAtrasado() {
        Map<String,Object> sesiones=Collections.singletonMap("actual",new HashMap<String,Object>(){{put("conectada",false);put("desconectadoDesde",1000L);}});
        assertTrue(IngresoSala.reservaVigente(true,null,sesiones,90999,90000));
        assertFalse(IngresoSala.reservaVigente(true,null,sesiones,91000,90000));
        assertFalse(IngresoSala.reservaVigente(true,null,sesiones,91001,90000));
    }
    @Test public void salidaDuranteRecreacionIncluyeSoloSesionesPropias() {
        MutableData s=sala();s.child("sesiones/u/pendiente/conectada").setValue(true);
        assertTrue(TransferenciaHost.abandonarSesion(s,"u","p","pendiente",Arrays.asList("antigua","actual","pendiente"),Arrays.asList("actual","pendiente")));
        assertFalse(s.child("jugadores").hasChild("u"));
    }
    @Test public void registroPendientePuedeUsarSesionPropiaAnteriorSinAfectarOtroCliente() {
        MutableData s=sala();
        assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","pendiente",Arrays.asList("actual","antigua","pendiente"),Arrays.asList("antigua","pendiente")));
        assertTrue(s.child("jugadores").hasChild("u"));
        assertTrue(TransferenciaHost.abandonarSesion(s,"u","p","pendiente",Arrays.asList("actual","antigua","pendiente"),Arrays.asList("actual","pendiente")));
    }
}
