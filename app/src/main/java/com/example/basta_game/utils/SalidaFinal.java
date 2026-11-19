package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;

/** D4: cada jugador abandona resultados de forma independiente.
 * La sala se reinicia SOLO cuando todos los presentes hayan abandonado los resultados.
 */
public final class SalidaFinal {
    private SalidaFinal() { }

    public static boolean registrar(MutableData sala,String partida,String uid,String siguienteLetra) {
        if (sala.getValue()==null || partida==null || uid==null ||
                !partida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"resultados_finales".equals(sala.child("fase").getValue(String.class)) ||
                !sala.child("jugadores").hasChild(uid)) return false;
        sala.child("salidasFinales").child(uid).setValue(true);
        for (MutableData jugador:sala.child("jugadores").getChildren()) {
            if (!Boolean.TRUE.equals(sala.child("salidasFinales").child(jugador.getKey()).getValue()))
                return true;
        }
        // Ya nadie conserva abierta la pantalla de resultados: reinicio atómico.
        sala.child("estadoPartida").setValue("Esperando jugadores");
        sala.child("fase").setValue("lobby");
        sala.child("rondaActual").setValue(0);
        sala.child("letraActual").setValue(siguienteLetra);
        for(String ruta:new String[]{
                "inicioRonda","inicioRondaDe","inicioRondaListoDe",
                "bastaPresionadoPor","bastaTimeStamp","respuestas","validaciones",
                "rondasProcesadas","revisionLista","resumenRondaListo","puntajesRondas",
                "votacionActiva","votacionesUsadas","historialVotaciones",
                "resultadosVotacionPalabras","cooldownVotacion","revisionCategorias",
                "comodinesPendientes","comodinesActivos","comodinesUsados",
                "comodinesResultados","reseteoFinalEnProceso","cambioRondaTimestamp",
                "historialGuardado","entradaPartida","esperaInicioRonda",
                "efectosVisuales","poderesRonda","salidasFinales"
        }) sala.child(ruta).setValue(null);
        sala.child("bastaPresionado").setValue(false);
        // Reinicia todos los comodines tambien al salir tras una partida abortada.
        ReinicioComodines.nuevaPartida(sala);
        sala.child("finPartida").setValue(ServerValue.TIMESTAMP);
        for(MutableData jugador:sala.child("jugadores").getChildren()) {
            jugador.child("estaListo").setValue(false);
            jugador.child("puntajePartida").setValue(0);
        }
        return true;
    }
}
