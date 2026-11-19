package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.MutableDataLocal;
import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Two possible serialization orders of ready vs. death-match activation. */
public class ContinuarRondaSinChoqueMarcaTest {
    private MutableData sala() {
        MutableData sala = MutableDataLocal.desde(new HashMap<>());
        sala.child("idPartida").setValue("partidaActual");
        sala.child("hostUID").setValue("a");
        sala.child("rondaActual").setValue(2);
        sala.child("fase").setValue("revision_puntuaciones");
        for (String uid : new String[]{"a", "b", "c", "d"}) {
            sala.child("jugadores/" + uid + "/estaConectado").setValue(true);
        }
        return sala;
    }
    private Map<String,Object> cambios() {
        Map<String,Object> map = new HashMap<>();
        map.put("rondaActual",3);
        map.put("fase","jugando");
        return map;
    }
    @Test public void alguienListoPrimeroImpideIniciarMarcaSinAlterarSala() {
        MutableData sala=sala();
        assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"a"));
        Object antes = sala.getValue();
        assertFalse(SeleccionMinijuego.publicar(sala,2,"b","c","B","C",0,0,"uso1"));
        assertEquals(antes,sala.getValue());
        assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"b"));
        assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"c"));
        assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"d"));
        assertTrue(InicioPartida.avanzar(sala,"a","partidaActual",2,cambios()));
    }
    @Test public void marcaPrimeroCongelaConfirmacionesYAvanceHastaLiquidacion() {
        MutableData sala=sala();
        assertTrue(SeleccionMinijuego.publicar(sala,2,"b","c","B","C",0,0,"uso2"));
        assertTrue(SeleccionMinijuego.dueloEnCurso(sala));
        Object antes=sala.getValue();
        assertFalse(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"a"));
        assertFalse(InicioPartida.avanzar(sala,"a","partidaActual",2,cambios()));
        assertEquals(antes,sala.getValue());
        // Duel resolution removes the minigame only after its result is committed.
        sala.child("minijuegoActual").setValue(null);
        for (String uid : new String[]{"a", "b", "c", "d"})
            assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,uid));
        assertTrue(InicioPartida.avanzar(sala,"a","partidaActual",2,cambios()));
    }
    @Test public void dueloNoPuedeNacerDespuesDeCambiarRondaNiContraSiMismo() {
        MutableData sala=sala();
        assertFalse(SeleccionMinijuego.publicar(sala,2,"a","a","A","A",0,0,"uso"));
        for (String uid : new String[]{"a","b","c","d"})
            assertTrue(InicioPartida.marcarListoRevision(sala,"partidaActual",2,uid));
        assertTrue(InicioPartida.avanzar(sala,"a","partidaActual",2,cambios()));
        Object antes=sala.getValue();
        assertFalse(SeleccionMinijuego.publicar(sala,2,"b","c","B","C",0,0,"uso3"));
        assertEquals(antes,sala.getValue());
    }
    @Test public void noSeAceptaListoDeOtraPartidaOUsuarioAjeno() {
        MutableData sala=sala();
        Object antes=sala.getValue();
        assertFalse(InicioPartida.marcarListoRevision(sala,"partidaAntigua",2,"a"));
        assertFalse(InicioPartida.marcarListoRevision(sala,"partidaActual",2,"intruso"));
        assertEquals(antes,sala.getValue());
    }
}
