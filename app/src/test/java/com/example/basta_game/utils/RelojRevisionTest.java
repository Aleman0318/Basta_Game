package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RelojRevisionTest {
    @Test public void cincoYSeisJugadoresPersistenDuracionYNoCambianTrasAbandono() {
        for(int cantidad:new int[]{5,6}) {
            MutableData s=sala();
            for(int i=3;i<cantidad;i++) s.child("jugadores/u"+i+"/estaConectado").setValue(true);
            s.child(RelojRevision.ruta(1)+"/reloj").setValue(null);
            assertTrue(RelojRevision.coordinar(s,"p",1,"h",1000));
            MutableData r=s.child(RelojRevision.ruta(1)+"/reloj");
            long duracion=cantidad==5 ? 5000 : 10000;
            r.child("modo").setValue("propuestas");r.child("inicio").setValue(1000);
            assertTrue(RelojRevision.prepararVoto(s,1,"v",1500,"Animal"));
            assertEquals(duracion,r.child("duracionMs").getValue());r.child("inicio").setValue(1000);
            s.child("jugadores/u3").setValue(null);
            assertEquals(1000+duracion,RelojRevision.finTurno(r.child("inicio").getValue(),r.child("duracionMs").getValue(),false));
            assertFalse(RelojRevision.coordinar(s,"p",1,"h",1000+duracion-1));
        }
    }
    @Test public void categoriaEnteramenteVaciaCierraTrasMostrarUnSegundoSinVotacion() {
        MutableData s=sala();
        for(String uid:Arrays.asList("h","b","c")) s.child("respuestas/ronda_1/"+uid+"/Animal").setValue("Sin respuesta");
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000);
        assertFalse(RelojRevision.coordinar(s,"p",1,"b",2999));
        assertTrue(RelojRevision.coordinar(s,"p",1,"b",3000));
        assertEquals(1L,s.child(RelojRevision.ruta(1)+"/indiceActual").getValue());
        for(String uid:Arrays.asList("h","b","c")) assertEquals(0L,s.child("validaciones/ronda_1/"+uid+"/Animal/puntos").getValue());
        assertNull(s.child("votacionActiva").getValue());
    }
    @Test public void respuestaPendienteONoVaciaConservaCincoSegundos() {
        MutableData s=sala();
        s.child("respuestas/ronda_1/h/Animal").setValue("Abeja");
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000);
        assertFalse(RelojRevision.coordinar(s,"p",1,"b",3000));
        assertTrue(RelojRevision.coordinar(s,"p",1,"b",6000));
    }
    @Test public void validacionTardiaNoSobrescribeResultadoYPreparacionAusenteNoBloquea() {
        MutableData s=sala();
        s.child("letraActual").setValue("A");
        s.child("respuestas/ronda_1/h/Animal").setValue("Abeja");
        s.child("respuestas/ronda_1/b/Animal").setValue("abeja");
        s.child("validaciones/ronda_1/c/Animal/puntos").setValue(5);
        Map<String,Object> inicial=new HashMap<>();
        inicial.put("Animal",Collections.singletonMap("puntos",10));
        assertFalse(RelojRevision.guardarIniciales(s,"p",1,"c",inicial));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000);
        assertTrue(RelojRevision.coordinar(s,"p",1,"b",6000));
        for(String uid:Arrays.asList("h","b","c")) assertEquals(5L,s.child("validaciones/ronda_1/"+uid+"/Animal/puntos").getValue());
        assertFalse(RelojRevision.guardarIniciales(s,"p",1,"h",inicial));
        assertFalse(RelojRevision.guardarIniciales(s,"vieja",1,"h",inicial));
    }
    @Test public void validacionBasicaMantieneReglaLocalSinInventarPuntos() {
        assertTrue(ValidacionBasica.correcta("A","\u00c1guila"));
        assertFalse(ValidacionBasica.correcta("A","a1"));
        assertFalse(ValidacionBasica.correcta("S","Sin respuesta"));
        assertFalse(ValidacionBasica.correcta("A"," "));
        assertFalse(ValidacionBasica.correcta("A","Perro"));
        MutableData s=sala(); s.child("letraActual").setValue("A");
        s.child("respuestas/ronda_1/h/Animal").setValue("Armadillo");
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000);
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",6000));
        assertEquals(10L,s.child("validaciones/ronda_1/h/Animal/puntos").getValue());
        assertEquals(0L,s.child("validaciones/ronda_1/b/Animal/puntos").getValue());
    }
    private MutableData sala() {
        MutableData s=MutableDataLocal.desde(new HashMap<>());
        s.child("idPartida").setValue("p"); s.child("hostUID").setValue("h"); s.child("rondaActual").setValue(1);
        s.child("categoriasActivas").setValue(Arrays.asList("Animal","Fruta"));
        for(String uid:Arrays.asList("h","b","c")) s.child("jugadores/"+uid+"/estaConectado").setValue(true);
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",1000));
        s.child(RelojRevision.ruta(1)+"/reloj/preparandoDesde").setValue(1000);
        return s;
    }
    @Test public void inicioEsCompartidoYNoSeReiniciaAlRepetirPreparacion() {
        MutableData s=sala();
        assertFalse(RelojRevision.coordinar(s,"p",1,"h",1001));
        for(String uid:Arrays.asList("h","b","c")) assertTrue(RelojRevision.preparado(s,"p",1,0,1,uid));
        assertFalse(RelojRevision.preparado(s,"p",1,0,1,"b"));
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",1002));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1002);
        assertFalse(RelojRevision.coordinar(s,"p",1,"h",1003));
        assertEquals(6002,RelojRevision.fin(s.child(RelojRevision.ruta(1)+"/reloj/inicio").getValue()));
        assertFalse(RelojRevision.preparado(s,"p",1,0,1,"h"));
    }
    @Test public void preparacionTieneMaximoOchoSegundosYNoExpulsaMiembros() {
        MutableData s=sala();
        assertFalse(RelojRevision.coordinar(s,"p",1,"h",8999));
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",9000));
        assertEquals(3,s.child("jugadores").getChildrenCount());
    }
    @Test public void cierreDeCategoriaUnicoYRechazoDeSolicitudesAntiguas() {
        MutableData s=sala();
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(2000);
        assertFalse(RelojRevision.coordinar(s,"p",1,"h",5499));
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",5500));
        assertEquals("resultado",s.child(RelojRevision.ruta(1)+"/reloj/modo").getValue());
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(5500);
        assertFalse(RelojRevision.coordinar(s,"p",1,"b",8499));
        assertTrue(RelojRevision.coordinar(s,"p",1,"b",8500));
        assertEquals(1L,s.child(RelojRevision.ruta(1)+"/indiceActual").getValue());
        assertFalse(RelojRevision.preparado(s,"p",1,0,1,"b"));
        assertFalse(RelojRevision.coordinar(s,"anterior",1,"h",7001));
        assertFalse(RelojRevision.coordinar(s,"p",1,"ausente",7001));
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",7001));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(7001);
        assertTrue(RelojRevision.coordinar(s,"p",1,"h",12001));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(12001); assertTrue(RelojRevision.coordinar(s,"p",1,"h",15001));
        Object antes=s.getValue(); assertFalse(RelojRevision.coordinar(s,"p",1,"h",15002)); assertEquals(antes,s.getValue());
    }
    @Test public void votoNoEntraAntesDelInicioNiDespuesDelPlazo() {
        MutableData s=sala();
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(1000);
        Map<String,Object> datos=new HashMap<>();
        datos.put("activa",true); datos.put("idVotacion","v"); datos.put("claveVotacion","k");
        datos.put("uidObjetivo","b"); datos.put("categoria","Animal"); datos.put("respuesta","Abeja");
        datos.put("turnoRevision",1);
        s.child(RelojRevision.ruta(1)+"/reloj/modo").setValue("propuestas");
        assertTrue(VotacionRevision.crear(s,"p",1,"b",datos,1500));
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","h","si",1501));
        s.child(RelojRevision.ruta(1)+"/reloj/inicio").setValue(2000);
        assertTrue(VotacionRevision.registrar(s,"p",1,"v","h","si",6999));
        assertFalse(VotacionRevision.registrar(s,"p",1,"v","c","no",7000));
        Map<String,Object> cambios=new HashMap<>(); cambios.put("votacionActiva/ronda_1",null);
        assertFalse(VotacionRevision.resolver(s,"p",1,"v","h",Collections.singletonMap("h","si"),cambios,6999));
        assertTrue(VotacionRevision.resolver(s,"p",1,"v","h",Collections.singletonMap("h","si"),cambios,7000));
        assertEquals("resultado",s.child(RelojRevision.ruta(1)+"/reloj/modo").getValue());
        assertFalse(VotacionRevision.resolver(s,"p",1,"v","h",Collections.singletonMap("h","si"),cambios,7001));
    }
}
