package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CategoriaAutomaticaTest {
    private MutableData sala(String a,String b){
        MutableData s=MutableDataLocal.desde(new HashMap<>());s.child("idPartida").setValue("p");s.child("rondaActual").setValue(1);s.child("letraActual").setValue("F");s.child("categoriasActivas").setValue(Arrays.asList("Color","Animal"));
        for(String u:Arrays.asList("a","b"))s.child("jugadores/"+u+"/estaConectado").setValue(true);
        s.child("respuestas/ronda_1/a/Color").setValue(a);s.child("respuestas/ronda_1/b/Color").setValue(b);return s;
    }
    @Test public void repetidasOInvalidasVanDirectamenteATresSegundos(){
        for(String[] par:new String[][]{{"fares","Fares"},{"Ox","Uva"},{"fares","Fares"}}){MutableData s=sala(par[0],par[1]);assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));
            MutableData r=s.child(RelojRevision.ruta(1)+"/reloj");assertEquals("resultado",r.child("modo").getValue());assertEquals(3000L,r.child("duracionMs").getValue());r.child("inicio").setValue(1000);
            assertFalse(RelojRevision.coordinar(s,"p",1,"b",3999));assertTrue(RelojRevision.coordinar(s,"p",1,"b",4000));assertEquals(1L,s.child(RelojRevision.ruta(1)+"/indiceActual").getValue());
        }
    }
    @Test public void clasificacionInvalidaOriginalNoAbreUnTurnoImposible(){
        MutableData s=sala("Foca","");s.child("validaciones/ronda_1/a/Color/puntos").setValue(0);s.child("validaciones/ronda_1/a/Color/correcta").setValue(false);
        assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));assertEquals("resultado",s.child(RelojRevision.ruta(1)+"/reloj/modo").getValue());assertEquals(0L,s.child("validaciones/ronda_1/a/Color/puntos").getValue());
    }
    @Test public void vaciasConservanDosSegundosYUnaElegibleConservaRevision(){
        MutableData s=sala("","");assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));MutableData r=s.child(RelojRevision.ruta(1)+"/reloj");assertEquals("revision",r.child("modo").getValue());r.child("inicio").setValue(1000);assertFalse(RelojRevision.coordinar(s,"p",1,"a",2999));assertTrue(RelojRevision.coordinar(s,"p",1,"a",3000));
        s=sala("Fucsia","");assertTrue(RelojRevision.coordinar(s,"p",1,"a",1000));assertEquals("revision",s.child(RelojRevision.ruta(1)+"/reloj/modo").getValue());
    }
}
