package com.example.basta_game.utils;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

/**
 * Descuento de una unidad sin basarse en una lectura vieja del inventario.
 * La lectura remota precalienta el nodo antes de la transacción; Firebase
 * resuelve la concurrencia con runTransaction sobre la cantidad real.
 */
public final class ConsumoInventario {
    private ConsumoInventario() { }

    public interface Resultado {
        void terminado(boolean descontado, String mensaje);
    }

    public static void descontar(DatabaseReference cantidadRef, Resultado callback) {
        if (!ConexionJuego.disponible()) {
            callback.terminado(false, "La conexión con Firebase no está lista. Intenta de nuevo");
            return;
        }
        cantidadRef.get().addOnCompleteListener(lectura -> {
            if (!lectura.isSuccessful() || lectura.getResult() == null) {
                callback.terminado(false, "No pudimos consultar tu inventario. Intenta de nuevo");
                return;
            }
            Object valor = lectura.getResult().getValue();
            Number cantidad = valor instanceof Number ? (Number) valor : null;
            if (cantidad == null || cantidad.longValue() <= 0L) {
                callback.terminado(false, "No tienes unidades disponibles de este comodín");
                return;
            }
            ejecutarTransaccion(cantidadRef, callback, true);
        });
    }

    private static void ejecutarTransaccion(DatabaseReference ref, Resultado callback, boolean reintentoPermitido) {
        final boolean[] lecturaLocalAusente = { false };
        ref.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData actual) {
                Object valor = actual.getValue();
                Number cantidad = valor instanceof Number ? (Number) valor : null;
                if (cantidad == null) {
                    lecturaLocalAusente[0] = true;
                    return Transaction.abort();
                }
                if (cantidad.longValue() <= 0L) return Transaction.abort();
                actual.setValue(cantidad.longValue() - 1L);
                return Transaction.success(actual);
            }

            @Override public void onComplete(DatabaseError error, boolean confirmado, DataSnapshot snapshot) {
                if (error != null) {
                    callback.terminado(false, "Firebase no pudo confirmar el uso. Intenta de nuevo");
                } else if (confirmado) {
                    callback.terminado(true, "");
                } else if (reintentoPermitido && lecturaLocalAusente[0] && ConexionJuego.disponible()) {
                    // La transacción vio inicialmente un nodo sin precargar.
                    // Solo reintentamos si el servidor confirma que siguen existiendo unidades.
                    ref.get().addOnCompleteListener(lectura -> {
                        if (!lectura.isSuccessful() || lectura.getResult() == null) {
                            callback.terminado(false, "No se pudo verificar el inventario. Intenta de nuevo");
                            return;
                        }
                        Object valor = lectura.getResult().getValue();
                        Number actual = valor instanceof Number ? (Number) valor : null;
                        if (actual != null && actual.longValue() > 0L) {
                            ejecutarTransaccion(ref, callback, false);
                        } else {
                            callback.terminado(false, "No tienes unidades disponibles de este comodín");
                        }
                    });
                } else {
                    callback.terminado(false, lecturaLocalAusente[0]
                            ? "El inventario aún se está sincronizando. Intenta de nuevo"
                            : "No tienes unidades disponibles de este comodín");
                }
            }
        }, false);
    }
}
