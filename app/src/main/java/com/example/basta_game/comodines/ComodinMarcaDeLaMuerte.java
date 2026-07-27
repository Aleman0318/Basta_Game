package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Random;

public class ComodinMarcaDeLaMuerte extends BaseComodin {

    public static final String ID_COMODIN = "marca_de_la_muerte";

    // Variables exclusivas de este comodín premium
    private String uidVictima;
    private String nombreVictima;

    public ComodinMarcaDeLaMuerte(String idSala, int rondaActual, String uidVictima, String nombreVictima) {
        super(idSala, rondaActual);
        this.uidVictima = uidVictima;
        this.nombreVictima = nombreVictima;
    }

    @Override
    public void activar(String nombreJugador, ComodinCallback callback) {
        descontarInventario(ID_COMODIN, callback, () -> {

            String[] juegos = {"piedra_papel_tijera"};
            String juegoElegido = juegos[new Random().nextInt(juegos.length)];

            HashMap<String, Object> updates = new HashMap<>();

            // --- FASE B: LA CREACIÓN DE LA ARENA ---
            HashMap<String, Object> minijuegoData = new HashMap<>();
            minijuegoData.put("activo", true);
            minijuegoData.put("tipoJuego", juegoElegido);
            minijuegoData.put("uidAtacante", miUid);
            minijuegoData.put("uidVictima", uidVictima);

            // ¡AQUÍ AGREGAMOS LOS NOMBRES PARA LOS ESPECTADORES!
            minijuegoData.put("nombreAtacante", nombreJugador);
            minijuegoData.put("nombreVictima", nombreVictima);

            // ESTADO INICIAL: "anuncio"
            minijuegoData.put("estado", "anuncio");
            minijuegoData.put("ganadorUid", "");

            HashMap<String, Object> datosJuegoMap = new HashMap<>();
            datosJuegoMap.put("turnoDe", miUid);
            datosJuegoMap.put("eleccionA", "");
            datosJuegoMap.put("eleccionV", "");
            datosJuegoMap.put("puntosA", 0);
            datosJuegoMap.put("puntosV", 0);

            minijuegoData.put("datosJuego", datosJuegoMap);
            updates.put("minijuegoActual", minijuegoData);

            salaRef.updateChildren(updates).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    callback.onSuccess("SILENCIO");
                } else {
                    callback.onError("Error al invocar la arena de combate.");
                }
            });
        });
    }

    /**
     * Da formato bonito al nombre del juego para el texto de notificación
     */
    private String formatearNombreJuego(String juego) {
        switch (juego) {
            case "piedra_papel_tijera": return "Piedra, Papel o Tijera";
            case "tres_en_raya": return "Tres en Raya";
            case "duelo_reflejos": return "Duelo de Reflejos";
            default: return "Muerte Súbita";
        }
    }
}