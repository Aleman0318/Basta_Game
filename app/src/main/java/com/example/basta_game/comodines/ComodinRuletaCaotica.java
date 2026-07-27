package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ComodinRuletaCaotica extends BaseComodin {

    public static final String ID_COMODIN = "ruleta_caotica";

    public ComodinRuletaCaotica(String idSala, int rondaActual) {
        super(idSala, rondaActual);
    }

    public static class JugadorPuntaje {
        public String uid;
        public int puntaje;
        public JugadorPuntaje(String uid, int puntaje) { this.uid = uid; this.puntaje = puntaje; }
    }

    @Override
    public void activar(String nombreJugador, ComodinCallback callback) {
        descontarInventario(ID_COMODIN, callback, () -> {
            salaRef.child("puntajesRondas").child("ronda_" + rondaActual)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshotPuntos) {
                            if (!snapshotPuntos.exists()) {
                                callback.onError("Aún no hay puntuaciones para la ruleta.");
                                return;
                            }

                            List<JugadorPuntaje> ranking = new ArrayList<>();
                            for (DataSnapshot snap : snapshotPuntos.getChildren()) {
                                Integer pts = snap.getValue(Integer.class);
                                if (snap.getKey() != null && pts != null) {
                                    ranking.add(new JugadorPuntaje(snap.getKey(), pts));
                                }
                            }

                            if (ranking.isEmpty()) {
                                callback.onError("No hay jugadores suficientes.");
                                return;
                            }

                            // Ordenar de mayor a menor
                            Collections.sort(ranking, (a, b) -> Integer.compare(b.puntaje, a.puntaje));

                            // TIRAMOS EL DADO (0 al 4)
                            Random random = new Random();
                            int efecto = random.nextInt(5);

                            HashMap<String, Object> updates = new HashMap<>();
                            String idUso = "ruleta_" + rondaActual + "_" + miUid + "_" + System.currentTimeMillis();

                            String tituloGlobal = "¡RULETA CAÓTICA!";
                            String mensajeGlobal = "";
                            boolean esExitoGlobal = true;
                            String simboloAnimacion = "🎰";

                            switch (efecto) {
                                case 0: // 1. El Comunismo (Promedio Total)
                                    int suma = 0;
                                    for (JugadorPuntaje jp : ranking) suma += jp.puntaje;
                                    int promedio = suma / ranking.size();
                                    for (JugadorPuntaje jp : ranking) {
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + jp.uid, promedio);
                                    }
                                    mensajeGlobal = nombreJugador + " trajo el COMUNISMO ☭. Todos los puntos de la ronda se sumaron y repartieron en partes iguales: ¡" + promedio + " pts para todos!";
                                    simboloAnimacion = "☭";
                                    break;

                                case 1: // 2. El Carrusel (Rotación)
                                    int n = ranking.size();
                                    if (n > 1) {
                                        int puntosPrimero = ranking.get(0).puntaje;
                                        for (int i = 0; i < n - 1; i++) {
                                            updates.put("puntajesRondas/ronda_" + rondaActual + "/" + ranking.get(i).uid, ranking.get(i+1).puntaje);
                                        }
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + ranking.get(n-1).uid, puntosPrimero);
                                    }
                                    mensajeGlobal = "¡CARRUSEL 🎡! La tabla giró. Todos reciben los puntos del jugador de abajo, ¡y el último lugar robó los puntos del primero!";
                                    simboloAnimacion = "🎡";
                                    break;

                                case 2: // 3. Amnesia Colectiva (Borrón a 0)
                                    for (JugadorPuntaje jp : ranking) {
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + jp.uid, 0);
                                    }
                                    mensajeGlobal = "¡AMNESIA COLECTIVA 💥! La ruleta fue cruel. Todo el esfuerzo de esta ronda fue en vano. ¡Todos bajan a CERO puntos!";
                                    esExitoGlobal = false; // Suena triste para todos
                                    simboloAnimacion = "0";
                                    break;

                                case 3: // 4. Impuesto a la Riqueza (Robin Hood)
                                    if (ranking.size() > 1) {
                                        JugadorPuntaje primero = ranking.get(0);
                                        JugadorPuntaje ultimo = ranking.get(ranking.size() - 1);

                                        int impuesto = (int) (primero.puntaje * 0.30);
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + primero.uid, Math.max(0, primero.puntaje - impuesto));
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + ultimo.uid, ultimo.puntaje + impuesto);
                                    }
                                    mensajeGlobal = "¡ROBIN HOOD 🏹! Se le ha cobrado un impuesto del 30% al primer lugar de la ronda para regalárselo al último lugar.";
                                    simboloAnimacion = "🏹";
                                    break;

                                case 4: // 5. Ruleta Rusa Inversa (Inversión)
                                    for (JugadorPuntaje jp : ranking) {
                                        String revStr = new StringBuilder(String.valueOf(jp.puntaje)).reverse().toString();
                                        int invertido = Integer.parseInt(revStr);
                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + jp.uid, invertido);
                                    }
                                    mensajeGlobal = "¡NÚMEROS LOCOS 🙃! Los dígitos de los puntos de esta ronda se han invertido (ej. 15 ahora es 51). ¡Revisen la tabla!";
                                    simboloAnimacion = "🙃";
                                    break;
                            }

                            // Guardamos registro
                            updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/tipo", ID_COMODIN);
                            updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/atacante", miUid);
                            updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/nombreAtacante", nombreJugador);
                            updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/efecto", efecto);
                            updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/timestamp", ServerValue.TIMESTAMP);

                            // Disparamos la noticia global (El truco aquí es mandar "RULETA" como atacante para que el Fragment anime a todos)
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/idUso", idUso);
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/titulo", tituloGlobal);
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/mensaje", mensajeGlobal);
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/esExito", esExitoGlobal);
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidAtacante", "RULETA");
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidVictima", simboloAnimacion); // Mandamos el emoji como "victima" para reciclar la variable
                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/timestamp", ServerValue.TIMESTAMP);

                            salaRef.updateChildren(updates).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onSuccess("SILENCIO");
                                } else {
                                    callback.onError("No se pudo girar la ruleta.");
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError("Error al leer puntuaciones.");
                        }
                    });
        });
    }
}