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

            // --- LÓGICA ALEATORIA CON PROBABILIDADES PONDERADAS (6 PARTES) ---
            // [0, 1, 2] = Tres en Raya (50%)
            // [3, 4]    = Piedra, Papel o Tijera (33.3%)
            // [5]       = Duelo de Reflejos / Tiroteo (16.7%)
            String[] ruletaPonderada = {
                    "tres_en_raya", "tres_en_raya", "tres_en_raya",
                    "piedra_papel_tijera", "piedra_papel_tijera",
                    "duelo_reflejos"
            };

            String juegoElegido = ruletaPonderada[new Random().nextInt(ruletaPonderada.length)];

            HashMap<String, Object> updates = new HashMap<>();

            // --- FASE B: LA CREACIÓN DE LA ARENA ---
            HashMap<String, Object> minijuegoData = new HashMap<>();
            minijuegoData.put("activo", true);
            minijuegoData.put("tipoJuego", juegoElegido);
            minijuegoData.put("uidAtacante", miUid);
            minijuegoData.put("uidVictima", uidVictima);

            // Nombres para los espectadores
            minijuegoData.put("nombreAtacante", nombreJugador);
            minijuegoData.put("nombreVictima", nombreVictima);

            // ESTADO INICIAL: "anuncio"
            minijuegoData.put("estado", "anuncio");
            minijuegoData.put("ganadorUid", "");

            // --- INICIALIZACIÓN DINÁMICA SEGÚN EL MINIJUEGO ---
            HashMap<String, Object> datosJuegoMap = new HashMap<>();
            datosJuegoMap.put("turnoDe", miUid); // El Atacante siempre da el primer golpe por defecto

            if (juegoElegido.equals("piedra_papel_tijera")) {
                datosJuegoMap.put("eleccionA", "");
                datosJuegoMap.put("eleccionV", "");
                datosJuegoMap.put("puntosA", 0);
                datosJuegoMap.put("puntosV", 0);
            }
            else if (juegoElegido.equals("tres_en_raya")) {
                // Creamos las 9 casillas (c0 a c8) vacías para el tablero
                for (int i = 0; i < 9; i++) {
                    datosJuegoMap.put("c" + i, "");
                }
                datosJuegoMap.put("histA", "");
                datosJuegoMap.put("histV", "");
            }
            else if (juegoElegido.equals("duelo_reflejos")) {
                datosJuegoMap.put("reaccionA", 0L); // Tiempo en ms
                datosJuegoMap.put("reaccionV", 0L); // Tiempo en ms

                // Generamos el tiempo de espera aleatorio (entre 2 y 5 segundos) UNA sola vez para ambos
                datosJuegoMap.put("delayTiroteo", 2000L + new Random().nextInt(3000));
            }

            minijuegoData.put("datosJuego", datosJuegoMap);
            updates.put("minijuegoActual", minijuegoData);
            String uso = "comodinesUsados/ronda_" + rondaActual + "/marca_" + miUid + "_" + System.currentTimeMillis();
            updates.put(uso + "/tipo", ID_COMODIN);
            updates.put(uso + "/uid", miUid);
            updates.put(uso + "/nombreJugador", nombreJugador);
            updates.put(uso + "/timestamp", com.google.firebase.database.ServerValue.TIMESTAMP);

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
            case "tres_en_raya": return "Tres en Raya Infinito";
            case "duelo_reflejos": return "Duelo de Reflejos";
            default: return "Muerte Súbita";
        }
    }
}
