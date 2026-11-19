package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImpuestoRevolucionarioTest {
    @Test public void liquidacionUsaCapturaYConservaNegativosDeDobleSinDuplicar() {
        MutableData s=sala();assertTrue(ImpuestoRevolucionario.prestar(s,"p",2,"a","b","uno"));
        s.child("rendimientoRondas/ronda_2").setValue(new HashMap<String,Object>(){{put("idPartida","p");put("cerrado",true);put("puntos",new HashMap<String,Object>(){{put("a",10);put("b",0);}});put("respuestasConfirmadas",new HashMap<String,Object>(){{put("a",true);put("b",true);}});}});
        s.child("puntajesRondas/ronda_2/a").setValue(-5);s.child("puntajesRondas/ronda_2/b").setValue(-15);
        ImpuestoRevolucionario.liquidar(s,"p",2);Object fin=s.getValue();ImpuestoRevolucionario.liquidar(s,"p",2);assertEquals(fin,s.getValue());
        assertEquals(-5L,s.child("puntajesRondas/ronda_2/a").getValue());assertEquals(-15L,s.child("puntajesRondas/ronda_2/b").getValue());
        assertEquals(5,ImpuestoRevolucionario.ajusteVisible(s,"a"));assertEquals(20,ImpuestoRevolucionario.disponible(s,"b",3));
    }
    @Test public void negativosPreviosNoSeIgnoranAlCalcularDisponible() {
        MutableData s=sala();s.child("puntajesRondas/ronda_2/b").setValue(-50);s.child("rondaActual").setValue(3);
        assertEquals(0,ImpuestoRevolucionario.disponible(s,"b",3));assertFalse(ImpuestoRevolucionario.prestar(s,"p",3,"a","b","uno"));
    }
    @Test public void salidaCancelaReservaSinTransferirNiPermitirReaplicarla() {
        MutableData s=sala();assertTrue(ImpuestoRevolucionario.prestar(s,"p",2,"a","b","uno"));
        ImpuestoRevolucionario.cancelarPorSalida(s,"b");Object fin=s.getValue();ImpuestoRevolucionario.cancelarPorSalida(s,"b");assertEquals(fin,s.getValue());
        assertEquals("devuelto",s.child("prestamosImpuesto/uno/estado").getValue());assertEquals(0,ImpuestoRevolucionario.ajusteVisible(s,"a"));
    }
    private MutableData sala(){MutableData s=MutableDataLocal.desde(new HashMap<>());s.child("idPartida").setValue("p");s.child("rondaActual").setValue(2);s.child("fase").setValue("jugando");
        for(String u:Arrays.asList("a","b","c"))s.child("jugadores").child(u).child("nombre").setValue(u);
        s.child("puntajesRondas/ronda_1/a").setValue(30);s.child("puntajesRondas/ronda_1/b").setValue(40);return s;}
    @Test public void ejemploYReintentosNoDuplican(){MutableData s=sala();assertTrue(ImpuestoRevolucionario.prestar(s,"p",2,"a","b","uno"));assertFalse(ImpuestoRevolucionario.prestar(s,"p",2,"a","b","uno"));
        assertEquals(5,ImpuestoRevolucionario.ajusteVisible(s,"a"));assertEquals(-5,ImpuestoRevolucionario.ajusteVisible(s,"b"));
        assertTrue(ImpuestoRevolucionario.resolver(s,"p","uno",25,20));assertFalse(ImpuestoRevolucionario.resolver(s,"p","uno",20,25));assertEquals(40L,s.child("puntajesRondas/ronda_1/b").getValue());}
    @Test public void empatePerdidaYAbandonoDevuelven(){for(int caso=0;caso<3;caso++){MutableData s=sala();assertTrue(ImpuestoRevolucionario.prestar(s,"p",2,"a","b","uno"));if(caso==2)s.child("jugadores/b").setValue(null);
        assertTrue(ImpuestoRevolucionario.resolver(s,"p","uno",caso==2?25:20,caso==0?20:caso==2?20:25));assertEquals(0,ImpuestoRevolucionario.ajusteVisible(s,"a"));}}
    @Test public void noReprestaDineroAjenoYRespetaDisponible(){MutableData s=sala();s.child("puntajesRondas/ronda_1/b").setValue(3);s.child("puntajesRondas/ronda_2/c").setValue(100);
        assertTrue(ImpuestoRevolucionario.prestar(s,"p",2,"c","b","uno"));assertEquals(3,ImpuestoRevolucionario.ajusteVisible(s,"c"));assertEquals(0,ImpuestoRevolucionario.disponible(s,"c",2));
        assertFalse(ImpuestoRevolucionario.prestar(s,"p",2,"a","c","dos"));assertNull(s.child("prestamosImpuesto/dos").getValue());}
}
