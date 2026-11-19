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

public class ComodinCambioDeFlujo extends BaseComodin {

    public static final String ID_COMODIN = "uno_reversa";

    public ComodinCambioDeFlujo(String idSala, int rondaActual) {
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
                                callback.onError("Aún no hay puntuaciones para intercambiar.");
                                return;
                            }

                            List<JugadorPuntaje> ranking = new ArrayList<>();
                            for (DataSnapshot snap : snapshotPuntos.getChildren()) {
                                Integer pts = snap.getValue(Integer.class);
                                if (snap.getKey() != null && pts != null) {
                                    ranking.add(new JugadorPuntaje(snap.getKey(), pts));
                                }
                            }
                            // Ordenamos de mayor a menor
                            Collections.sort(ranking, (a, b) -> Integer.compare(b.puntaje, a.puntaje));

                            // Descubrimos en qué posición voy antes de hacer la magia
                            int miPosicion = -1;
                            for (int i = 0; i < ranking.size(); i++) {
                                if (ranking.get(i).uid.equals(miUid)) {
                                    miPosicion = i;
                                    break;
                                }
                            }

                            String uidObjetivo = elegirObjetivoIntercambio(ranking, miUid);
                            int misPuntos = obtenerPuntosDeLista(ranking, miUid);
                            int susPuntos = obtenerPuntosDeLista(ranking, uidObjetivo);

                            // ¿De cuánto será el golpe si me equivoco?
                            int puntosCastigo = 5; // Castigo base genérico
                            if (ranking.size() == 2) {
                                puntosCastigo = 15; // 1 vs 1 = Castigo duro
                            } else if (miPosicion == 0) {
                                puntosCastigo = 20; // Voy ganando = Castigo brutal
                            }

                            final int castigoFinal = puntosCastigo;

                            HashMap<String, Object> updates = new HashMap<>();
                            String idUso = "flujo_" + rondaActual + "_" + miUid + "_" + System.currentTimeMillis();

                            // Obtenemos el nombre de la víctima para el chisme global
                            salaRef.child("jugadores").child(uidObjetivo).child("nombre")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshotNombre) {
                                            String nombreVictima = snapshotNombre.getValue(String.class);
                                            if (nombreVictima == null) nombreVictima = "un jugador";

                                            String tituloGlobal;
                                            String mensajeGlobal;
                                            boolean esExitoGlobal;

                                            if (uidObjetivo.equals(miUid)) {
                                                // CASTIGO (Cayó en la trampa de la probabilidad)
                                                updates.put("puntajesRondas/ronda_" + rondaActual + "/" + miUid, Math.max(0, misPuntos - castigoFinal));
                                                updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/resultado", "castigo");

                                                tituloGlobal = "¡EL KARMA GOLPEÓ A <font color='#E53935'>" + nombreJugador.toUpperCase() + "</font>!";
                                                mensajeGlobal = "<b>" + nombreJugador + "</b> intentó cambiar el flujo y terminó perdiendo <font color='#E53935'><b>" + castigoFinal + " puntos</b></font>.";
                                                esExitoGlobal = false;
                                            } else {
                                                // ÉXITO
                                                updates.put("puntajesRondas/ronda_" + rondaActual + "/" + miUid, susPuntos);
                                                updates.put("puntajesRondas/ronda_" + rondaActual + "/" + uidObjetivo, misPuntos);
                                                updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/resultado", "intercambio");
                                                updates.put("comodinesResultados/ronda_" + rondaActual + "/" + idUso + "/victima", uidObjetivo);

                                                tituloGlobal = "¡ROBO MAESTRO DE <font color='#2AA514'>" + nombreJugador.toUpperCase() + "</font>!";
                                                mensajeGlobal = "<b>" + nombreJugador + "</b> intercambió su puntaje con <b>" + nombreVictima + "</b>.";
                                                esExitoGlobal = true;
                                            }

                                            // NUEVO NODO GLOBAL: Para que todos los jugadores vean la tarjeta animada
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/idUso", idUso);
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/titulo", tituloGlobal);
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/mensaje", mensajeGlobal);
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/esExito", esExitoGlobal);
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/timestamp", ServerValue.TIMESTAMP);

                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidAtacante", miUid);
                                            updates.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidVictima", uidObjetivo);

                                            // Resolver protección y puntuación en UNA transacción de sala.
                                            // Si el objetivo tiene Escudo, se publica PARRY y el atacante
                                            // pierde la unidad usada, pero no se tocan los puntos.
                                            final String victimaNombreFinal = nombreVictima;
                                            salaRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                                                @NonNull @Override public com.google.firebase.database.Transaction.Result doTransaction(
                                                        @NonNull com.google.firebase.database.MutableData sala) {
                                                    Object numeroRonda = sala.child("rondaActual").getValue();
                                                    if (sala.getValue() == null ||
                                                            !"revision_puntuaciones".equals(sala.child("fase").getValue()) ||
                                                            !(numeroRonda instanceof Number) ||
                                                            ((Number) numeroRonda).intValue() != rondaActual ||
                                                            !sala.child("jugadores").hasChild(miUid) ||
                                                            uidObjetivo == null ||
                                                            !sala.child("jugadores").hasChild(uidObjetivo)) {
                                                        return com.google.firebase.database.Transaction.abort();
                                                    }
                                                    String partida = sala.child("idPartida").getValue(String.class);
                                                    if (!uidObjetivo.equals(miUid) &&
                                                            com.example.basta_game.utils.DefensaEscudoDorado.registrarBloqueo(
                                                                    sala, partida, rondaActual, uidObjetivo, miUid,
                                                                    nombreJugador, victimaNombreFinal, idUso, "uno_reversa")) {
                                                        return com.google.firebase.database.Transaction.success(sala);
                                                    }
                                                    // Recalcular puntos sobre el estado actual, no sobre la
                                                    // lectura anterior: evita sobrescribir cambios concurrentes.
                                                    Object misActuales = sala.child("puntajesRondas/ronda_" + rondaActual +
                                                            "/" + miUid).getValue();
                                                    Object susActuales = sala.child("puntajesRondas/ronda_" + rondaActual +
                                                            "/" + uidObjetivo).getValue();
                                                    if (!(misActuales instanceof Number) ||
                                                            !(susActuales instanceof Number))
                                                        return com.google.firebase.database.Transaction.abort();
                                                    int puntosMios = ((Number) misActuales).intValue();
                                                    int puntosVictima = ((Number) susActuales).intValue();
                                                    if (uidObjetivo.equals(miUid)) {
                                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + miUid,
                                                                Math.max(0, puntosMios - castigoFinal));
                                                    } else {
                                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + miUid, puntosVictima);
                                                        updates.put("puntajesRondas/ronda_" + rondaActual + "/" + uidObjetivo, puntosMios);
                                                    }
                                                    for (java.util.Map.Entry<String, Object> entry : updates.entrySet())
                                                        sala.child(entry.getKey()).setValue(entry.getValue());
                                                    return com.google.firebase.database.Transaction.success(sala);
                                                }
                                                @Override public void onComplete(com.google.firebase.database.DatabaseError error,
                                                                                 boolean confirmado,
                                                                                 com.google.firebase.database.DataSnapshot datos) {
                                                    if (error == null && confirmado) callback.onSuccess("SILENCIO");
                                                    else callback.onError("No se pudo confirmar Cambio de Flujo; revisa tu inventario");
                                                }
                                            }, false);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            callback.onError("Error al resolver el comodín.");
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

    private int obtenerPuntosDeLista(List<JugadorPuntaje> lista, String uid) {
        for (JugadorPuntaje jp : lista) {
            if (jp.uid.equals(uid)) return jp.puntaje;
        }
        return 0;
    }

    private String elegirObjetivoIntercambio(List<JugadorPuntaje> rankingDescendente, String uidAtacante) {
        int n = rankingDescendente.size();
        if (n <= 1) return uidAtacante;

        int miPosicion = -1;
        for (int i = 0; i < n; i++) {
            if (rankingDescendente.get(i).uid.equals(uidAtacante)) {
                miPosicion = i;
                break;
            }
        }

        double[] probabilidades = new double[n];

        // --- LÓGICA DE PROBABILIDAD ESTRICTA ---
        if (n == 2) {
            // Regla: 1 vs 1 = 65% de probabilidad de auto-castigo
            for (int i = 0; i < n; i++) {
                if (i == miPosicion) probabilidades[i] = 0.65;
                else probabilidades[i] = 0.35;
            }
        }
        else if (miPosicion == 0) {
            // Regla: Va en 1er lugar (3+ jugadores) = 70% de probabilidad de auto-castigo
            probabilidades[0] = 0.70;
            // Distribuimos el 30% restante de salvación equitativamente entre los demás
            double resto = 0.30;
            for (int i = 1; i < n; i++) {
                probabilidades[i] = resto / (n - 1);
            }
        }
        else {
            // Casos normales (Va perdiendo o en medio en partidas grandes)
            if (n == 3) {
                for (int i = 0; i < n; i++) {
                    if (i == miPosicion) probabilidades[i] = 0.25; // Menos riesgo de equivocarse
                    else probabilidades[i] = 0.75 / (n - 1);
                }
            } else {
                if (miPosicion == n - 1) { // Es el ÚLTIMO lugar (Desesperado, tiene mucha ventaja)
                    probabilidades[0] = 0.04;
                    probabilidades[1] = 0.06;
                    probabilidades[2] = 0.24;
                    probabilidades[3] = 0.30;
                    double resto = 0.36;
                    int quedan = n - 4;
                    if(quedan > 0){
                        for (int i = 4; i < n; i++) probabilidades[i] = resto / quedan;
                    }
                    probabilidades[miPosicion] = 0.0; // Imposible que se auto-castigue si va último
                } else { // Va en medio de la tabla
                    for (int i = 0; i < n; i++) {
                        if (i == miPosicion) probabilidades[i] = 0.15;
                        else probabilidades[i] = 0.85 / (n - 1);
                    }
                }
            }
        }

        // Tiramos los dados con la probabilidad
        double rand = Math.random();
        double acumulado = 0.0;
        for (int i = 0; i < n; i++) {
            acumulado += probabilidades[i];
            if (rand <= acumulado) {
                return rankingDescendente.get(i).uid;
            }
        }
        return rankingDescendente.get(n - 1).uid;
    }
}