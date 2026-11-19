package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.example.basta_game.utils.ConexionJuego;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.util.UUID;

/** Viento: un único evento compartido por ronda, sin tocar respuestas ni puntos. */
public class ComodinVientoDelCaos extends BaseComodin {
    public static final String ID = "viento_caos";
    private final String idPartida;

    public ComodinVientoDelCaos(String idSala, String idPartida, int ronda) {
        super(idSala, ronda);
        this.idPartida = idPartida;
    }

    @Override public void activar(String nombreJugador, ComodinCallback callback) {
        if (!ConexionJuego.disponible() || idPartida == null || idPartida.isEmpty()) {
            callback.onError("Necesitas conexión y una partida activa");
            return;
        }
        final long version = ConexionJuego.version();
        salaRef.get().addOnSuccessListener(sala -> {
            if (!ConexionJuego.vigente(version) ||
                    !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                    !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                    !sala.child("jugadores").hasChild(miUid) ||
                    !(sala.child("rondaActual").getValue() instanceof Number) ||
                    ((Number) sala.child("rondaActual").getValue()).intValue() != rondaActual ||
                    sala.child("respuestas/ronda_" + rondaActual).hasChild(miUid)) {
                callback.onError("Viento del Caos no está disponible en esta fase");
                return;
            }
            final DatabaseReference efectoRef = salaRef.child("efectosVisuales")
                    .child("vientoCaos").child("ronda_" + rondaActual);
            if (sala.child("efectosVisuales/vientoCaos/ronda_" + rondaActual).exists()) {
                callback.onError("El Viento del Caos ya se activó en esta ronda");
                return;
            }
            final DatabaseReference unidades = usuarioRef.child("inventarioComodines")
                    .child(ID).child("cantidad");
            com.example.basta_game.utils.ConsumoInventario.descontar(unidades, (descontado, mensaje) -> {
                if (!descontado) { callback.onError(mensaje); return; }
                    final String uso = UUID.randomUUID().toString();
                    final long semilla = System.nanoTime();
                    efectoRef.runTransaction(new Transaction.Handler() {
                        @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData dato) {
                            if (dato.getValue() != null) return Transaction.abort();
                            dato.child("idPartida").setValue(idPartida);
                            dato.child("ronda").setValue(rondaActual);
                            dato.child("uid").setValue(miUid);
                            dato.child("idUso").setValue(uso);
                            dato.child("nombreJugador").setValue(nombreJugador);
                            dato.child("semilla").setValue(semilla);
                            return Transaction.success(dato);
                        }
                        @Override public void onComplete(DatabaseError error2, boolean confirmado, DataSnapshot resultado) {
                            if (error2 == null && confirmado) {
                                callback.onSuccess("¡Viento del Caos! Las categorías se desordenaron para todos");
                            } else {
                                // Compensación ante evento rechazado. No duplica un efecto.
                                unidades.runTransaction(new Transaction.Handler() {
                                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData dato) {
                                        Object valor = dato.getValue();
                                        if (!(valor instanceof Number)) return Transaction.abort();
                                        dato.setValue(((Number) valor).longValue() + 1);
                                        return Transaction.success(dato);
                                    }
                                    @Override public void onComplete(DatabaseError error3, boolean devuelto, DataSnapshot resultado3) {
                                        callback.onError(devuelto && error3 == null
                                                ? "Ya se activó Viento o hubo un error; recuperaste tu unidad"
                                                : "No se confirmó Viento; revisa tu inventario antes de reintentar");
                                    }
                                }, false);
                            }
                        }
                    }, false);
            });
        }).addOnFailureListener(error -> callback.onError("No se pudo comprobar la ronda"));
    }
}
