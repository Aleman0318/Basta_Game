package com.example.basta_game.utils;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DecretoSilencioTest {
    @Test public void nuncaProhibeLaVocalOficial() {
        for(String letra:Arrays.asList("A","E","I","O","U","á"))for(int n=0;n<100;n++) {
            String vocal=DecretoSilencio.elegirVocal(letra,n);assertTrue("AEIOU".contains(vocal));
            assertNotEquals("á".equals(letra)?"A":letra,vocal);
        }
        assertEquals("A",DecretoSilencio.elegirVocal("Ñ",0));
    }
    @Test public void sesionAntiguaNoAutorizaAQuienYaNoEsParticipante() {
        MutableData s=sala();s.child("jugadores/a").setValue(null);s.child("sesiones/a/vieja/conectada").setValue(true);
        assertFalse(DecretoSilencio.preparar(s,"p",1,"a","uso",0,2000));
    }
    @Test public void otraPartidaNoHeredaDecretosNiConfirmaciones() {
        MutableData s=sala();assertTrue(DecretoSilencio.preparar(s,"p",1,"a","viejo",0,2000));
        s.child("decretoSilencio/ronda_1/recibido/a").setValue(true);s.child("idPartida").setValue("nueva");
        assertTrue(DecretoSilencio.preparar(s,"nueva",1,"a","nuevo",1,2000));
        assertFalse(s.child("decretoSilencio/ronda_1/recibido").hasChildren());assertEquals("E",s.child("decretoSilencio/ronda_1/vocal").getValue());
    }
    private MutableData sala() {
        MutableData s=ResultadoMarcaTest.sala(40);s.child("fase").setValue("jugando");s.child("inicioRonda").setValue(1000);s.child("tiempoRonda").setValue(60);
        s.child("letraActual").setValue("B");
        s.child("categoriasActivas").setValue(Arrays.asList("País","Animal","Color"));return s;
    }
    @Test public void seleccionDosDistintasEsDeterminista() {
        for(int n=0;n<100;n++) {List<String> c=DecretoSilencio.elegirCategorias(Arrays.asList("País","Animal","Color","Animal"),n);
            assertEquals(2,new HashSet<>(c).size());assertEquals(c,DecretoSilencio.elegirCategorias(Arrays.asList("País","Animal","Color","Animal"),n));}
        assertTrue(DecretoSilencio.elegirCategorias(Arrays.asList("Animal","Animal"),0).isEmpty());
    }
    @Test public void unaVocalCompartidaNoSeReeligeAlReentrar() {
        MutableData s=sala();assertTrue(DecretoSilencio.preparar(s,"p",1,"a","uso",0,2000));
        assertEquals("A",s.child("decretoSilencio/ronda_1/vocal").getValue());
        assertFalse(DecretoSilencio.preparar(s,"p",1,"v","otra",1,3000));
        assertEquals("uso",s.child("decretoSilencio/ronda_1/idUso").getValue());
    }
    @Test public void vocalesAcentuadasYNiEnieNoSeConfunden() {
        for(String[] p:new String[][]{{"Árbol","A"},{"té","E"},{"sí","I"},{"avión","O"},{"pingüino","U"},{"a\u0301","A"}})
            assertTrue(DecretoSilencio.infringe(p[0],p[1]));
        assertFalse(DecretoSilencio.infringe("Ñn","A"));assertFalse(DecretoSilencio.infringe("","A"));assertFalse(DecretoSilencio.infringe(null,"A"));
    }
    private MutableData evaluable(String respuesta) {
        MutableData s=sala();assertTrue(DecretoSilencio.preparar(s,"p",1,"a","uso",0,2000));
        s.child("decretoSilencio/ronda_1/categorias").setValue(Arrays.asList("Animal","País"));
        s.child("fase").setValue("revision_puntuaciones");
        for(String uid:Arrays.asList("a","v")) {s.child("decretoSilencio/ronda_1/recibido").child(uid).setValue(true);
            for(String cat:Arrays.asList("Animal","País")) {s.child("respuestas/ronda_1").child(uid).child(cat).setValue(respuesta);s.child("validaciones/ronda_1").child(uid).child(cat).child("puntos").setValue(10);}}
        return s;
    }
    @Test public void categoriasNoAfectadasNoCambian() {MutableData s=evaluable("Águila");assertFalse(DecretoSilencio.evaluar(s,"p",1,"Color"));assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());}
    @Test public void cumplirNoOtorgaBonus() {MutableData s=evaluable("Lince");assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());assertEquals(0L,s.child("decretoSilencio/ronda_1/evaluaciones/Animal/a/multa").getValue());}
    @Test public void infraccionCeroYUnaMultaPorCategoriaSinRepetir() {
        MutableData s=evaluable("AaaÁ");assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        // Durante la votación, el Decreto solo registra la infracción: el +10 sigue provisional.
        assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());
        assertEquals(10L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        assertFalse(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        assertTrue(DecretoSilencio.evaluar(s,"p",1,"País"));
        DecretoSilencio.aplicarAlResultado(s,"p",1,"Animal");
        DecretoSilencio.aplicarAlResultado(s,"p",1,"Animal"); // Reintento idempotente.
        DecretoSilencio.aplicarAlResultado(s,"p",1,"País");
        assertEquals(0L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        assertEquals(0L,s.child("validaciones/ronda_1/a/País/puntos").getValue());
        assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());
        assertEquals("AaaÁ",s.child("respuestas/ronda_1/a/Animal").getValue());
    }
    @Test public void vaciaNoEsUnaInfraccion() {
        for(String respuesta:Arrays.asList("","   ","Sin respuesta")) {
            MutableData s=evaluable(respuesta);assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));assertEquals(0L,s.child("decretoSilencio/ronda_1/evaluaciones/Animal/a/multa").getValue());
        }
    }
    @Test public void sinConfirmacionNoHayMultaRetroactiva() {MutableData s=evaluable("Águila");s.child("decretoSilencio/ronda_1/recibido/a").setValue(null);assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());}
    @Test public void rondaCerradaBastaYTiempoInsuficienteNoActivan() {
        MutableData s=sala();assertFalse(DecretoSilencio.preparar(s,"p",1,"a","uso",0,42000));
        s.child("bastaTimeStamp").setValue(3000);assertFalse(DecretoSilencio.preparar(s,"p",1,"a","uso",0,4000));
        s.child("fase").setValue("revision_puntuaciones");assertFalse(DecretoSilencio.preparar(s,"p",1,"a","uso",0,2000));
    }
    @Test public void respuestasYaBloqueadasImpidenUnDecretoTardio() {MutableData s=sala();s.child("respuestas/ronda_1/v/Animal").setValue("Abeja");assertFalse(DecretoSilencio.preparar(s,"p",1,"a","uso",0,2000));}
    @Test public void reconexionRecuperaResultadoSinMultarDosVeces() {
        MutableData s=evaluable("Abeja");assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        MutableData recuperada=MutableDataLocal.desde((Map<String,Object>)s.getValue());assertEquals(s.getValue(),recuperada.getValue());
        assertFalse(DecretoSilencio.evaluar(recuperada,"p",1,"Animal"));assertEquals(40L,recuperada.child("puntajesRondas/ronda_1/a").getValue());
    }

    @Test public void evaluacionInmediataNoDescuentaTotalDosVeces() {
        MutableData s=evaluable("Águila");
        assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());
        assertTrue(Boolean.TRUE.equals(s.child("decretoSilencio/ronda_1/evaluaciones/Animal/a/infraccion").getValue()));
        assertEquals(10L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        DecretoSilencio.aplicarAlResultado(s,"p",1,"Animal");
        assertEquals(0L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        int multa=DecretoSilencio.invalidarEnCierre(s,"p",1,"a");
        assertEquals(10,multa);
        assertEquals(20,DecretoSilencio.aplicarMultaEnCierre(s,"p",1,"a",30,multa));
    }
    @Test public void nuevaConfirmacionDespuesDeEvaluarNoCreaMultaRetroactiva() {
        MutableData s=evaluable("Águila");
        s.child("decretoSilencio/ronda_1/recibido/a").setValue(null);
        assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        s.child("decretoSilencio/ronda_1/recibido/a").setValue(true);
        assertEquals(5,DecretoSilencio.invalidarEnCierre(s,"p",1,"a")); // Solo País, no Animal.
        assertFalse(Boolean.TRUE.equals(s.child("decretoSilencio/ronda_1/evaluaciones/Animal/a/infraccion").getValue()));
    }
    @Test public void faseJugandoAunPermiteEvaluacionAlIniciarRevision() {
        MutableData s=evaluable("Abeja");
        s.child("fase").setValue("jugando");
        assertTrue(DecretoSilencio.evaluar(s,"p",1,"Animal"));
        // Aunque la fase siga en jugando, no se adelanta el +0 durante la votación.
        assertEquals(10L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        DecretoSilencio.aplicarAlResultado(s,"p",1,"Animal");
        assertEquals(0L,s.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        assertEquals(40L,s.child("puntajesRondas/ronda_1/a").getValue());
    }
    @Test public void nuevoCierreDecretoInvalidaDosCategoriasYRegistraMultas() {
        MutableData sala=evaluable("Águila");
        sala.child("fase").setValue("revision_puntuaciones");
        int nominal=DecretoSilencio.invalidarEnCierre(sala,"p",1,"a");
        assertEquals(10,nominal);
        assertEquals(0L,sala.child("validaciones/ronda_1/a/Animal/puntos").getValue());
        assertEquals(0L,sala.child("validaciones/ronda_1/a/País/puntos").getValue());
        int luego=DecretoSilencio.aplicarMultaEnCierre(sala,"p",1,"a",30,nominal);
        assertEquals(20,luego);
        assertEquals(10,((Number)sala.child("decretoSilencio/ronda_1/resultados/a/multaAplicada").getValue()).intValue());
        assertEquals("Águila",sala.child("respuestas/ronda_1/a/Animal").getValue());
    }
    @Test public void multasNoCreanSaldoNegativoPeroRespetanDeudaDobleONada() {
        MutableData sala=evaluable("Águila");
        int nominal=DecretoSilencio.invalidarEnCierre(sala,"p",1,"a");
        assertEquals(0,DecretoSilencio.aplicarMultaEnCierre(sala,"p",1,"a",4,nominal));
        assertEquals(-15,DecretoSilencio.aplicarMultaEnCierre(sala,"p",1,"a",-15,nominal));
    }
    @Test public void sinAvisoConfirmadoNoHayMulta() {
        MutableData sala=evaluable("Águila");
        sala.child("decretoSilencio/ronda_1/recibido/a").setValue(null);
        assertEquals(0,DecretoSilencio.invalidarEnCierre(sala,"p",1,"a"));
        assertEquals(10L,sala.child("validaciones/ronda_1/a/Animal/puntos").getValue());
    }
}
