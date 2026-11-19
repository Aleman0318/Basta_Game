package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.example.basta_game.utils.ConexionJuego;
import com.example.basta_game.utils.DefensaEscudoDorado;
import com.example.basta_game.utils.ConsumoInventario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import java.util.UUID;

/** Escudo manual, una unidad por ronda; protege también los resultados. */
public final class ComodinEscudoDorado extends BaseComodin {
    private final String partida;
    private final String idUso;

    public ComodinEscudoDorado(String sala, String partida, int ronda) {
        super(sala, ronda);
        this.partida = partida;
        this.idUso = "escudo_" + UUID.randomUUID();
    }

    private boolean valido(DataSnapshot sala) {
        Object valorRonda = sala.child("rondaActual").getValue();
        return partida != null && partida.equals(sala.child("idPartida").getValue(String.class)) &&
                valorRonda instanceof Number &&
                DefensaEscudoDorado.puedeEquipar(
                        sala.child("fase").getValue(String.class),
                        Boolean.TRUE.equals(sala.child("bastaPresionado").getValue(Boolean.class)) ||
                                sala.child("bastaTimeStamp").exists(),
                        DefensaEscudoDorado.yaEquipadoEnPartida(partida,
                                sala.child("poderesRonda/ronda_" + rondaActual + "/escudos/" + miUid + "/idPartida").getValue()),
                        ((Number) valorRonda).intValue(), rondaActual) &&
                sala.child("jugadores").hasChild(miUid) &&
                !sala.child("respuestas/ronda_" + rondaActual).hasChild(miUid);
    }

    @Override public void activar(String nombreJugador, ComodinCallback callback) {
        if (!ConexionJuego.disponible()) {
            callback.onError("Necesitas conexión para equipar tu Escudo Dorado");
            return;
        }
        final long version = ConexionJuego.version();
        salaRef.get().addOnCompleteListener(lectura -> {
            if (!lectura.isSuccessful() || lectura.getResult() == null ||
                    !ConexionJuego.vigente(version)) {
                callback.onError("No se pudo comprobar la ronda. Intenta de nuevo");
                return;
            }
            if (!valido(lectura.getResult())) {
                callback.onError("El Escudo Dorado ya está equipado o la ronda está cerrada");
                return;
            }
            DatabaseReference cantidad = usuarioRef.child("inventarioComodines")
                    .child(DefensaEscudoDorado.ID).child("cantidad");
            ConsumoInventario.descontar(cantidad, (descontado, mensaje) -> {
                if (!descontado) {
                    callback.onError(mensaje);
                    return;
                }
                salaRef.runTransaction(new Transaction.Handler() {
                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                        Object actualRonda = sala.child("rondaActual").getValue();
                        if (sala.getValue() == null || partida == null ||
                                !partida.equals(sala.child("idPartida").getValue()) ||
                                !(actualRonda instanceof Number) ||
                                !DefensaEscudoDorado.puedeEquipar(
                                        sala.child("fase").getValue(String.class),
                                        Boolean.TRUE.equals(sala.child("bastaPresionado").getValue()) ||
                                                sala.child("bastaTimeStamp").getValue() != null,
                                        DefensaEscudoDorado.yaEquipadoEnPartida(partida,
                                                sala.child("poderesRonda/ronda_" + rondaActual +
                                                        "/escudos/" + miUid + "/idPartida").getValue()),
                                        ((Number) actualRonda).intValue(), rondaActual) ||
                                !sala.child("jugadores").hasChild(miUid) ||
                                sala.child("respuestas/ronda_" + rondaActual).hasChild(miUid)) {
                            return Transaction.abort();
                        }
                        MutableData poderes = sala.child("poderesRonda").child("ronda_" + rondaActual);
                        String anterior = poderes.child("idPartida").getValue(String.class);
                        // La misma sala puede usarse otra vez tras un abandono.
                        // Borrar SOLAMENTE poderes obsoletos de otra partida, de forma atomica.
                        if (!partida.equals(anterior)) poderes.setValue(null);
                        poderes.child("idPartida").setValue(partida);
                        MutableData escudo = poderes.child("escudos").child(miUid);
                        escudo.child("idUso").setValue(idUso);
                        escudo.child("idPartida").setValue(partida);
                        escudo.child("nombreJugador").setValue(nombreJugador == null ? "Un jugador" : nombreJugador);
                        escudo.child("activo").setValue(true);
                        escudo.child("timestamp").setValue(ServerValue.TIMESTAMP);
                        return Transaction.success(sala);
                    }
                    @Override public void onComplete(DatabaseError error, boolean confirmado, DataSnapshot resultado) {
                        if (error == null && confirmado) {
                            callback.onSuccess("SILENCIO");
                            return;
                        }
                        // Se descontó antes: si cambió la fase, devolver una unidad.
                        cantidad.runTransaction(new Transaction.Handler() {
                            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData actual) {
                                Object valor = actual.getValue();
                                if (!(valor instanceof Number) || ((Number) valor).longValue() < 0 ||
                                        ((Number) valor).longValue() >= Integer.MAX_VALUE) return Transaction.abort();
                                actual.setValue(((Number) valor).longValue() + 1);
                                return Transaction.success(actual);
                            }
                            @Override public void onComplete(DatabaseError e, boolean devuelto, DataSnapshot datos) {
                                callback.onError(e == null && devuelto
                                        ? "Ya no se podía equipar: recuperaste tu Escudo Dorado"
                                        : "No se confirmó el escudo. Verifica tu inventario antes de reintentar");
                            }
                        }, false);
                    }
                }, false);
            });
        });
    }
}
